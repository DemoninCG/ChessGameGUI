/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ChessGui;

/**
 *
 * @author corban
 */
/*
package ChessCui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author Corban Guy, Naz Janif
 */
/*
public class PlayerData {
    public static final int K = 32; // Typical K-factor for casual players
    private static final String FILE_PATH = "resources/player_scores.txt";
    private static final double DEFAULT_ELO = 1000.0; // Starting Elo for new players

    // Map to store player data, keyed by player name (lowercase)
    private Map<String, PlayerStats> playerDataMap;

    // Inner class to hold individual player stats
    private static class PlayerStats {
        String name;
        int wins;
        int draws;
        int losses;
        double elo;

        PlayerStats(String name, int wins, int draws, int losses, double elo) {
            this.name = name;
            this.wins = wins;
            this.draws = draws;
            this.losses = losses;
            this.elo = elo;
        }

        @Override
        public String toString() {
            // Comma-separated format for saving to file
            return String.format("%s,%d,%d,%d,%.1f", name, wins, draws, losses, elo);
        }
    }

    public PlayerData() {
        this.playerDataMap = new HashMap<>();
        loadPlayerData();
    }

    // Loads player data from the file
    // Creates the directory and file if they don't exist
    private void loadPlayerData() {
        Path filePath = Paths.get(FILE_PATH);
        // Ensure directory exists
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            System.err.println("Error creating directory: " + filePath.getParent() + " - " + e.getMessage());
            return; // Failure means we likely can't proceed
        }

        // Check if file exists, create if not
        if (!Files.exists(filePath)) {
            System.out.println("Player data file not found. Creating new file: " + FILE_PATH);
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                System.err.println("Error creating player data file: " + e.getMessage());
                return; // Cannot proceed if file cannot be created
            }
        } else {
            // Only needed during debugging
            // System.out.println("Loading player data from: " + FILE_PATH);
        }

        // Try-with-resources to ensure the reader is closed
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    try {
                        String name = parts[0].trim();
                        int wins = Integer.parseInt(parts[1].trim());
                        int draws = Integer.parseInt(parts[2].trim());
                        int losses = Integer.parseInt(parts[3].trim());
                        double elo = Double.parseDouble(parts[4].trim());

                        // Store using lowercase name as key for case-insensitive lookup
                        playerDataMap.put(name.toLowerCase(), new PlayerStats(name, wins, draws, losses, elo));
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Skipping invalid data line: " + line + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Warning: Skipping malformed data line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading player data file: " + e.getMessage());
        }
        // Only needed during debugging
        // System.out.println("Player data loaded successfully.");
    }

    // Checks if a player exists, and if not, adds them with default stats
    // Also prints the status (found or added)
    public PlayerStats checkOrAddPlayer(String playerName) {
        String lowerCaseName = playerName.toLowerCase(); // Use lowercase for lookup
        if (playerDataMap.containsKey(lowerCaseName)) {
            double elo = playerDataMap.get(lowerCaseName).elo;
            System.out.println("Player '" + playerName + "' found. Welcome back. Your current elo is " + elo + ".");
            return playerDataMap.get(lowerCaseName);
        } else {
            System.out.println("Player '" + playerName + "' not found. Adding new player.");
            PlayerStats newPlayer = new PlayerStats(playerName, 0, 0, 0, DEFAULT_ELO);
            playerDataMap.put(lowerCaseName, newPlayer);
            savePlayerData(); // Save immediately after adding a new player
            return newPlayer;
        }
    }

    // Saves current player data map to the file (overwriting the existing content)
    public void savePlayerData() {
        Path filePath = Paths.get(FILE_PATH);
        // Try-with-resources for the writer
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (PlayerStats stats : playerDataMap.values()) {
                writer.write(stats.toString());
                writer.newLine();
            }
             System.out.println("Player data saved.");
        } catch (IOException e) {
            System.err.println("Error writing player data file: " + e.getMessage());
        }
    }

    // 1 means white won the game, 0 means draw, -1 means white lost the game
    public void updateGameResults(String whitePlayerName, String blackPlayerName, int whiteResult) {
        PlayerStats whitePlayer = playerDataMap.get(whitePlayerName.toLowerCase());
        PlayerStats blackPlayer = playerDataMap.get(blackPlayerName.toLowerCase());

        if (whitePlayer == null || blackPlayer == null) {
             System.err.println("Cannot update results: One or both players not found in memory.");
             return;
        }

        // Elo calculation logic
        double whiteElo = whitePlayer.elo;
        double blackElo = blackPlayer.elo;

        double expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (blackElo - whiteElo) / 400.0));
        double expectedBlack = 1.0 - expectedWhite; // Expected scores sum to 1

        double actualWhiteScore;
        double actualBlackScore;

        if (whiteResult == 1) { // White wins
            whitePlayer.wins++;
            blackPlayer.losses++;
            actualWhiteScore = 1.0;
            actualBlackScore = 0.0;
        } else if (whiteResult == -1) { // Black wins
            whitePlayer.losses++;
            blackPlayer.wins++;
            actualWhiteScore = 0.0;
            actualBlackScore = 1.0;
        } else { // Draw
            whitePlayer.draws++;
            blackPlayer.draws++;
            actualWhiteScore = 0.5;
            actualBlackScore = 0.5;
        }

        // Update elo
        whitePlayer.elo = whiteElo + K * (actualWhiteScore - expectedWhite);
        blackPlayer.elo = blackElo + K * (actualBlackScore - expectedBlack);

        System.out.printf("%s's new elo: %.1f\n", whitePlayer.name, whitePlayer.elo);
        System.out.printf("%s's win/draw/loss record: %d/%d/%d\n", whitePlayer.name, whitePlayer.wins, whitePlayer.draws, whitePlayer.losses);
        System.out.printf("%s's new elo: %.1f\n", blackPlayer.name, blackPlayer.elo);
        System.out.printf("%s's win/draw/loss record: %d/%d/%d\n", blackPlayer.name, blackPlayer.wins, blackPlayer.draws, blackPlayer.losses);

        // Save the updated data
        savePlayerData();
    }
}
*/