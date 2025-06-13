package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Board 
{
    public static final int SIZE = 8;
    private Piece[][] squares;
    private int[] enPassantTarget; // [row, col] of the square behind the pawn that just moved two squares (null if EP not possible)

    public Board() 
    {
        this.squares = new Piece[SIZE][SIZE];
        this.enPassantTarget = null;
        initializeEmptyBoard();
    }

    // Creates a (size)-by-(size) 2D array of nulls
    private void initializeEmptyBoard() 
    {
        for (int row = 0; row < SIZE; row++) 
        {
            for (int col = 0; col < SIZE; col++) 
            {
                this.squares[row][col] = null;
            }
        }
    }

    // Sets designated row/col to piece (if in bounds)
    public void placePiece(Piece piece, int row, int col) 
    {
        if (isValidPosition(row, col)) 
        {
            this.squares[row][col] = piece;
        } 
        else 
        {
            System.err.println("Invalid position: " + row + ", " + col);
        }
    }

    // returns piece at row/col
    public Piece getPiece(int row, int col) 
    {
        if (isValidPosition(row, col)) 
        {
            return this.squares[row][col];
        } 
        else 
        {
            System.err.println("Invalid position: " + row + ", " + col);
            return null;
        }
    }

    // Sets designated row/col to null
    public void removePiece(int row, int col) 
    {
        if (isValidPosition(row, col)) 
        {
            this.squares[row][col] = null;
        } 
        else 
        {
            System.err.println("Invalid position: " + row + ", " + col);
        }
    }

    // Within board bounds
    private boolean isValidPosition(int row, int col) 
    {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public void displayBoard() 
    {
        // Long horizontal line
        System.out.println("  " + ("-").repeat(41));
        for (int row = 0; row < SIZE; row++) 
        {
            System.out.print(8 - row + " |");
            for (int col = 0; col < SIZE; col++) 
            {
                Piece piece = this.squares[row][col];
                // If point at row/col is a piece
                if (piece != null) 
                {
                    // Display first letter of color and piece name (knight is 'N')
                    String pieceName = piece.getNameInitial();
                    String color = piece.getColor().substring(0, 1).toLowerCase();
                    System.out.print(" " + color + pieceName + " |");
                } 
                else 
                {
                    System.out.print("    |");
                }
            }
            System.out.println();
            // Long horizontal line
            System.out.println("  " + ("-").repeat(41));
        }

        System.out.println("    a    b    c    d    e    f    g    h");
    }

    // Move piece from start row/col to end row/col
    // silenceInvalid is to prevent messages from popping up while moves are being simulated
    public boolean movePiece(int startRow, int startCol, int endRow, int endCol, boolean silenceInvalid) {
        Piece pieceToMove = getPiece(startRow, startCol);

        if (pieceToMove == null) {
            if (!silenceInvalid) System.err.println("No piece at the starting position: " + ChessGame.getAlgebraic(startRow, startCol));
            return false;
        }

        // Pre-move validation stuff

        // Store en passant target from the previous move before checking validity
        int[] currentEnPassantTarget = this.enPassantTarget;

        // Determine if the intended move looks like an attempted en passant
        boolean isAttemptingEnPassant = (pieceToMove instanceof Pawn && currentEnPassantTarget != null && endRow == currentEnPassantTarget[0] && endCol == currentEnPassantTarget[1]);

        // Check if move is valid
        if (!pieceToMove.canMove(startRow, startCol, endRow, endCol, this)) {
            // Special check for castling input (might fail canMove but be handled elsewhere)
            if (!(pieceToMove instanceof King && (Math.abs(endCol - startCol) == 2))) {
                if (!silenceInvalid) System.err.println("Invalid move pattern for " + pieceToMove.getName() + " from " + ChessGame.getAlgebraic(startRow, startCol) + " to " + ChessGame.getAlgebraic(endRow, endCol));
            } else {
                // If it was a king move by 2 squares, assume castling attempt failed
                if (!silenceInvalid) System.err.println("Castling conditions not met or path is not clear/safe.");
            }
            return false;
        }

        // Check for capturing own piece (only relevant for non-en passant moves)
        Piece destinationPiece = getPiece(endRow, endCol);
        if (!isAttemptingEnPassant && destinationPiece != null && destinationPiece.getColor().equals(pieceToMove.getColor())) {
            if (!silenceInvalid) System.err.println("Cannot capture your own piece at " + ChessGame.getAlgebraic(endRow, endCol));
            return false;
        }

        // Simulate move and check for self-check
        String movingPieceColor = pieceToMove.getColor();
        Piece actualCapturedPiece = null; // Track the piece actually captured/removed
        int capturedPieceRow = -1, capturedPieceCol = -1; // Track where it was

        // Temporarily move the piece
        squares[endRow][endCol] = pieceToMove;
        squares[startRow][startCol] = null;

        // If en passant, find and temporarily remove the captured pawn
        if (isAttemptingEnPassant) {
            capturedPieceRow = startRow;
            capturedPieceCol = endCol;
            actualCapturedPiece = getPiece(capturedPieceRow, capturedPieceCol); // Should be the opponent's pawn

            if (actualCapturedPiece instanceof Pawn && !actualCapturedPiece.getColor().equals(movingPieceColor)) {
                squares[capturedPieceRow][capturedPieceCol] = null; // Remove it for the check validation
            } else {
                // This is an internal error if Pawn.canMove was correct
                System.err.println("ERROR: En passant logic is bugged. Expected pawn at " + ChessGame.getAlgebraic(capturedPieceRow, capturedPieceCol));
                actualCapturedPiece = null;
            }
        } else if (destinationPiece != null) {
            // Standard capture: captured piece was at the destination square
            actualCapturedPiece = destinationPiece;
            capturedPieceRow = endRow;
            capturedPieceCol = endCol;
        }

        // Store the current en passant target state before potentially modifying it, needed for undo
        int[] enPassantTargetBeforeCheck = this.enPassantTarget;

        // Check if the king of the moving player is in check after the temporary move
        boolean inCheckAfterMove = isKingInCheck(movingPieceColor);

        // Undo temporary changes if move is illegal (would cause self-check)
        if (inCheckAfterMove) {
            // Move piece back
            squares[startRow][startCol] = pieceToMove;
            // Restore destination square (empty if en passant)
            squares[endRow][endCol] = isAttemptingEnPassant ? null : destinationPiece;

            // If en passant was attempted and a piece was temporarily removed, put it back
            if (isAttemptingEnPassant && actualCapturedPiece != null) {
                squares[capturedPieceRow][capturedPieceCol] = actualCapturedPiece;
            }
            // Restore the en passant target state as it was before the move attempt
            this.enPassantTarget = enPassantTargetBeforeCheck; // Restore original EP target state

            if (!silenceInvalid) System.err.println("Illegal move. This move puts your king in check.");
            return false;
        }

        // Move is valid, finalize board state

        this.enPassantTarget = null;

        // Update 'hasMoved' flags for king and rook for castling rights
        if (pieceToMove instanceof King) {
            ((King) pieceToMove).setHasMoved(true);
            // Move the rook
            if (Math.abs(endCol - startCol) == 2) { // Castling move detected
                int rookStartCol = (endCol > startCol) ? 7 : 0; // Kingside or Queenside
                int rookEndCol = (endCol > startCol) ? 5 : 3;
                Piece rook = getPiece(startRow, rookStartCol);
                if (rook instanceof Rook) {
                    squares[startRow][rookEndCol] = rook;
                    squares[startRow][rookStartCol] = null;
                    ((Rook) rook).setHasMoved(true);
                }
            }
        } else if (pieceToMove instanceof Rook) {
            ((Rook) pieceToMove).setHasMoved(true);
        }

        // Set new en passant target if this move was a two-square pawn advance
        if (pieceToMove instanceof Pawn && Math.abs(endRow - startRow) == 2) {
            this.enPassantTarget = new int[]{(startRow + endRow) / 2, startCol}; // Set target square behind pawn
        }

        return true;
    }

    public void promotePawn(int row, int col, Piece newPiece) {
        if (isValidPosition(row, col) && getPiece(row, col) instanceof Pawn && newPiece != null) {
            if (getPiece(row, col).getColor().equals(newPiece.getColor())) { // Ensure colors match
               squares[row][col] = newPiece;
            } else {
                 System.err.println("Promotion color mismatch at " + ChessGame.getAlgebraic(row, col));
            }
        } else {
            System.err.println("Invalid promotion attempt at " + ChessGame.getAlgebraic(row, col));
        }
    }

    public int[] getEnPassantTarget() {
        return (enPassantTarget == null) ? null : enPassantTarget.clone();
    }
    
    
    // Setter primarily for temporary board creation
    public void setEnPassantTarget(int row, int col) {
        if (isValidPosition(row, col)) {
            this.enPassantTarget = new int[]{row, col};
        } else {
            this.enPassantTarget = null;
        }
    }

    // Overload for easy setting to null
    public void setEnPassantTarget(int[] target) {
        if (target == null) {
            this.enPassantTarget = null;
        } else if (target.length == 2 && isValidPosition(target[0], target[1])) {
            this.enPassantTarget = target.clone();
        } else {
             this.enPassantTarget = null;
        }
    }

    // Helper method to check if a square is attacked by any opponent's piece
    private boolean isSquareAttacked(int row, int col, String attackingColor) 
    {
        String opponentColor = (attackingColor.equals("white")) ? "black" : "white";
        for (int i = 0; i < SIZE; i++) 
        {
            for (int j = 0; j < SIZE; j++) 
            {
                Piece piece = getPiece(i, j);
                if (piece != null && piece.getColor().equals(opponentColor)) 
                {
                    if (piece.canMove(i, j, row, col, this)) 
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private int[] findKing(String color) 
    {
        for (int row = 0; row < SIZE; row++) 
        {
            for (int col = 0; col < SIZE; col++) 
            {
                Piece piece = getPiece(row, col);
                if (piece instanceof King && piece.getColor().equals(color)) 
                {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    public boolean isKingInCheck(String color) 
    {
        int[] kingPosition = findKing(color);
        if (kingPosition == null) 
        {
            return false;
        }
        int kingRow = kingPosition[0];
        int kingCol = kingPosition[1];

        String opponentColor = (color.equals("white")) ? "black" : "white";
        for (int row = 0; row < SIZE; row++) 
        {
            for (int col = 0; col < SIZE; col++) 
            {
                Piece piece = getPiece(row, col);
                if (piece != null && piece.getColor().equals(opponentColor)) 
                {
                    if (piece.canMove(row, col, kingRow, kingCol, this)) 
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void initializeStandardBoard() 
    {
        // Place white pieces
        placePiece(new Rook("white"), 7, 0);
        placePiece(new Knight("white"), 7, 1);
        placePiece(new Bishop("white"), 7, 2);
        placePiece(new Queen("white"), 7, 3);
        placePiece(new King("white"), 7, 4);
        placePiece(new Bishop("white"), 7, 5);
        placePiece(new Knight("white"), 7, 6);
        placePiece(new Rook("white"), 7, 7);
        for (int i = 0; i < SIZE; i++) {
            placePiece(new Pawn("white"), 6, i);
        }

        // Place black pieces
        placePiece(new Rook("black"), 0, 0);
        placePiece(new Knight("black"), 0, 1);
        placePiece(new Bishop("black"), 0, 2);
        placePiece(new Queen("black"), 0, 3);
        placePiece(new King("black"), 0, 4);
        placePiece(new Bishop("black"), 0, 5);
        placePiece(new Knight("black"), 0, 6);
        placePiece(new Rook("black"), 0, 7);
        for (int i = 0; i < SIZE; i++) {
            placePiece(new Pawn("black"), 1, i);
        }
    }
}
 