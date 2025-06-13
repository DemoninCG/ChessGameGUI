package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Bishop extends Piece 
{

    public Bishop(String color) 
    {
        super("Bishop", color);
    }

    @Override
    public boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board) 
    {
        // Bishops move diagonally any number of squares.

        // Check if the move is diagonal. The absolute difference in rows must equal
        // the absolute difference in columns.
        if (Math.abs(endRow - startRow) != Math.abs(endCol - startCol)) 
        {
            return false; // Not a diagonal move.
        }

        // Determine the direction of movement (up-left, up-right, down-left, down-right).
        int rowStep = (endRow > startRow) ? 1 : -1;
        int colStep = (endCol > startCol) ? 1 : -1;

        // Check for any pieces obstructing the path.
        int currentRow = startRow + rowStep;
        int currentCol = startCol + colStep;

        while (currentRow != endRow && currentRow >= 0 && currentCol >= 0) 
        {
            if (board.getPiece(currentRow, currentCol) != null) 
            {
                return false; // There's a piece in the way.
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        // Check if the destination square is empty or contains an opponent's piece.
        Piece destinationPiece = board.getPiece(endRow, endCol);
        return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
    }
}
