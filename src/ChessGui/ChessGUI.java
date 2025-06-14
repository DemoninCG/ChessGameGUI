package ChessGui;

import javax.swing.*;
import java.awt.*;

/**
 * The main window for the chess game GUI.
 * It holds the BoardPanel and other UI components like a status label.
 */
public class ChessGUI extends JFrame {
    private final BoardPanel boardPanel;
    private final JLabel statusLabel;

    /**
     * Constructs the main GUI window.
     * @param game The main game logic controller.
     */
    public ChessGUI(ChessGame game) {
        setTitle("Java Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // The BoardPanel needs a reference to the game to send move requests
        boardPanel = new BoardPanel(game);
        add(boardPanel, BorderLayout.CENTER);

        // Status label to show whose turn it is
        statusLabel = new JLabel("Enter player names in the console to begin.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setPreferredSize(new Dimension(getWidth(), 30));
        add(statusLabel, BorderLayout.NORTH);

        pack(); // Sizes the frame so all contents are at their preferred sizes
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true);
    }

    /**
     * Triggers a redraw of the chessboard.
     * This should be called after any change in the board's state.
     */
    public void updateBoard() {
        boardPanel.repaint();
    }

    /**
     * Updates the status message shown to the user.
     * @param message The text to display.
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }
}