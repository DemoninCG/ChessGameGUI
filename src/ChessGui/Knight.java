package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Knight extends Piece 
{

    public Knight(String color) 
    {
        super("Knight", color);
    }

    @Override
    public boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board) 
    {
        // Knights move in an 'L' shape: two squares in one direction (horizontal or vertical)
        // and then one square perpendicularly.

        int rowDiff = Math.abs(endRow - startRow);
        int colDiff = Math.abs(endCol - startCol);

        // Check if the move is an 'L' shape (2 squares in one direction, 1 in the other).
        if ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)) 
        {
            // Check if the destination square is empty or contains an opponent's piece.
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        }

        // If the move doesn't fit the 'L' shape, it's not a valid knight move.
        return false;
    }
}

