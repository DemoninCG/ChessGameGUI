package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Rook extends Piece 
{

    private boolean hasMoved;

    public Rook(String color) 
    {
        super("Rook", color);
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
        
        if (startRow == endRow) 
        { // Moving horizontally
            int minCol = Math.min(startCol, endCol);
            int maxCol = Math.max(startCol, endCol);
            for (int col = minCol + 1; col < maxCol; col++) 
            {
                if (board.getPiece(startRow, col) != null) 
                {
                    return false; // Obstacle in the way
                }
            }
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        } 
        else if (startCol == endCol) { // Moving vertically
            int minRow = Math.min(startRow, endRow);
            int maxRow = Math.max(startRow, endRow);
            for (int row = minRow + 1; row < maxRow; row++) 
            {
                if (board.getPiece(row, startCol) != null) 
                {
                    return false; // Obstacle in the way
                }
            }
            Piece destinationPiece = board.getPiece(endRow, endCol);
            return destinationPiece == null || !destinationPiece.getColor().equals(getColor());
        }
        return false; // Not moving horizontally or vertically
    }
}
  
