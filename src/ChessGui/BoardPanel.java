package ChessGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class BoardPanel extends JPanel {

    public static final int TILE_SIZE = 80;
    private final Color LIGHT_SQUARE_COLOR = new Color(238, 238, 210);
    private final Color DARK_SQUARE_COLOR = new Color(118, 150, 86);
    private final Color HIGHLIGHT_COLOR = new Color(130, 170, 255, 150); // Semi-transparent blue

    private final ChessGame game;
    private final Board board;
    private final Map<String, Image> pieceImages;

    // State for handling drag-and-drop
    private Piece selectedPiece;
    private int startRow, startCol;
    private int dragX, dragY; // To draw the piece at the cursor's location
    private List<Point> legalMoveDestinations;

    public BoardPanel(ChessGame game) {
        this.game = game;
        this.board = game.getBoard(); // Get the board from the game
        this.pieceImages = new HashMap<>();
        this.legalMoveDestinations = new ArrayList<>();
        loadPieceImages();

        setPreferredSize(new Dimension(Board.SIZE * TILE_SIZE, Board.SIZE * TILE_SIZE));

        MouseHandler handler = new MouseHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private void loadPieceImages() {
        String[] pieceNames = {"Pawn", "Rook", "Knight", "Bishop", "Queen", "King"};
        for (String name : pieceNames) {
            String whiteKey = "white" + name;
            String blackKey = "black" + name;
            String whitePath = "resources/pieces-png/white-" + name.toLowerCase() + ".png";
            String blackPath = "resources/pieces-png/black-" + name.toLowerCase() + ".png";

            try {
                pieceImages.put(whiteKey, new ImageIcon(whitePath).getImage());
                pieceImages.put(blackKey, new ImageIcon(blackPath).getImage());
            } catch (Exception e) {
                System.err.println("Could not load image for: " + name);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                drawSquare(g2d, row, col);
                // Draw piece, unless it's the one being dragged
                if (selectedPiece == null || !(row == startRow && col == startCol)) {
                    drawPiece(g2d, row, col);
                }
            }
        }

        // Highlight legal move destinations
        drawHighlights(g2d);

        // Draw the selected piece at the cursor's location
        if (selectedPiece != null) {
            drawDraggedPiece(g2d);
        }
    }

    private void drawSquare(Graphics2D g, int row, int col) {
        g.setColor((row + col) % 2 == 0 ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);
        g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
    }

    // Draws piece icons
    private void drawPiece(Graphics2D g, int row, int col) {
        Piece piece = board.getPiece(row, col);
        if (piece != null) {
            String imageKey = piece.getColor() + piece.getName();
            Image pieceImage = pieceImages.get(imageKey);
            if (pieceImage != null) {
                g.drawImage(pieceImage, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, this);
            } else { // Fallback drawing
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 30));
                g.drawString(piece.getNameInitial(), col * TILE_SIZE + 25, row * TILE_SIZE + 50);
            }
        }
    }

    private void drawHighlights(Graphics2D g) {
        g.setColor(HIGHLIGHT_COLOR);
        for (Point dest : legalMoveDestinations) {
            g.fillOval(dest.y * TILE_SIZE + TILE_SIZE / 4, dest.x * TILE_SIZE + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
        }
    }

    private void drawDraggedPiece(Graphics2D g) {
        String imageKey = selectedPiece.getColor() + selectedPiece.getName();
        Image pieceImage = pieceImages.get(imageKey);
        if (pieceImage != null) {
            // Center the image on the cursor
            g.drawImage(pieceImage, dragX - TILE_SIZE / 2, dragY - TILE_SIZE / 2, TILE_SIZE, TILE_SIZE, this);
        }
    }

    // Inner class to handle mouse events
    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (game.isGameOver()) return;

            int col = e.getX() / TILE_SIZE;
            int row = e.getY() / TILE_SIZE;

            Piece clickedPiece = board.getPiece(row, col);
            if (clickedPiece != null && clickedPiece.getColor().equals(game.getCurrentPlayer())) {
                selectedPiece = clickedPiece;
                startRow = row;
                startCol = col;
                dragX = e.getX();
                dragY = e.getY();
                legalMoveDestinations = game.getLegalMovesForPiece(startRow, startCol);
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedPiece != null) {
                dragX = e.getX();
                dragY = e.getY();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (selectedPiece != null) {
                int endCol = e.getX() / TILE_SIZE;
                int endRow = e.getY() / TILE_SIZE;

                // Attempt to make the move by notifying the main game logic
                game.attemptMove(startRow, startCol, endRow, endCol);

                // Reset state regardless of move success
                selectedPiece = null;
                legalMoveDestinations.clear();
                repaint();
            }
        }
    }
}