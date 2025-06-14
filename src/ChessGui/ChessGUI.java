package ChessGui;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Corban Guy, Naz Janif
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

        // Chess board panel
        boardPanel = new BoardPanel(game);
        add(boardPanel, BorderLayout.CENTER);

        // Side Panel for logs and controls
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(220, 0));

        moveLogArea = new JTextArea();
        moveLogArea.setEditable(false);
        moveLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        moveLogArea.setBackground(Color.DARK_GRAY);
        moveLogArea.setForeground(Color.WHITE);
        moveLogArea.setLineWrap(true);
        moveLogArea.setWrapStyleWord(true);
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

    // Triggers a redraw of the chess board
    public void updateBoard() {
        boardPanel.repaint();
    }

    // Updates the status message shown to the user (e.g., whose turn it is)
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    // Appends a message to the move log text area
    public void logMessage(String message) {
        // Ensure this GUI update happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            moveLogArea.append(message + "\n");
            // Auto-scroll to the bottom
            moveLogArea.setCaretPosition(moveLogArea.getDocument().getLength());
        });
    }

    //Enabled/disables certain elements based on whether a game is currently running
    public void setGameControlsEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
    }
    
    public void setNewGameButtonEnabled(boolean enabled) {
        newGameButton.setEnabled(enabled);
    }
    
    public void setResignButtonEnabled(boolean enabled) {
        resignButton.setEnabled(enabled);
    }
}