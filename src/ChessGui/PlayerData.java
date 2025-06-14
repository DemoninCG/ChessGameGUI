package ChessGui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages player data using an embedded Apache Derby database.
 * This class handles database connection, table creation, and all
 * read/write operations for player statistics.
 */
public class PlayerData {
    public static final int K = 32; // Elo K-factor for rating calculations
    private static final double DEFAULT_ELO = 1000.0;
    private static final String DB_CONNECTION_URL = "jdbc:derby:ChessPlayerDB;create=true";
    private static final String TABLE_NAME = "PLAYERS";
    private final ChessGame game;
    private Connection dbConnection;

    /**
     * Inner class to act as a data transfer object for player stats.
     */
    private static class PlayerStats {
        String name;
        int wins;
        int losses;
        int ties;
        double elo;

        PlayerStats(String name, int wins, int losses, int ties, double elo) {
            this.name = name;
            this.wins = wins;
            this.losses = losses;
            this.ties = ties;
            this.elo = elo;
        }
    }

    /**
     * Constructor for PlayerData. Initializes the database connection.
     * @param game A reference to the main ChessGame logic.
     */
    public PlayerData(ChessGame game) {
        this.game = game;
        initializeDatabase();
        registerShutdownHook();
    }

    /**
     * Establishes a connection to the Derby database and creates the
     * player table if it does not already exist.
     */
    private void initializeDatabase() {
        try {
            // Modern JDBC drivers are loaded automatically from the classpath.
            dbConnection = DriverManager.getConnection(DB_CONNECTION_URL);
            createPlayerTableIfNotExists();
        } catch (SQLException e) {
            handleSQLException(e, "Database initialization failed. The application may not function correctly.");
        }
    }

    /**
     * Checks for the existence of the PLAYERS table and creates it if it's missing.
     * The table stores player name, wins, losses, ties, and Elo rating.
     */
    private void createPlayerTableIfNotExists() {
        // Use try-with-resources to ensure the Statement is closed.
        try (Statement stmt = dbConnection.createStatement()) {
            // Check the database's system catalog to see if the table exists.
            ResultSet rs = dbConnection.getMetaData().getTables(null, "APP", TABLE_NAME, null);
            if (!rs.next()) {
                String createTableSQL = "CREATE TABLE " + TABLE_NAME + " ("
                        + "name VARCHAR(50) NOT NULL PRIMARY KEY, "
                        + "wins INT DEFAULT 0, "
                        + "losses INT DEFAULT 0, "
                        + "ties INT DEFAULT 0, "
                        + "elo DOUBLE DEFAULT " + DEFAULT_ELO + ")";
                stmt.execute(createTableSQL);
                game.log("Created PLAYERS table in the database.");
            }
        } catch (SQLException e) {
            // Derby throws an exception if the table already exists, which can be ignored.
            // The SQLState for "table/view already exists" is X0Y32.
            if (!e.getSQLState().equals("X0Y32")) {
                handleSQLException(e, "Error during table creation/check.");
            }
        }
    }

