package ChessGui;

import javax.swing.*;
import java.awt.*;

/**
 * The main window for the chess game GUI.
 * It holds the BoardPanel, a status label, a side panel for move logging,
 * and controls for starting a new game.
 */
public class ChessGUI extends JFrame {

    private final BoardPanel boardPanel;
    private final JLabel statusLabel;
    private final JTextArea moveLogArea;
    private final JButton newGameButton;
    private final JButton resignButton;

    private final ChessGame game;

    public ChessGUI() {
        this.game = new ChessGame();
        this.game.setGui(this);

        setTitle("Java Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Welcome to Chess! Click 'New Game' to begin.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(game);
        add(boardPanel, BorderLayout.CENTER);

        // Side Panel for logs and controls
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(220, 0));

        moveLogArea = new JTextArea();
        moveLogArea.setEditable(false);
        moveLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(moveLogArea);
        sidePanel.add(logScrollPane, BorderLayout.CENTER);

        // Panel for buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        
        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> new Thread(game::startGame).start());
        buttonPanel.add(newGameButton);
        
        resignButton = new JButton("Resign");
        resignButton.setEnabled(false); // Disabled until a game starts
        resignButton.addActionListener(e -> game.resign());
        buttonPanel.add(resignButton);
        
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);

        add(sidePanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Triggers a redraw of the chessboard.
     */
    public void updateBoard() {
        boardPanel.repaint();
    }

    /**
     * Updates the status message shown to the user (e.g., whose turn it is).
     * @param message The text to display.
     */
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    /**
     * Appends a message to the move log text area.
     * @param message The text to log.
     */
    public void logMessage(String message) {
        // Ensure this GUI update happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            moveLogArea.append(message + "\n");
            // Auto-scroll to the bottom
            moveLogArea.setCaretPosition(moveLogArea.getDocument().getLength());
        });
    }

    /**
     * Enables or disables the "New Game" button.
     * @param enabled true to enable, false to disable.
     */
    public void setGameControlsEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
    }
    
    /**
     * Enables or disables the "New Game" button.
     * @param enabled true to enable, false to disable.
     */
    public void setNewGameButtonEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
    }
    
    /**
     * Enables or disables the "Resign" button.
     * @param enabled true to enable, false to disable.
     */
    public void setResignButtonEnabled(boolean enabled) {
        resignButton.setEnabled(enabled);
    }
}