package ChessGui;

import static ChessGui.Board.SIZE;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class ChessGame {

    private Board board;
    private String currentPlayer;
    private PlayerData playerData;
    private String whitePlayerName;
    private String blackPlayerName;
    private int gameResult = 0; // 1 = white win, 0 = draw, -1 = black win
    private boolean isGameOver = true; // Game is over until New Game is clicked

    private ChessGUI gui;

    public ChessGame() {
        this.board = new Board();
        this.currentPlayer = "white";
        // Pass reference to this game logic instance to PlayerData
        this.playerData = new PlayerData(this);
    }
    
    public static void main(String[] args) {
        // GUI's constructor and event listeners will handle the proper start of the game
        SwingUtilities.invokeLater(ChessGUI::new);
    }

    public void setGui(ChessGUI gui) {
        this.gui = gui;
    }

    public Board getBoard() {
        return this.board;
    }

    public String getCurrentPlayer() {
        return this.currentPlayer;
    }

    public boolean isGameOver() {
        return this.isGameOver;
    }

    // Sets up and starts a new game
    public void startGame() {
        gui.setGameInProgress(true);

        if (!promptForPlayerNames()) {
            gui.setGameInProgress(false); // Re-enable if cancelled
            return;
        }

        isGameOver = false;
        board.initializeEmptyBoard();
        board.initializeStandardBoard();
        currentPlayer = "white";
        gui.logMessage("\n--- New Game Started ---");
        gui.logMessage(whitePlayerName + " (White) vs. " + blackPlayerName + " (Black)");
        
        updateTurnAndStatus();
        gui.updateBoard();
    }

    // Returns true if names were entered successfully, false if cancelled
    private boolean promptForPlayerNames() {
        // Get White player's name
        while (true) {
            whitePlayerName = JOptionPane.showInputDialog(gui, "Enter White player's name:", "Player 1", JOptionPane.PLAIN_MESSAGE);
            if (whitePlayerName == null) return false; // User cancelled
            whitePlayerName = whitePlayerName.trim();
            if (whitePlayerName.isEmpty()) {
                JOptionPane.showMessageDialog(gui, "Player name cannot be empty.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            } else if (!whitePlayerName.matches("^[a-zA-Z]+$")) {
                JOptionPane.showMessageDialog(gui, "Please use letters only (A-Z).", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            } else {
                break;
            }
        }
        playerData.checkOrAddPlayer(whitePlayerName);

        // Get Black player's name
        while (true) {
            blackPlayerName = JOptionPane.showInputDialog(gui, "Enter Black player's name:", "Player 2", JOptionPane.PLAIN_MESSAGE);
            if (blackPlayerName == null) return false; // User cancelled
            blackPlayerName = blackPlayerName.trim();
            if (blackPlayerName.isEmpty()) {
                JOptionPane.showMessageDialog(gui, "Player name cannot be empty.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            } else if (!blackPlayerName.matches("^[a-zA-Z]+$")) {
                JOptionPane.showMessageDialog(gui, "Please use letters only (A-Z).", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            } else if (blackPlayerName.equalsIgnoreCase(whitePlayerName)) {
                JOptionPane.showMessageDialog(gui, "Player names cannot be the same.", "Invalid Name", JOptionPane.ERROR_MESSAGE);
            } else {
                break;
            }
        }
        playerData.checkOrAddPlayer(blackPlayerName);
        return true;
    }

    // Called by the GUI to try to perform a move
    public void attemptMove(int startRow, int startCol, int endRow, int endCol) {
        if (isGameOver) return;
        Piece pieceToMove = board.getPiece(startRow, startCol);
        if (pieceToMove == null || !pieceToMove.getColor().equals(currentPlayer)) return;

        // movePiece method returns a descriptive string
        String moveResult = board.movePiece(startRow, startCol, endRow, endCol, false);

        // If the move was illegal, log it and do nothing else
        if (moveResult.equals(Board.MOVE_ILLEGAL)) {
            log("Invalid move: " + getAlgebraic(startRow, startCol) + " to " + getAlgebraic(endRow, endCol));
            return;
        }

        // Log the successful move based on its type
        logMove(moveResult, pieceToMove, startRow, startCol, endRow, endCol);

        // Check for promotion after a successful move
        if (pieceToMove instanceof Pawn && (endRow == 0 || endRow == 7)) {
            handlePawnPromotion(endRow, endCol);
        }

        finalizeTurn();
    }
    
    private void logMove(String moveResult, Piece piece, int startRow, int startCol, int endRow, int endCol) {
        String moveLog;
        switch (moveResult) {
            case Board.MOVE_CASTLE:
                String side = (endCol == 6) ? "kingside (O-O)" : "queenside (O-O-O)";
                moveLog = String.format("%s castles %s.", currentPlayer, side);
                break;
            case Board.MOVE_EN_PASSANT:
                moveLog = String.format("%s captures via en passant at %s.",
                    currentPlayer, getAlgebraic(endRow, endCol));
                break;
            case Board.MOVE_OK:
            default:
                Piece capturedPiece = board.getPiece(endRow, endCol);
                moveLog = String.format("%s's move: %s from %s to %s.",
                    currentPlayer, piece.getName(), getAlgebraic(startRow, startCol), getAlgebraic(endRow, endCol));
                break;
        }
        log(moveLog);
    }
    
    // Public method for PlayerData to use
    public void log(String message) {
        if (gui != null) {
            gui.logMessage(message);
        } else {
            System.out.println(message); // Fallback if GUI isn't ready
        }
    }
    
    // Groups together the actions to take after any successful move
    private void finalizeTurn() {
        switchPlayer();
        updateTurnAndStatus();
        checkGameEndConditions();
        gui.updateBoard();
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer.equals("white")) ? "black" : "white";
    }
    
    private void updateTurnAndStatus() {
        String currentTurnPlayerName = currentPlayer.equals("white") ? whitePlayerName : blackPlayerName;
        String status = String.format("%s's Turn (%s)", currentTurnPlayerName, currentPlayer.toUpperCase());
        if (board.isKingInCheck(currentPlayer)) {
            status += " - CHECK!";
            gui.logMessage(currentPlayer.toUpperCase() + " is in check!");
        }
        gui.setStatusMessage(status);
    }
    
    // Handles the current player resigning the game
    public void resign() {
        if (isGameOver) return;
        
        isGameOver = true;
        gui.setGameInProgress(false); // Game is over, update button states
        
        String resigningPlayerName = currentPlayer.equals("white") ? whitePlayerName : blackPlayerName;
        String winnerColor = getOpponent(currentPlayer);
        String winnerName = winnerColor.equals("white") ? whitePlayerName : blackPlayerName;
        
        gameResult = winnerColor.equals("white") ? 1 : -1;
        
        String message = resigningPlayerName + " has resigned. " + winnerName + " wins.";
        
        log("\n--- GAME OVER ---");
        log(message);
        gui.setStatusMessage(message);
        JOptionPane.showMessageDialog(gui, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
        playerData.updateGameResults(whitePlayerName, blackPlayerName, gameResult);
    }
    
    private void checkGameEndConditions() {
        List<int[]> legalMoves = getAllPossibleMoves(currentPlayer);
        if (legalMoves.isEmpty()) {
            isGameOver = true;
            gui.setGameInProgress(false); // Game is over, update button states
            String message;
            if (board.isKingInCheck(currentPlayer)) {
                String winnerColor = getOpponent(currentPlayer);
                String winnerName = winnerColor.equals("white") ? whitePlayerName : blackPlayerName;
                message = "Checkmate! " + winnerName + " ("+ winnerColor.toUpperCase() + ") wins.";
                gameResult = winnerColor.equals("white") ? 1 : -1;
            } else {
                message = "Stalemate! The game is a draw.";
                gameResult = 0;
            }
            gui.logMessage("--- GAME OVER ---");
            gui.logMessage(message);
            gui.setStatusMessage(message);
            JOptionPane.showMessageDialog(gui, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            playerData.updateGameResults(whitePlayerName, blackPlayerName, gameResult);
        }
    }
    
    private void handlePawnPromotion(int row, int col) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        String choice = (String) JOptionPane.showInputDialog(gui, "Pawn promotion! Choose a piece:", "Promotion", JOptionPane.PLAIN_MESSAGE, null, options, "Queen");
        if (choice == null) choice = "Queen"; // Default if dialog is closed

        Piece newPiece;
        String color = (row == 0) ? "white" : "black";
        switch (choice) {
            case "Rook": newPiece = new Rook(color); break;
            case "Bishop": newPiece = new Bishop(color); break;
            case "Knight": newPiece = new Knight(color); break;
            default: newPiece = new Queen(color); break;
        }
        board.promotePawn(row, col, newPiece);
        gui.logMessage(currentPlayer + " promoted pawn to a " + choice + ".");
    }
    
    public List<Point> getLegalMovesForPiece(int startRow, int startCol) {
        List<Point> legalDests = new ArrayList<>();
        Piece piece = board.getPiece(startRow, startCol);
        if (piece == null || !piece.getColor().equals(currentPlayer)) {
            return legalDests; // Return empty list if no piece or not current player's piece
        }

        for (int endRow = 0; endRow < Board.SIZE; endRow++) {
            for (int endCol = 0; endCol < Board.SIZE; endCol++) {
                // Create a temporary board to simulate the move
                Board simulationBoard = createTemporaryBoard();
                
                // Get the string result from the move attempt
                String moveResult = simulationBoard.movePiece(startRow, startCol, endRow, endCol, true);

                // A move is legal if the result is anything other than "illegal"
                if (!moveResult.equals(Board.MOVE_ILLEGAL)) {
                    legalDests.add(new Point(endRow, endCol));
                }
            }
        }
        return legalDests;
    }
    
    private List<int[]> getAllPossibleMoves(String color) {
        List<int[]> legalMoves = new ArrayList<>();
        for (int startRow = 0; startRow < Board.SIZE; startRow++) {
            for (int startCol = 0; startCol < Board.SIZE; startCol++) {
                if (board.getPiece(startRow, startCol) != null && board.getPiece(startRow, startCol).getColor().equals(color)) {
                    for (Point dest : getLegalMovesForPiece(startRow, startCol)) {
                        legalMoves.add(new int[]{startRow, startCol, dest.x, dest.y});
                    }
                }
            }
        }
        return legalMoves;
    }

    private Board createTemporaryBoard() {
        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    try {
                        Piece copy = p.getClass().getDeclaredConstructor(String.class).newInstance(p.getColor());
                        if (p instanceof King) ((King)copy).setHasMoved(((King)p).hasMoved());
                        else if (p instanceof Rook) ((Rook)copy).setHasMoved(((Rook)p).hasMoved());
                        tempBoard.placePiece(copy, r, c);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        }
        tempBoard.setEnPassantTarget(board.getEnPassantTarget());
        return tempBoard;
    }
    
    // Gets the match history string
    public String getMatchHistory() {
        return playerData.getMatchHistory();
    }

    // Initiates recalculation of all player rankings
    public void recalculateRankings() {
        playerData.recalculateAllRankings();
    }
    
    private String getOpponent(String color) {
        return (color.equals("white")) ? "black" : "white";
    }

    public static String getAlgebraic(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return "Invalid";
        char file = (char) ('a' + col);
        char rank = (char) ('1' + (SIZE - 1 - row));
        return "" + file + rank;
    }
}