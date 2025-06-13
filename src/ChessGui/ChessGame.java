package ChessGui;

import static ChessGui.Board.SIZE;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class ChessGame 
{

    private Board board;
    private String currentPlayer;
    private Scanner scanner;
    private PlayerData playerData;
    private String whitePlayerName;
    private String blackPlayerName;
    private int gameResult = 0; // 1 = white win, 0 = draw, -1 = black win (white loss)

    public ChessGame() 
    {
        this.board = new Board();
        this.currentPlayer = "white";
        this.scanner = new Scanner(System.in);
        this.playerData = new PlayerData();
    }

    public void startGame() 
    {
        // Player name handling
        System.out.println("Welcome to Chess.");
        String tempName; // Temporary variable to hold input

        // Get and validate white player's name
        while (true) {
            System.out.print("Enter White player's name: ");
            tempName = scanner.nextLine().trim(); // Read and trim whitespace
            
            // Check for quit command
            if (tempName.equalsIgnoreCase("q") || tempName.equalsIgnoreCase("quit")) {
                System.out.println("Exiting program.");
                System.exit(0); // Terminate the program immediately
            }

            if (tempName.isEmpty()) {
                 System.out.println("Player name cannot be empty. Please try again.");
            } else if (tempName.matches("^[a-zA-Z]+$")) { // Regex to ensure letters only (not the only way to handle this)
                this.whitePlayerName = tempName;
                break; // Exit loop on valid input
            } else {
                System.out.println("Invalid name. Please use letters only (A-Z or a-z). No spaces, numbers, or symbols.");
            }
        }
        playerData.checkOrAddPlayer(whitePlayerName);

        // Get and validate black player's name
        while (true) {
            System.out.print("Enter Black player's name: ");
            tempName = scanner.nextLine().trim(); // Read and trim whitespace
            
            // Check for quit command
            if (tempName.equalsIgnoreCase("q") || tempName.equalsIgnoreCase("quit")) {
                System.out.println("Exiting program.");
                System.exit(0); // Terminate the program immediately
            }

            if (tempName.isEmpty()) {
                System.out.println("Player name cannot be empty. Please try again.");
            } else if (!tempName.matches("^[a-zA-Z]+$")) {
                System.out.println("Invalid name. Please use letters only (A-Z or a-z). No spaces, numbers, or symbols.");
            } else if (tempName.equalsIgnoreCase(this.whitePlayerName)) { // Ensure the same name isn't inputted twice
                 System.out.println("Black player name cannot be the same as White player name. Please enter a different name.");
            } else {
                this.blackPlayerName = tempName;
                break; // Exit loop on valid input
            }
        }
        playerData.checkOrAddPlayer(blackPlayerName);
        
        board.initializeStandardBoard();
        boolean gameOver = false;

        while (!gameOver) {
            // Display board with pieces
            board.displayBoard();
            // Display player name + color
            String currentTurnPlayerName = currentPlayer.equals("white") ? whitePlayerName : blackPlayerName;
            System.out.println(currentTurnPlayerName + " (" + currentPlayer.toUpperCase() + ")'s turn.");
            
            List<int[]> legalMoves = getAllPossibleMoves(currentPlayer); // Get all valid moves

            if (board.isKingInCheck(currentPlayer)) { // Player is in check 
                System.out.println(currentPlayer + " king is in check!");
                if (legalMoves.isEmpty()) //Checkmate
                {
                    String winnerColor = getOpponent(currentPlayer);
                    String winnerName = winnerColor.equals("white") ? whitePlayerName : blackPlayerName;
                    System.out.println("Checkmate! " + winnerName + " ("+ winnerColor.toUpperCase() + ") wins.");
                    gameResult = winnerColor.equals("white") ? 1 : -1; // Set result
                    // End the game and break the loop
                    gameOver = true;
                    continue;
                }
            }
            else // Not in check
            {
                if (legalMoves.isEmpty()) // Stalemate
                {
                    System.out.println("Stalemate! The game is a draw.");
                    gameResult = 0; // Set result
                    // End the game and break the loop
                    gameOver = true;
                    continue;
                }
            }
            

            System.out.println("Enter your move (e.g., a2 b4, O-O, O-O-O), or type 'q' to quit:");
            // Convert input to lowercase
            String moveInput = scanner.nextLine().toLowerCase();

            // Quitting the game / resignation
            if (moveInput.equals("q") || moveInput.equals("quit") || moveInput.equals("r") || moveInput.equals("resign")) 
            {
                String winnerColor = getOpponent(currentPlayer);
                String winnerName = winnerColor.equals("white") ? whitePlayerName : blackPlayerName;
                System.out.println(currentTurnPlayerName + " (" + currentPlayer.toUpperCase() + ") has resigned. " + winnerName + " (" + winnerColor.toUpperCase() + ") wins.");
                gameResult = winnerColor.equals("white") ? 1 : -1; // Set result based on who resigned
                gameOver = true;
                continue;
            }

            int[] moveCoordinates = null;
            // Kingside castling
            if (moveInput.equals("0-0") || moveInput.equals("o-o")) 
            {
                if (handleCastling("kingside")) 
                {
                    currentPlayer = getOpponent(currentPlayer);
                }
                continue;
            }
            // Queenside castling
            else if (moveInput.equals("0-0-0") || moveInput.equals("o-o-o")) 
            {
                if (handleCastling("queenside")) 
                {
                    currentPlayer = getOpponent(currentPlayer);
                }

                continue;
            } 
            // Check for valid move notation
            else if (isValidMoveFormat(moveInput)) 
            {
                moveCoordinates = parseMoveInput(moveInput);
            } 
            else 
            {
                System.out.println("Invalid move format.");
                continue;
            }

            // Handling piece movement
            if (moveCoordinates != null) 
            {
                int startCol = moveCoordinates[0];
                int startRow = moveCoordinates[1];
                int endCol = moveCoordinates[2];
                int endRow = moveCoordinates[3];

                Piece pieceToMove = board.getPiece(startRow, startCol);

                if (pieceToMove != null && pieceToMove.getColor().equals(currentPlayer)) 
                {
                    if (board.movePiece(startRow, startCol, endRow, endCol, false)) 
                    {
                        // Check for pawn promotion
                        if (pieceToMove instanceof Pawn && (endRow == 0 || endRow == 7)) 
                        {
                            Piece promotedPiece = handlePawnPromotion(endRow, endCol);
                            if (promotedPiece != null) 
                            {
                                board.promotePawn(endRow, endCol, promotedPiece);
                                currentPlayer = getOpponent(currentPlayer);
                            } 
                            else 
                            {
                                // If promotion fails (e.g., invalid input), the turn doesn't change
                                continue;
                            }
                        } 
                        else 
                        {
                            currentPlayer = getOpponent(currentPlayer);
                        }
                    }
                } 
                else if (pieceToMove == null) 
                {
                    System.out.println("No piece at the starting position.");
                } else 
                {
                    System.out.println("That's not your piece!");
                }
            }
            System.out.println();
        }
        
        // Update player data based on the result
        playerData.updateGameResults(whitePlayerName, blackPlayerName, gameResult);
        
        scanner.close();
    }

    private Piece handlePawnPromotion(int row, int col) 
    {
        System.out.println("Pawn promotion! Promote to (Q)ueen, (R)ook, (B)ishop, or (K)night?");
        String choice = scanner.nextLine().trim().toUpperCase();
        String color = (row == 0) ? "white" : "black"; // Promoted piece keeps the pawn's color

        switch (choice) 
        {
            case "Q":
                return new Queen(color);
            case "R":
                return new Rook(color);
            case "B":
                return new Bishop(color);
            case "K":
                return new Knight(color);
            default:
                System.out.println("Invalid promotion choice. Promoting to Queen by default.");
                return new Queen(color);
        }
    }

    private boolean handleCastling(String type) 
    {
        int kingRow = (currentPlayer.equals("white")) ? 7 : 0;
        int kingStartCol = 4;
        int kingEndCol = (type.equals("kingside")) ? 6 : 2;

        Piece king = board.getPiece(kingRow, kingStartCol);

        if (king instanceof King && king.getColor().equals(currentPlayer)) 
        {
            return board.movePiece(kingRow, kingStartCol, kingRow, kingEndCol, false);
        } 
        else 
        {
            System.out.println("Cannot castle. The king is not in the correct position or has moved.");
            return false;
        }
    }

    private String getOpponent(String color) 
    {
        return (color.equals("white")) ? "black" : "white";
    }

    // Checks valid move notation (e.g. "b2 b4")
    private boolean isValidMoveFormat(String moveInput) 
    {
        return moveInput.matches("^[a-h][1-8] [a-h][1-8]$");
    }

    // Converts input move notation into start/end rows and columns
    private int[] parseMoveInput(String moveInput) 
    {
        String[] squares = moveInput.split(" ");
        String startSquare = squares[0];
        String endSquare = squares[1];

        int startCol = startSquare.charAt(0) - 'a';
        int startRow = 7 - (startSquare.charAt(1) - '1');
        int endCol = endSquare.charAt(0) - 'a';
        int endRow = 7 - (endSquare.charAt(1) - '1');

        return new int[]{startCol, startRow, endCol, endRow};
    }

    // Returns list of valid moves for a player
    private List<int[]> getAllPossibleMoves(String color) {
        List<int[]> legalMoves = new ArrayList<>();
        for (int startRow = 0; startRow < Board.SIZE; startRow++) {
            for (int startCol = 0; startCol < Board.SIZE; startCol++) {
                Piece piece = board.getPiece(startRow, startCol);
                if (piece != null && piece.getColor().equals(color)) {
                    for (int endRow = 0; endRow < Board.SIZE; endRow++) {
                        for (int endCol = 0; endCol < Board.SIZE; endCol++) {
                            // Create a temporary board for each potential move to test
                            Board simulationBoard = createTemporaryBoard();
                            
                            Piece tempPiece = simulationBoard.getPiece(startRow, startCol); // Get piece from sim board

                            // Check if move pattern is possible at all
                            if (tempPiece != null && tempPiece.canMove(startRow, startCol, endRow, endCol, simulationBoard)) {
                                
                                // Attempt the move on the simulation board (simulationBoard.movePiece() handles all validation)
                                if (simulationBoard.movePiece(startRow, startCol, endRow, endCol, true)) {
                                    // Move is legal
                                    legalMoves.add(new int[]{startRow, startCol, endRow, endCol});
                                }
                            }
                        }
                    }
                }
            }
        } // Tower of closing brackets tall enough to enter the stratosphere
        return legalMoves;
    }

    // Returns a new board with duplicates of each piece copied across
    private Board createTemporaryBoard() {
        Board tempBoard = new Board();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                Piece p = board.getPiece(r, c);
                if (p != null) {
                    try {
                        Piece copy = p.getClass().getDeclaredConstructor(String.class).newInstance(p.getColor());
                        if (copy instanceof King && p instanceof King) {
                            ((King)copy).setHasMoved(((King)p).hasMoved());
                        } else if (copy instanceof Rook && p instanceof Rook) {
                            ((Rook)copy).setHasMoved(((Rook)p).hasMoved());
                        }
                       tempBoard.placePiece(copy, r, c);
                    } catch (Exception e) {
                        System.err.println("Error creating temporary board piece: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        // Copy en passant target state
        int[] currentEPTarget = board.getEnPassantTarget();
        if (currentEPTarget != null) {
           tempBoard.setEnPassantTarget(currentEPTarget[0], currentEPTarget[1]);
        }

        return tempBoard;
    }
    
    // Helper for error messages (returns row/col as algebraic notation)
    public static String getAlgebraic(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return "Invalid Square";
        }
        char file = (char) ('a' + col);
        char rank = (char) ('1' + (SIZE - 1 - row));
        return "" + file + rank;
    }
    
    public static void main(String[] args) 
    {
        ChessGame game = new ChessGame();
        game.startGame();
    }
}
