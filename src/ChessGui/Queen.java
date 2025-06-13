package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Queen extends Piece 
{

    public Queen(String color) 
    {
        super("Queen", color);
    }

    @Override
    public boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board) 
    {
        // The queen can move horizontally, vertically, or diagonally.

        // Check for horizontal or vertical movement (like a Rook).
        if (startRow == endRow) 
        {
            int step = (endCol > startCol) ? 1 : -1;
            for (int col = startCol + step; col != endCol; col += step) 
            {
                if (board.getPiece(startRow, col) != null) 
                {
                    return false; // Obstruction in horizontal path.
                }
            }
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        } 
        else if (startCol == endCol) 
        {
            int step = (endRow > startRow) ? 1 : -1;
            for (int row = startRow + step; row != endRow; row += step) 
            {
                if (board.getPiece(row, startCol) != null) 
                {
                    return false; // Obstruction in vertical path.
                }
            }
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        }

        // Check for diagonal movement (like a Bishop).
        if (Math.abs(endRow - startRow) == Math.abs(endCol - startCol)) 
        {
            int rowStep = (endRow > startRow) ? 1 : -1;
            int colStep = (endCol > startCol) ? 1 : -1;
            int currentRow = startRow + rowStep;
            int currentCol = startCol + colStep;
            while (currentRow != endRow && currentRow >= 0 && currentCol >= 0) {
                if (board.getPiece(currentRow, currentCol) != null) 
                {
                    return false; // Obstruction in diagonal path.
                }
                currentRow += rowStep;
                currentCol += colStep;
            }
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        }

        // If none of the above conditions are met, it's not a valid queen move.
        return false;
    }
}
