package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class King extends Piece 
{

    private boolean hasMoved;

    public King(String color) 
    {
        super("King", color);
        this.hasMoved = false;
    }

    public boolean hasMoved() 
    {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) 
    {
        this.hasMoved = hasMoved;
    }

    @Override
    public boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board) 
    {
        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        // Standard king move (one square in any direction)
        if ((rowDiff <= 1 && colDiff <= 1) && !(rowDiff == 0 && colDiff == 0)) 
        {
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        }

        // Castling logic
        if (!hasMoved) 
        {
            // Kingside castling
            if (endRow == startRow && endCol == startCol + 2) 
            {
                Rook kingsideRook = (Rook) board.getPiece(startRow, 7);
                if (kingsideRook != null && !kingsideRook.hasMoved() &&
                    board.getPiece(startRow, 5) == null && board.getPiece(startRow, 6) == null &&
                    !board.isKingInCheck(getColor()) &&
                    !isSquareAttacked(startRow, 5, board, getColor()) &&
                    !isSquareAttacked(startRow, 6, board, getColor())) 
                {
                    return true;
                }
            }

            // Queenside castling
            if (endRow == startRow && endCol == startCol - 2) 
            {
                Rook queensideRook = (Rook) board.getPiece(startRow, 0);
                if (queensideRook != null && !queensideRook.hasMoved() &&
                    board.getPiece(startRow, 1) == null && board.getPiece(startRow, 2) == null && board.getPiece(startRow, 3) == null &&
                    !board.isKingInCheck(getColor()) &&
                    !isSquareAttacked(startRow, 3, board, getColor()) &&
                    !isSquareAttacked(startRow, 2, board, getColor())) 
                {
                    return true;
                }
            }
        }

        return false;
    }

    // Helper method to check if a square is attacked by any opponent's piece
    private boolean isSquareAttacked(int row, int col, Board board, String attackingColor) 
    {
        String opponentColor = (attackingColor.equals("white")) ? "black" : "white";
        for (int i = 0; i < Board.SIZE; i++) 
        {
            for (int j = 0; j < Board.SIZE; j++) 
            {
                Piece piece = board.getPiece(i, j);
                if (piece != null && piece.getColor().equals(opponentColor)) 
                {
                    if (piece.canMove(i, j, row, col, board)) 
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isKingInCheck(int row, int col, Board board) 
    {
        for (int i = 0; i < Board.SIZE; i++) 
        {
            for (int j = 0; j < Board.SIZE; j++) 
            {
                Piece piece = board.getPiece(i, j);
                if (piece != null && !piece.getColor().equals(getColor())) 
                {
                    if (piece.canMove(i, j, row, col, board)) 
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}