    /**
     * Searches for a player in the database by name. If the player is not found,
     * a new record is created for them with default statistics.
     * @param playerName The name of the player to check.
     */
    public void checkOrAddPlayer(String playerName) {
        String sql = "SELECT elo FROM " + TABLE_NAME + " WHERE name = ?";
        // Use try-with-resources for the PreparedStatement.
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double elo = rs.getDouble("elo");
                game.log("Player '" + playerName + "' found. Elo: " + String.format("%.1f", elo));
            } else {
                addNewPlayer(playerName);
            }
        } catch (SQLException e) {
            handleSQLException(e, "Error searching for player '" + playerName + "'.");
        }
    }

    /**
     * Inserts a new player record into the database with default stats.
     * @param playerName The name of the new player.
     */
    private void addNewPlayer(String playerName) {
        String sql = "INSERT INTO " + TABLE_NAME + " (name, elo) VALUES (?, ?)";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setDouble(2, DEFAULT_ELO);
            pstmt.executeUpdate();
            game.log("New player '" + playerName + "' added.");
        } catch (SQLException e) {
            handleSQLException(e, "Error adding new player '" + playerName + "'.");
        }
    }

    /**
     * Updates the records and Elo ratings for both players after a game concludes.
     * @param whitePlayerName The name of the player who was white.
     * @param blackPlayerName The name of the player who was black.
     * @param gameResult 1 if white won, -1 if black won, 0 for a draw.
     */
    public void updateGameResults(String whitePlayerName, String blackPlayerName, int gameResult) {
        try {
            PlayerStats whitePlayer = getPlayerStatsFromDB(whitePlayerName);
            PlayerStats blackPlayer = getPlayerStatsFromDB(blackPlayerName);

            if (whitePlayer == null || blackPlayer == null) {
                game.log("Error: Could not retrieve player data to update scores.");
                return;
            }

            double oldWhiteElo = whitePlayer.elo;
            double oldBlackElo = blackPlayer.elo;

            // Elo calculation logic
            double expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (oldBlackElo - oldWhiteElo) / 400.0));
            double actualWhiteScore;

            if (gameResult == 1) { // White wins
                whitePlayer.wins++;
                blackPlayer.losses++;
                actualWhiteScore = 1.0;
            } else if (gameResult == -1) { // Black wins
                whitePlayer.losses++;
                blackPlayer.wins++;
                actualWhiteScore = 0.0;
            } else { // Draw
                whitePlayer.ties++;
                blackPlayer.ties++;
                actualWhiteScore = 0.5;
            }

            whitePlayer.elo += K * (actualWhiteScore - expectedWhite);
            // Black's score is the inverse of white's
            blackPlayer.elo += K * ((1.0 - actualWhiteScore) - (1.0 - expectedWhite));

            // Write updated stats back to the database
            updatePlayerStatsInDB(whitePlayer);
            updatePlayerStatsInDB(blackPlayer);

            // Log results to the GUI
            game.log("\n--- Elo & Record Updates ---");
            logPlayerUpdate(whitePlayer, oldWhiteElo);
            logPlayerUpdate(blackPlayer, oldBlackElo);

        } catch (SQLException e) {
            handleSQLException(e, "A database error occurred while updating scores.");
        }
    }

    /**
     * Retrieves all statistics for a single player from the database.
     * @param playerName The name of the player to retrieve.
     * @return A PlayerStats object, or null if the player is not found.
     * @throws SQLException if a database access error occurs.
     */
    private PlayerStats getPlayerStatsFromDB(String playerName) throws SQLException {
        String sql = "SELECT wins, losses, ties, elo FROM " + TABLE_NAME + " WHERE name = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                    playerName,
                    rs.getInt("wins"),
                    rs.getInt("losses"),
                    rs.getInt("ties"),
                    rs.getDouble("elo")
                );
            }
        }
        return null;
    }

    /**
     * Writes a player's updated statistics to the database using an UPDATE query.
     * @param player The PlayerStats object containing the new data.
     * @throws SQLException if a database access error occurs.
     */
    private void updatePlayerStatsInDB(PlayerStats player) throws SQLException {
        String sql = "UPDATE " + TABLE_NAME + " SET wins = ?, losses = ?, ties = ?, elo = ? WHERE name = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, player.wins);
            pstmt.setInt(2, player.losses);
            pstmt.setInt(3, player.ties);
            pstmt.setDouble(4, player.elo);
            pstmt.setString(5, player.name);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Logs a player's updated stats to the game's message area.
     * @param player The player whose stats are being logged.
     * @param oldElo The player's Elo rating before the game.
     */
    private void logPlayerUpdate(PlayerStats player, double oldElo) {
        game.log(String.format("%s: %.1f -> %.1f Elo", player.name, oldElo, player.elo));
        game.log(String.format("Record: %d W / %d L / %d T", player.wins, player.losses, player.ties));
    }

    /**
     * Registers a JVM shutdown hook to ensure the database is closed properly
     * when the application exits, preventing database corruption.
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    // Derby's official shutdown method is to request a connection with shutdown=true
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");
                }
            } catch (SQLException se) {
                // A successful shutdown of Derby throws SQLState "XJ015". This is expected.
                if (!"XJ015".equals(se.getSQLState())) {
                    handleSQLException(se, "Error during database shutdown.");
                } else {
                    System.out.println("Derby database shut down successfully.");
                }
            }
        }));
    }

    /**
     * Centralized handler for logging SQL exceptions to standard error and the game log.
     * @param e The SQLException that was thrown.
     * @param customMessage A descriptive message about the context of the error.
     */
    private void handleSQLException(SQLException e, String customMessage) {
        System.err.println("--- DATABASE ERROR ---");
        System.err.println("Custom Message: " + customMessage);
        e.printStackTrace(System.err);
        game.log("Database Error: " + e.getMessage());
    }
}