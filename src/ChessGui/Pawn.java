package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class Pawn extends Piece 
{

    public Pawn(String color) 
    {
        super("Pawn", color);
    }

    @Override
    public boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board)
    {
        String color = getColor();
        Piece destinationPiece = board.getPiece(endRow, endCol);
        int forwardStep = (color.equals("white")) ? -1 : 1;
        int startingRow = (color.equals("white")) ? 6 : 1;

        // Standard forward moves
        if (startCol == endCol)
        {
            if (endRow == startRow + forwardStep && destinationPiece == null)
            {
                return true;
            }
            if (startRow == startingRow && endRow == startRow + 2 * forwardStep && destinationPiece == null && board.getPiece(startRow + forwardStep, startCol) == null)
            {
                return true;
            }
        }
        // Standard diagonal capture
        else if (Math.abs(endCol - startCol) == 1 && endRow == startRow + forwardStep && destinationPiece != null && !destinationPiece.getColor().equals(color))
        {
            return true;
        }

        // En passant capture
        int[] enPassantTarget = board.getEnPassantTarget();
        if (enPassantTarget != null && endRow == enPassantTarget[0] && endCol == enPassantTarget[1])
        {
            // Check if the capturing pawn is on the correct rank
            int capturingPawnRank = (color.equals("white")) ? 3 : 4; // 5th rank for white (index 4), 4th for black (index 3)
            if (startRow == capturingPawnRank && Math.abs(endCol - startCol) == 1)
            {
                // Check if the captured pawn is an opponent's pawn
                Piece capturedPawn = board.getPiece(startRow, endCol); // The captured pawn is at the capturing pawn's row and the target column
                if (capturedPawn instanceof Pawn && !capturedPawn.getColor().equals(color))
                {
                    return true;
                }
            }
        }

        return false;
    }

    // Helper method to get the starting row of a pawn (needed for a more robust en passant check)
    public int getStartingRow() 
    {
        return (getColor().equals("white")) ? 6 : 1;
    }
}
