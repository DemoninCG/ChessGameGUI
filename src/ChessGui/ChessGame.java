package ChessGui;

import static ChessGui.Board.SIZE;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ChessGame {

    private Board board;
    private String currentPlayer;
    private PlayerData playerData;
    private String whitePlayerName;
    private String blackPlayerName;
    private int gameResult = 0; // 1=white win, 0=draw, -1=black win
    private boolean isGameOver = false;

    private ChessGUI gui;

    public ChessGame() {
        this.board = new Board();
        this.currentPlayer = "white";
        this.playerData = new PlayerData();
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

    // This method now only handles the initial setup.
    public void startGame() {
        // Player name handling remains in CLI for now
        System.out.println("Welcome to Chess.");
        Scanner scanner = new Scanner(System.in);
        String tempName;

        // --- (Player name input logic is unchanged) ---
        while (true) {
            System.out.print("Enter White player's name: ");
            tempName = scanner.nextLine().trim();
            if (tempName.equalsIgnoreCase("q") || tempName.equalsIgnoreCase("quit")) System.exit(0);
            if (tempName.isEmpty()) System.out.println("Player name cannot be empty.");
            else if (tempName.matches("^[a-zA-Z]+$")) { this.whitePlayerName = tempName; break; }
            else System.out.println("Invalid name. Please use letters only.");
        }
        playerData.checkOrAddPlayer(whitePlayerName);

        while (true) {
            System.out.print("Enter Black player's name: ");
            tempName = scanner.nextLine().trim();
            if (tempName.equalsIgnoreCase("q") || tempName.equalsIgnoreCase("quit")) System.exit(0);
            if (tempName.isEmpty()) System.out.println("Player name cannot be empty.");
            else if (!tempName.matches("^[a-zA-Z]+$")) System.out.println("Invalid name. Please use letters only.");
            else if (tempName.equalsIgnoreCase(this.whitePlayerName)) System.out.println("Black player name cannot be the same as White player name.");
            else { this.blackPlayerName = tempName; break; }
        }
        playerData.checkOrAddPlayer(blackPlayerName);
        scanner.close(); // No more console input needed

        // Initialize board and start the game turn
        board.initializeStandardBoard();
        System.out.println("\n--- Game Started ---");
        System.out.println("You can now move pieces on the board. Close the window to exit.");
        updateTurnAndStatus();
    }
    
    /**
     * This is the new central method called by the GUI to perform a move.
     */
    public void attemptMove(int startRow, int startCol, int endRow, int endCol) {
        if (isGameOver) return;

        Piece pieceToMove = board.getPiece(startRow, startCol);
        // Basic validation
        if (pieceToMove == null || !pieceToMove.getColor().equals(currentPlayer)) {
            return; // Not your piece or no piece
        }

        // Use board's move logic which includes check validation
        if (board.movePiece(startRow, startCol, endRow, endCol, false)) {
            // Check for pawn promotion
            if (pieceToMove instanceof Pawn && (endRow == 0 || endRow == 7)) {
                handlePawnPromotion(endRow, endCol);
            }
            
            // Switch player and update status
            switchPlayer();
            updateTurnAndStatus();
            checkGameEndConditions();

        } else {
            // Invalid move message could be shown, but for now, the piece just snaps back.
            System.err.println("Invalid move attempted from " + getAlgebraic(startRow, startCol) + " to " + getAlgebraic(endRow, endCol));
        }

        // Always update the GUI
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
        }
        gui.setStatusMessage(status);
    }
    
    private void checkGameEndConditions() {
        List<int[]> legalMoves = getAllPossibleMoves(currentPlayer);
        if (legalMoves.isEmpty()) {
            isGameOver = true;
            if (board.isKingInCheck(currentPlayer)) {
                // CHECKMATE
                String winnerColor = getOpponent(currentPlayer);
                String winnerName = winnerColor.equals("white") ? whitePlayerName : blackPlayerName;
                String message = "Checkmate! " + winnerName + " ("+ winnerColor.toUpperCase() + ") wins.";
                gui.setStatusMessage(message);
                JOptionPane.showMessageDialog(gui, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
                gameResult = winnerColor.equals("white") ? 1 : -1;
            } else {
                // STALEMATE
                String message = "Stalemate! The game is a draw.";
                gui.setStatusMessage(message);
                JOptionPane.showMessageDialog(gui, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
                gameResult = 0;
            }
            playerData.updateGameResults(whitePlayerName, blackPlayerName, gameResult);
        }
    }
    
    /**
     * Gets all valid destination squares for a piece at a given position.
     * @return A list of Points representing legal (row, col) destinations.
     */
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
                // movePiece on the simulation board returns true if the move is legal (including check prevention)
                if (simulationBoard.movePiece(startRow, startCol, endRow, endCol, true)) {
                    legalDests.add(new Point(endRow, endCol));
                }
            }
        }
        return legalDests;
    }
    
    private void handlePawnPromotion(int row, int col) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        String choice = (String) JOptionPane.showInputDialog(
            gui, 
            "Pawn promotion! Choose a piece:", 
            "Promotion", 
            JOptionPane.PLAIN_MESSAGE, 
            null, 
            options, 
            "Queen");

        Piece newPiece;
        String color = (row == 0) ? "white" : "black";

        switch (choice) {
            case "Rook": newPiece = new Rook(color); break;
            case "Bishop": newPiece = new Bishop(color); break;
            case "Knight": newPiece = new Knight(color); break;
            default: newPiece = new Queen(color); break; // Queen is the default
        }
        board.promotePawn(row, col, newPiece);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessGame game = new ChessGame();
            ChessGUI gui = new ChessGUI(game);
            game.setGui(gui);
            // Run the initial setup (name input) in a separate thread
            // so it doesn't block the GUI's Event Dispatch Thread.
            new Thread(game::startGame).start();
        });
    }

    // --- The rest of the helper methods are the same as before ---
    // (createTemporaryBoard, getAlgebraic, getOpponent, getAllPossibleMoves)
    // --- They are included here for completeness ---

    private String getOpponent(String color) {
        return (color.equals("white")) ? "black" : "white";
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        tempBoard.setEnPassantTarget(board.getEnPassantTarget());
        return tempBoard;
    }
    
    public static String getAlgebraic(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return "Invalid";
        char file = (char) ('a' + col);
        char rank = (char) ('1' + (SIZE - 1 - row));
        return "" + file + rank;
    }
}