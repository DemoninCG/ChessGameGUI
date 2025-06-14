package ChessGui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages player and match data using an embedded Apache Derby database.
 * This class handles database connection, table creation, and all
 * read/write operations for player statistics and match history.
 */
public class PlayerData {
    public static final int K = 32; // Elo K-factor for rating calculations
    private static final double DEFAULT_ELO = 1000.0;
    private static final String DB_CONNECTION_URL = "jdbc:derby:ChessPlayerDB;create=true";
    private static final String PLAYERS_TABLE_NAME = "PLAYERS";
    private static final String MATCHES_TABLE_NAME = "MATCHES";
    private final ChessGame game;
    private Connection dbConnection;

    // Data Transfer Objects (DTOs) for cleaner code
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
    
    private static class MatchRecord {
        final String whitePlayer;
        final String blackPlayer;
        final String winner; // Can be null for a tie

        MatchRecord(String white, String black, String winner) {
            this.whitePlayer = white;
            this.blackPlayer = black;
            this.winner = winner;
        }
    }


    public PlayerData(ChessGame game) {
        this.game = game;
        initializeDatabase();
        registerShutdownHook();
    }

    private void initializeDatabase() {
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION_URL);
            createTablesIfNotExists();
        } catch (SQLException e) {
            handleSQLException(e, "Database initialization failed. The application may not function correctly.");
        }
    }

    private void createTablesIfNotExists() {
        try (Statement stmt = dbConnection.createStatement()) {
            // Check for PLAYERS table
            if (!tableExists(PLAYERS_TABLE_NAME)) {
                String createPlayersSQL = "CREATE TABLE " + PLAYERS_TABLE_NAME + " ("
                        + "name VARCHAR(50) NOT NULL PRIMARY KEY, "
                        + "wins INT DEFAULT 0, "
                        + "losses INT DEFAULT 0, "
                        + "ties INT DEFAULT 0, "
                        + "elo DOUBLE DEFAULT " + DEFAULT_ELO + ")";
                stmt.execute(createPlayersSQL);
                game.log("Created PLAYERS table in the database.");
            }

            // Check for MATCHES table
            if (!tableExists(MATCHES_TABLE_NAME)) {
                String createMatchesSQL = "CREATE TABLE " + MATCHES_TABLE_NAME + " ("
                        + "match_id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                        + "white_player_name VARCHAR(50) NOT NULL, "
                        + "black_player_name VARCHAR(50) NOT NULL, "
                        + "winner_name VARCHAR(50), " // Can be NULL
                        + "PRIMARY KEY (match_id), "
                        + "FOREIGN KEY (white_player_name) REFERENCES " + PLAYERS_TABLE_NAME + "(name), "
                        + "FOREIGN KEY (black_player_name) REFERENCES " + PLAYERS_TABLE_NAME + "(name)"
                        + ")";
                stmt.execute(createMatchesSQL);
                game.log("Created MATCHES table in the database.");
            }
        } catch (SQLException e) {
            handleSQLException(e, "Error during table creation/check.");
        }
    }
    
    private boolean tableExists(String tableName) throws SQLException {
        ResultSet rs = dbConnection.getMetaData().getTables(null, "APP", tableName.toUpperCase(), null);
        return rs.next();
    }
    
    public void checkOrAddPlayer(String playerName) {
        String sql = "SELECT elo FROM " + PLAYERS_TABLE_NAME + " WHERE name = ?";
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

    private void addNewPlayer(String playerName) {
        String sql = "INSERT INTO " + PLAYERS_TABLE_NAME + " (name, elo) VALUES (?, ?)";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setDouble(2, DEFAULT_ELO);
            pstmt.executeUpdate();
            game.log("New player '" + playerName + "' added.");
        } catch (SQLException e) {
            handleSQLException(e, "Error adding new player '" + playerName + "'.");
        }
    }

    public void updateGameResults(String whitePlayerName, String blackPlayerName, int gameResult) {
        try {
            PlayerStats whitePlayer = getPlayerStatsFromDB(whitePlayerName);
            PlayerStats blackPlayer = getPlayerStatsFromDB(blackPlayerName);

            if (whitePlayer == null || blackPlayer == null) {
                game.log("Error: Could not retrieve player data to update scores.");
                return;
            }

            // --- Elo and Stat Calculation ---
            double oldWhiteElo = whitePlayer.elo;
            double oldBlackElo = blackPlayer.elo;
            double expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (oldBlackElo - oldWhiteElo) / 400.0));
            double actualWhiteScore;
            String winnerName = null;

            if (gameResult == 1) { // White wins
                whitePlayer.wins++; blackPlayer.losses++; actualWhiteScore = 1.0; winnerName = whitePlayerName;
            } else if (gameResult == -1) { // Black wins
                whitePlayer.losses++; blackPlayer.wins++; actualWhiteScore = 0.0; winnerName = blackPlayerName;
            } else { // Draw
                whitePlayer.ties++; blackPlayer.ties++; actualWhiteScore = 0.5;
            }

            whitePlayer.elo += K * (actualWhiteScore - expectedWhite);
            blackPlayer.elo += K * ((1.0 - actualWhiteScore) - (1.0 - expectedWhite));

            // --- Database Updates in a Transaction ---
            dbConnection.setAutoCommit(false); // Start transaction
            updatePlayerStatsInDB(whitePlayer);
            updatePlayerStatsInDB(blackPlayer);
            recordMatchInDB(whitePlayerName, blackPlayerName, winnerName);
            dbConnection.commit(); // Commit transaction

            // --- Logging ---
            game.log("\n--- Elo & Record Updates ---");
            logPlayerUpdate(whitePlayer, oldWhiteElo);
            logPlayerUpdate(blackPlayer, oldBlackElo);

        } catch (SQLException e) {
            handleSQLException(e, "A database error occurred while updating scores.");
            try { dbConnection.rollback(); } catch (SQLException ex) { /* Ignore rollback error */ }
        } finally {
            try { dbConnection.setAutoCommit(true); } catch (SQLException ex) { /* Ignore */}
        }
    }
    
    private void recordMatchInDB(String whiteName, String blackName, String winnerName) throws SQLException {
        String sql = "INSERT INTO " + MATCHES_TABLE_NAME + " (white_player_name, black_player_name, winner_name) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, whiteName);
            pstmt.setString(2, blackName);
            if (winnerName != null) {
                pstmt.setString(3, winnerName);
            } else {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            }
            pstmt.executeUpdate();
        }
    }
    
    public String getMatchHistory() {
        StringBuilder history = new StringBuilder();
        String sql = "SELECT match_id, white_player_name, black_player_name, winner_name "
                   + "FROM " + MATCHES_TABLE_NAME + " ORDER BY match_id DESC FETCH FIRST 10 ROWS ONLY";

        try (Statement stmt = dbConnection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while(rs.next()) {
                String white = rs.getString("white_player_name");
                String black = rs.getString("black_player_name");
                String winner = rs.getString("winner_name");
                String result;
                if (winner == null) {
                    result = "Draw";
                } else {
                    result = winner + " won";
                }
                history.append(String.format("ID %d: %s (W) vs %s (B) - %s%n", rs.getInt("match_id"), white, black, result));
                count++;
            }
            if (count == 0) {
                return "No matches have been played yet.";
            }
        } catch(SQLException e) {
            handleSQLException(e, "Could not fetch match history.");
            return "Error: Could not fetch match history.";
        }
        return history.toString();
    }
    
    public void recalculateAllRankings() {
        game.log("Starting full ranking recalculation...");
        Map<String, PlayerStats> currentStatsMap = new HashMap<>();

        try {
            dbConnection.setAutoCommit(false); // --- Start Transaction ---
            
            // 1. Reset all player stats in DB to default
            try (Statement stmt = dbConnection.createStatement()) {
                stmt.executeUpdate("UPDATE " + PLAYERS_TABLE_NAME 
                    + " SET wins = 0, losses = 0, ties = 0, elo = " + DEFAULT_ELO);
            }
            
            // 2. Load the reset players into an in-memory map
            try (Statement stmt = dbConnection.createStatement(); 
                 ResultSet rs = stmt.executeQuery("SELECT name, elo FROM " + PLAYERS_TABLE_NAME)) {
                while(rs.next()) {
                    String name = rs.getString("name");
                    currentStatsMap.put(name, new PlayerStats(name, 0, 0, 0, DEFAULT_ELO));
                }
            }
            
            // 3. Get all matches in chronological order
            List<MatchRecord> allMatches = new ArrayList<>();
            try (Statement stmt = dbConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT white_player_name, black_player_name, winner_name FROM " 
                                                    + MATCHES_TABLE_NAME + " ORDER BY match_id ASC")) {
                while(rs.next()) {
                    allMatches.add(new MatchRecord(
                        rs.getString("white_player_name"),
                        rs.getString("black_player_name"),
                        rs.getString("winner_name")
                    ));
                }
            }

            // 4. Re-simulate each game's outcome on the in-memory map
            for (MatchRecord match : allMatches) {
                PlayerStats white = currentStatsMap.get(match.whitePlayer);
                PlayerStats black = currentStatsMap.get(match.blackPlayer);

                double expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (black.elo - white.elo) / 400.0));
                double actualWhiteScore;

                if (match.winner == null) { // Draw
                    white.ties++; black.ties++; actualWhiteScore = 0.5;
                } else if (match.winner.equals(white.name)) { // White won
                    white.wins++; black.losses++; actualWhiteScore = 1.0;
                } else { // Black won
                    white.losses++; black.wins++; actualWhiteScore = 0.0;
                }
                
                white.elo += K * (actualWhiteScore - expectedWhite);
                black.elo += K * ((1.0 - actualWhiteScore) - (1.0 - expectedWhite));
            }

            // 5. Write the final calculated stats back to the database in a batch update
            String updateSQL = "UPDATE " + PLAYERS_TABLE_NAME + " SET wins = ?, losses = ?, ties = ?, elo = ? WHERE name = ?";
            try (PreparedStatement pstmt = dbConnection.prepareStatement(updateSQL)) {
                for (PlayerStats player : currentStatsMap.values()) {
                    pstmt.setInt(1, player.wins);
                    pstmt.setInt(2, player.losses);
                    pstmt.setInt(3, player.ties);
                    pstmt.setDouble(4, player.elo);
                    pstmt.setString(5, player.name);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            dbConnection.commit(); // --- Commit Transaction ---
            game.log("Recalculation complete. All player stats have been updated.");

        } catch (SQLException e) {
            handleSQLException(e, "Recalculation failed. Rolling back changes.");
            try { dbConnection.rollback(); } catch (SQLException ex) { /* ignore */ }
        } finally {
            try { dbConnection.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
        }
    }

    // --- Helper and Utility Methods from previous version (unchanged) ---

    private PlayerStats getPlayerStatsFromDB(String playerName) throws SQLException {
        String sql = "SELECT wins, losses, ties, elo FROM " + PLAYERS_TABLE_NAME + " WHERE name = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(playerName, rs.getInt("wins"), rs.getInt("losses"), rs.getInt("ties"), rs.getDouble("elo"));
            }
        }
        return null;
    }

    private void updatePlayerStatsInDB(PlayerStats player) throws SQLException {
        String sql = "UPDATE " + PLAYERS_TABLE_NAME + " SET wins = ?, losses = ?, ties = ?, elo = ? WHERE name = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, player.wins);
            pstmt.setInt(2, player.losses);
            pstmt.setInt(3, player.ties);
            pstmt.setDouble(4, player.elo);
            pstmt.setString(5, player.name);
            pstmt.executeUpdate();
        }
    }
    
    private void logPlayerUpdate(PlayerStats player, double oldElo) {
        game.log(String.format("%s: %.1f -> %.1f Elo", player.name, oldElo, player.elo));
        game.log(String.format("Record: %d W / %d L / %d T", player.wins, player.losses, player.ties));
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");
                }
            } catch (SQLException se) {
                if (!"XJ015".equals(se.getSQLState())) {
                    handleSQLException(se, "Error during database shutdown.");
                } else {
                    System.out.println("Derby database shut down successfully.");
                }
            }
        }));
    }

    private void handleSQLException(SQLException e, String customMessage) {
        System.err.println("--- DATABASE ERROR ---");
        System.err.println("Custom Message: " + customMessage);
        e.printStackTrace(System.err);
        game.log("Database Error: " + e.getMessage());
    }
}