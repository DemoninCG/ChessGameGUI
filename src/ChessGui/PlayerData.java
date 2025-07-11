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
 *
 * @author Corban Guy, Naz Janif
 */
public class PlayerData {
    public static final int K = 32; // Elo K-factor for rating calculations
    private static final double DEFAULT_ELO = 1000.0;
    private static final String DB_CONNECTION_URL = "jdbc:derby:ChessPlayerDB;create=true";
    private static final String PLAYERS_TABLE_NAME = "PLAYERS";
    private static final String MATCHES_TABLE_NAME = "MATCHES";
    private final ChessGame game;
    private Connection dbConnection;

    // Data transfer objects for cleaner code
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

    // Create player and match tables if they don't currently exist
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
    
    // Searches for a player in the database, adds them if they don't exist (new player)
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

            // Elo and stat calculation
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

            // Database updates in a transaction
            dbConnection.setAutoCommit(false); // Start transaction
            updatePlayerStatsInDB(whitePlayer);
            updatePlayerStatsInDB(blackPlayer);
            recordMatchInDB(whitePlayerName, blackPlayerName, winnerName);
            dbConnection.commit(); // Commit transaction

            // Logging
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
            dbConnection.setAutoCommit(false); // Start Transaction
            
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

            // 4. Re-simulate each game's outcome
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

            dbConnection.commit(); // Commit transaction
            game.log("Recalculation complete. All player stats have been updated.");

        } catch (SQLException e) {
            handleSQLException(e, "Recalculation failed. Rolling back changes.");
            try { dbConnection.rollback(); } catch (SQLException ex) { /* ignore */ }
        } finally {
            try { dbConnection.setAutoCommit(true); } catch (SQLException ex) { /* ignore */ }
        }
    }
    
    // Debug functions, not accessible by players but useful for viewing/modifying the database if needed
    
    // Prints the full list of players and their stats to the console
    public void debugPrintAllPlayers() {
        System.out.println("\n--- DEBUG: PLAYER LIST ---");
        String sql = "SELECT name, wins, losses, ties, elo FROM " + PLAYERS_TABLE_NAME + " ORDER BY elo DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.isBeforeFirst()) { // Check if the result set is empty
                System.out.println("No players found in the database.");
                return;
            }

            System.out.println(String.format("%-15s | %-10s | %-5s | %-5s | %-5s", "Name", "Elo", "W", "L", "T"));
            System.out.println("----------------+------------+-------+-------+-------");

            while (rs.next()) {
                String name = rs.getString("name");
                double elo = rs.getDouble("elo");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int ties = rs.getInt("ties");
                System.out.println(String.format("%-15s | %-10.1f | %-5d | %-5d | %-5d", name, elo, wins, losses, ties));
            }

        } catch (SQLException e) {
            handleSQLException(e, "Debug function 'debugPrintAllPlayers' failed.");
        }
        System.out.println("--- END PLAYER LIST ---\n");
    }

    // Prints the full list of all matches played to the console
    public void debugPrintAllMatches() {
        System.out.println("\n--- DEBUG: MATCH HISTORY (ALL) ---");
        String sql = "SELECT match_id, white_player_name, black_player_name, winner_name FROM " + MATCHES_TABLE_NAME + " ORDER BY match_id ASC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (!rs.isBeforeFirst()) {
                System.out.println("No matches found in the database.");
                return;
            }
            
            System.out.println(String.format("%-5s | %-15s | %-15s | %-15s", "ID", "White", "Black", "Result"));
            System.out.println("------+-----------------+-----------------+-----------------");

            while (rs.next()) {
                int id = rs.getInt("match_id");
                String white = rs.getString("white_player_name");
                String black = rs.getString("black_player_name");
                String winner = rs.getString("winner_name");
                
                String result = (winner == null) ? "Draw" : winner + " won";
                
                System.out.println(String.format("%-5d | %-15s | %-15s | %-15s", id, white, black, result));
            }

        } catch (SQLException e) {
            handleSQLException(e, "Debug function 'debugPrintAllMatches' failed.");
        }
        System.out.println("--- END MATCH HISTORY ---\n");
    }

    // Adds a new player with a given name and default stats (does nothing if a player with that name already exists)
    public void debugAddPlayer(String playerName) {
        // First check if player exists
        String checkSql = "SELECT name FROM " + PLAYERS_TABLE_NAME + " WHERE name = ?";
        try (PreparedStatement checkPstmt = dbConnection.prepareStatement(checkSql)) {
            checkPstmt.setString(1, playerName);
            ResultSet rs = checkPstmt.executeQuery();
            if (rs.next()) {
                System.out.println("DEBUG: Player '" + playerName + "' already exists. No action taken.");
                return;
            }
        } catch (SQLException e) {
            handleSQLException(e, "Debug function 'debugAddPlayer' failed during check for player '" + playerName + "'.");
            return; // Exit if check fails
        }
        
        // Player does not exist, so add them
        System.out.println("DEBUG: Attempting to add new player '" + playerName + "'...");
        String insertSql = "INSERT INTO " + PLAYERS_TABLE_NAME + " (name, elo) VALUES (?, ?)";
        try (PreparedStatement insertPstmt = dbConnection.prepareStatement(insertSql)) {
            insertPstmt.setString(1, playerName);
            insertPstmt.setDouble(2, DEFAULT_ELO);
            int rowsAffected = insertPstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("DEBUG: Successfully added player '" + playerName + "' with default Elo of " + DEFAULT_ELO);
            } else {
                System.out.println("DEBUG: Failed to add player '" + playerName + "'.");
            }
        } catch (SQLException e) {
            handleSQLException(e, "Debug function 'debugAddPlayer' failed during insertion of player '" + playerName + "'.");
        }
    }

    // Manually sets the Elo rating for a specific player
    public void debugSetPlayerElo(String playerName, double newElo) {
        System.out.println("DEBUG: Attempting to set Elo for '" + playerName + "' to " + newElo + "...");
        String sql = "UPDATE " + PLAYERS_TABLE_NAME + " SET elo = ? WHERE name = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setDouble(1, newElo);
            pstmt.setString(2, playerName);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("DEBUG: Successfully updated Elo for '" + playerName + "'.");
            } else {
                System.out.println("DEBUG: Player '" + playerName + "' not found. No Elo update was performed.");
            }
            
        } catch (SQLException e) {
            handleSQLException(e, "Debug function 'debugSetPlayerElo' failed for player '" + playerName + "'.");
        }
    }

    // Helper methods

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