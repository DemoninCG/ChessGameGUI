package ChessGui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class PlayerData {
    public static final int K = 32; // Typical K-factor for casual players
    private static final String DEFAULT_FILE_PATH = "resources/player_scores.txt";
    private static final String BACKUP_FILE_SUFFIX = ".bak";
    private static final double DEFAULT_ELO = 1000.0; // Starting Elo for new players
    private final ChessGame game; // New field to reference the game logic

    private final Path dataFilePath; // Using Path for modern file operations
    private Map<String, PlayerStats> playerDataMap; // Map to store player data, keyed by player name (lowercase)


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

        // Creates a PlayerStats object by parsing a line from the data file
        static PlayerStats fromString(String line) throws IllegalArgumentException, NumberFormatException {
            if (line == null || line.trim().isEmpty()) {
                throw new IllegalArgumentException("Input line cannot be null or empty.");
            }
            String[] parts = line.split(",");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Line does not contain 5 parts: " + line);
            }
            // Trim parts to avoid issues with whitespace
            String name = parts[0].trim();
            int wins = Integer.parseInt(parts[1].trim());
            int draws = Integer.parseInt(parts[2].trim());
            int losses = Integer.parseInt(parts[3].trim());
            double elo = Double.parseDouble(parts[4].trim());
            return new PlayerStats(name, wins, draws, losses, elo);
        }
    }

    // Constructor using default file path
    public PlayerData(ChessGame game) {
        this(DEFAULT_FILE_PATH, game);
    }

    // Constructor for custom file path
    public PlayerData(String filePath, ChessGame game) {
        this.dataFilePath = Paths.get(filePath);
        this.playerDataMap = new HashMap<>();
        this.game = game; // Store the reference
        initializeAndLoadData();
    }

    // Ensures file system readiness and loads player data
    private void initializeAndLoadData() {
        ensureDirectoryExists(this.dataFilePath.getParent());
        ensureDataFileExists(this.dataFilePath);
        loadPlayerDataFromFile();
    }

    private void loadPlayerDataFromFile() {
        if (!checkFileExists(dataFilePath) || !checkFileReadable(dataFilePath)) {
            System.err.println("Error: Cannot load data. File missing or not readable: " + dataFilePath);
            return; // Cannot proceed without a readable file
        }

        List<String> lines = readLinesFromFile(dataFilePath);
        if (lines == null) {
            System.err.println("Error: Failed to read lines from " + dataFilePath);
            // playerDataMap remains empty or retains previous state if initialization failed mid-way
            return;
        }

        Map<String, PlayerStats> loadedData = parsePlayerData(lines); // Processes read data
        this.playerDataMap = loadedData;
        // Only needed during debugging
        // System.out.println("Player data loaded successfully from: " + dataFilePath + " (" + loadedData.size() + " records)");
    }

    // Checks if the specified file exists
    private boolean checkFileExists(Path filePath) {
        boolean exists = Files.exists(filePath);
        if (!exists) {
            System.err.println("Info: File does not exist: " + filePath);
            // This might be expected if the file is being created for the first time
        }
        return exists;
    }

    // Checks if the specified file is readable
    private boolean checkFileReadable(Path filePath) {
        boolean readable = Files.isReadable(filePath); // Checks file system permissions
        if (!readable) {
            // Checks existence first
            if (Files.exists(filePath)) {
                System.err.println("Error: File exists but is not readable: " + filePath);
            } else {
                // Already handled by checkFileExists, but defensive check
                System.err.println("Error: Cannot check readability, file does not exist: " + filePath);
            }
        }
        return readable;
    }

    // Reads and returns all lines from the specified file as a list
    private List<String> readLinesFromFile(Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    // Parses a list of strings (presumably from the data file) into PlayerStats objects
    private Map<String, PlayerStats> parsePlayerData(List<String> lines) {
        Map<String, PlayerStats> dataMap = new HashMap<>();
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (line.trim().isEmpty()) {
                continue; // Skip empty lines
            }
            try {
                PlayerStats stats = PlayerStats.fromString(line);
                dataMap.put(stats.name.toLowerCase(), stats); // Uses lowercase name as key
            } catch (IllegalArgumentException e) {
                System.err.println("Warning: Skipping invalid data line #" + lineNumber + ": \"" + line + "\" - " + e.getMessage());
            }
        }
        return dataMap;
    }

    // File writing-related methods

    // Ensures the directory for the given path exists (creates it if necessary)
    private void ensureDirectoryExists(Path dirPath) {
        if (dirPath != null) { // Ensure parent path
            if (!Files.exists(dirPath)) {
                System.out.println("Directory not found. Creating: " + dirPath);
                try {
                    Files.createDirectories(dirPath);
                } catch (IOException e) {
                    System.err.println("CRITICAL ERROR: Could not create directory: " + dirPath + " - " + e.getMessage());
                }
            } else if (!Files.isDirectory(dirPath)) {
                System.err.println("CRITICAL ERROR: Path exists but is not a directory: " + dirPath);
            }
        }
    }

    // Ensures the data file exists at the specified path (creates an empty file if necessary)
    private void ensureDataFileExists(Path filePath) {
        if (!Files.exists(filePath)) {
            System.out.println("Data file not found. Creating empty file: " + filePath);
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                System.err.println("CRITICAL ERROR: Could not create data file: " + filePath + " - " + e.getMessage());
            }
        }
    }

    // Saves the current state of the playerDataMap to the data file (after attempting to create a backup)
    public void savePlayerData() {
        // Attempts to backup the existing file first
        boolean backupSuccess = backupCurrentDataFile(this.dataFilePath);
        if (!backupSuccess) {
            System.err.println("Warning: Failed to create backup of " + dataFilePath + ". Saving proceed cautiously.");
        }

        // Prepares data for writing
        List<String> linesToWrite = this.playerDataMap.values().stream()
            .map(PlayerStats::toString) // Uses the toString method for formatting
            .collect(Collectors.toList());

        // Writes the data to the main file
        boolean writeSuccess = writeLinesToFile(this.dataFilePath, linesToWrite);
        if (writeSuccess) {
            // Only needed during debugging
            // System.out.println("Player data successfully saved to: " + this.dataFilePath);
        } else {
            System.err.println("ERROR: Failed to save player data to: " + this.dataFilePath);
        }
    }

    // Creates a backup copy of the source file
    // The backup has the same name with a ".bak" suffix appended (replaces existing backup if present)
    private boolean backupCurrentDataFile(Path sourcePath) {
        if (!Files.exists(sourcePath)) {
            return true; // Nothing to back up
        }

        Path backupPath = Paths.get(sourcePath.toString() + BACKUP_FILE_SUFFIX);
        try {
            Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            System.err.println("Error creating backup file: " + backupPath + " from " + sourcePath + " - " + e.getMessage());
            return false;
        }
    }

    // Writes a list of strings to the specified file, overwriting any existing content
    private boolean writeLinesToFile(Path filePath, List<String> lines) {
         // Use try-with-resources to ensure the writer is closed automatically
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, // Create if doesn't exist
                StandardOpenOption.TRUNCATE_EXISTING, // Overwrite content
                StandardOpenOption.WRITE)) { // Specify write intention
            for (int i = 0; i < lines.size(); i++) {
                writer.write(lines.get(i));
                if (i < lines.size() - 1) { // Add newline for all but the last line
                    writer.newLine();
                }
            }
            writer.flush(); // Ensure data is written to the file system
            return true;
        } catch (IOException e) {
            System.err.println("Error writing lines to file: " + filePath + " - " + e.getMessage());
            return false;
        }
    }

    // Checks if a player exists, and if not, adds them with default stats
    public PlayerStats checkOrAddPlayer(String playerName) {
        String lowerCaseName = playerName.toLowerCase();
        if (playerDataMap.containsKey(lowerCaseName)) {
            double elo = playerDataMap.get(lowerCaseName).elo;
            game.log("Player '" + playerName + "' found. Elo: " + String.format("%.1f", elo));
            return playerDataMap.get(lowerCaseName);
        } else {
            game.log("New player '" + playerName + "' added.");
            PlayerStats newPlayer = new PlayerStats(playerName, 0, 0, 0, DEFAULT_ELO);
            playerDataMap.put(lowerCaseName, newPlayer);
            savePlayerData();
            return newPlayer;
        }
    }

    // 1 means white won the game, 0 means draw, -1 means white lost the game
    public void updateGameResults(String whitePlayerName, String blackPlayerName, int whiteResult) {
        // ... (logic is the same)
        PlayerStats whitePlayer = playerDataMap.get(whitePlayerName.toLowerCase());
        PlayerStats blackPlayer = playerDataMap.get(blackPlayerName.toLowerCase());

        if (whitePlayer == null || blackPlayer == null) {
            System.err.println("Cannot update results: One or both players not found in memory.");
            return;
        }

        double oldWhiteElo = whitePlayer.elo;
        double oldBlackElo = blackPlayer.elo;
        
        // Elo calculation logic...
        double expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (oldBlackElo - oldWhiteElo) / 400.0));
        double expectedBlack = 1.0 - expectedWhite;
        double actualWhiteScore, actualBlackScore;

        if (whiteResult == 1) { /* White wins */ whitePlayer.wins++; blackPlayer.losses++; actualWhiteScore = 1.0; actualBlackScore = 0.0; }
        else if (whiteResult == -1) { /* Black wins */ whitePlayer.losses++; blackPlayer.wins++; actualWhiteScore = 0.0; actualBlackScore = 1.0; }
        else { /* Draw */ whitePlayer.draws++; blackPlayer.draws++; actualWhiteScore = 0.5; actualBlackScore = 0.5; }
        
        whitePlayer.elo = oldWhiteElo + K * (actualWhiteScore - expectedWhite);
        blackPlayer.elo = oldBlackElo + K * (actualBlackScore - expectedBlack);

        game.log("\n--- Elo & Record Updates ---");
        game.log(String.format("%s: %.1f -> %.1f Elo", whitePlayer.name, oldWhiteElo, whitePlayer.elo));
        game.log(String.format("Record: %d W / %d D / %d L", whitePlayer.wins, whitePlayer.draws, whitePlayer.losses));
        game.log(String.format("%s: %.1f -> %.1f Elo", blackPlayer.name, oldBlackElo, blackPlayer.elo));
        game.log(String.format("Record: %d W / %d D / %d L", blackPlayer.wins, blackPlayer.draws, blackPlayer.losses));
        
        savePlayerData();
    }
}