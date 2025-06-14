package ChessGui;

import javax.swing.*;
import java.awt.*;

public class ChessGUI extends JFrame {

    private final BoardPanel boardPanel;
    private final JLabel statusLabel;
    private final JTextArea moveLogArea;
    private final JButton newGameButton;
    private final JButton resignButton;
    private final JButton viewHistoryButton;
    private final JButton recalculateButton;

    private final ChessGame game;

    public ChessGUI() {
        this.game = new ChessGame();
        this.game.setGui(this);

        setTitle("Java Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout(5, 5)); // Add some gaps

        statusLabel = new JLabel("Welcome to Chess! Click 'New Game' to begin.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new BoardPanel(game);
        add(boardPanel, BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new BorderLayout(0, 10)); // Vertical gap
        sidePanel.setPreferredSize(new Dimension(250, 0)); // Wider panel
        sidePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5)); // Add padding

        moveLogArea = new JTextArea();
        moveLogArea.setEditable(false);
        moveLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        moveLogArea.setBackground(Color.DARK_GRAY);
        moveLogArea.setForeground(Color.WHITE);
        moveLogArea.setLineWrap(true);
        moveLogArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(moveLogArea);
        sidePanel.add(logScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5)); // 2x2 grid for buttons
        
        newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> new Thread(game::startGame).start());
        buttonPanel.add(newGameButton);
        
        resignButton = new JButton("Resign");
        resignButton.addActionListener(e -> game.resign());
        buttonPanel.add(resignButton);

        viewHistoryButton = new JButton("View Match History");
        viewHistoryButton.addActionListener(e -> showMatchHistory());
        buttonPanel.add(viewHistoryButton);
        
        recalculateButton = new JButton("Recalculate Rankings");
        recalculateButton.addActionListener(e -> handleRecalculateRankings());
        buttonPanel.add(recalculateButton);

        sidePanel.add(buttonPanel, BorderLayout.SOUTH);

        add(sidePanel, BorderLayout.EAST);
        
        setGameInProgress(false); // Set initial button states

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void showMatchHistory() {
        String history = game.getMatchHistory();
        JTextArea textArea = new JTextArea(history);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 250));
        JOptionPane.showMessageDialog(this, scrollPane, "Recent Match History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRecalculateRankings() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "This will reset all stats and recalculate them from the match history.\nThis cannot be undone. Are you sure?",
            "Confirm Recalculation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            // Run in a separate thread to avoid freezing the GUI
            new Thread(() -> {
                game.recalculateRankings();
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, "All player rankings have been recalculated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE)
                );
            }).start();
        }
    }

    public void updateBoard() {
        boardPanel.repaint();
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            moveLogArea.append(message + "\n");
            moveLogArea.setCaretPosition(moveLogArea.getDocument().getLength());
        });
    }

    /**
     * Toggles the state of all control buttons based on whether a game is active.
     * @param inProgress True if a game is being played, false otherwise.
     */
    public void setGameInProgress(boolean inProgress) {
        newGameButton.setEnabled(!inProgress);
        viewHistoryButton.setEnabled(!inProgress);
        recalculateButton.setEnabled(!inProgress);
        resignButton.setEnabled(inProgress);
    }
}