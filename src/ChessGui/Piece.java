package ChessGui;

/**
 *
 * @author Corban Guy, Naz Janif
 */
// Declaring an abstract class named 'Piece' to be implemented by the various piece types
public abstract class Piece 
{

    // Declaring a private String variable named 'name' to store the name of the piece.
    // 'private' access modifier ensures that this variable can only be accessed within this class.
    private String name;

    // Declaring a private String variable named 'color' to store the color of the piece (e.g., "white" or "black").
    private String color;

    // Constructor for the 'Piece' class. It takes the name and color of the piece as arguments.
    public Piece(String name, String color) 
    {
        this.name = name;
        this.color = color;
    }

    // It allows other classes to access the name of the piece without directly modifying it
    // encapsulation
    public String getName() 
    {
        return name;
    }
    
    public String getNameInitial() 
    {
        return (name.equals("Knight")) ? "N" : name.substring(0, 1).toUpperCase();
    }

    // It allows other classes to access the color of the piece without directly modifying it
    // encapsulation
    public String getColor() 
    {
        return color;
    }

    // An abstract method named 'canMove'.
    // Abstract methods do not have an implementation in the abstract class.
    // Subclasses of 'Piece' must provide their own implementation for this method
    // (abstraction and polymorphism).
    // This method will determine if a piece can move from a starting position to an ending position
    // on the chessboard. The parameters represent the starting and ending row and column.
    public abstract boolean canMove(int startRow, int startCol, int endRow, int endCol, Board board);

    // Overriding the 'toString' method from the Object class.
    // This method is called when an object of the 'Piece' class (or its subclasses) needs to be
    // represented as a String.
    @Override
    public String toString() 
    {
        // Returns a string representation of the piece, including its color and name.
        return color + " " + name;
    }
}
