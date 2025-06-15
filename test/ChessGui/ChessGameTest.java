package ChessGui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 *
 * @author Corban Guy, Naz Janif
 */
public class ChessGameTest {

    private ChessGame game;
    private Board board;
    private ChessGUI gui;
    

    // Initializes a ChessGame instance by creating a ChessGUI object
    @Before
    public void setUp() throws Exception {
        gui = new ChessGUI();
        game = (ChessGame) getPrivateField(gui, "game");
        board = game.getBoard();
    }

    @After
    public void tearDown() {
        // Close the GUI window to free resources
        if (gui != null) {
            gui.dispose();
        }
    }

    private void setupTestGame(String white, String black) throws Exception {
        setPrivateField(game, "whitePlayerName", white);
        setPrivateField(game, "blackPlayerName", black);
        setPrivateField(game, "isGameOver", false);
        board.initializeStandardBoard();
        setPrivateField(game, "currentPlayer", "white");
        gui.setGameInProgress(true);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
    
    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private Object callPrivateMethod(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    // Game flow and state tests
    @Test
    public void testNewGameIsInitiallyOver() {
        assertTrue("A new game instance should be 'over' until started.", game.isGameOver());
    }

    @Test
    public void testStartGameSetsUpBoardAndPlayersCorrectly() throws Exception {
        setupTestGame("Alice", "Bob");
        assertFalse("Game should be in progress after starting.", game.isGameOver());
        assertEquals("White should be the first player.", "white", game.getCurrentPlayer());
        assertTrue("White King should be at e1.", board.getPiece(7, 4) instanceof King);
        assertTrue("Black King should be at e8.", board.getPiece(0, 4) instanceof King);
    }

    @Test
    public void testSwitchPlayerTogglesBetweenWhiteAndBlack() throws Exception {
        setupTestGame("Alice", "Bob");
        assertEquals("white", game.getCurrentPlayer());
        callPrivateMethod(game, "switchPlayer");
        assertEquals("black", game.getCurrentPlayer());
        callPrivateMethod(game, "switchPlayer");
        assertEquals("white", game.getCurrentPlayer());
    }

    @Test
    public void testResignEndsGameAndDeclaresOpponentWinner() throws Exception {
        setupTestGame("Alice", "Bob"); // White to move
        game.resign();
        assertTrue("Game should be over after a player resigns.", game.isGameOver());
        assertEquals("Black should win if White resigns.", -1, getPrivateField(game, "gameResult"));
    }

    // Move validation and game end tests
    @Test
    public void testAttemptMove_ValidPawnMove_IsSuccessful() throws Exception {
        setupTestGame("Alice", "Bob");
        game.attemptMove(6, 4, 4, 4); // White e2-e4
        assertNotNull("Pawn should be at the new location.", board.getPiece(4, 4));
        assertNull("Original square should be empty.", board.getPiece(6, 4));
        assertEquals("Player should switch after a valid move.", "black", game.getCurrentPlayer());
    }

    @Test
    public void testAttemptMove_InvalidMove_WhenNotYourTurn() throws Exception {
        setupTestGame("Alice", "Bob");
        game.attemptMove(1, 4, 3, 4); // Attempt to move black's pawn on white's turn
        assertNull("Black's pawn should not have moved.", board.getPiece(3, 4));
        assertEquals("Player should not switch after an invalid move.", "white", game.getCurrentPlayer());
    }

    @Test
    public void testCheckmateIsDetectedAndEndsGame() throws Exception {
        setupTestGame("Alice", "Bob");
        game.attemptMove(6, 5, 5, 5); // f2-f3
        game.attemptMove(1, 4, 3, 4); // e7-e5
        game.attemptMove(6, 6, 4, 6); // g2-g4
        game.attemptMove(0, 3, 4, 7); // Qd8-h4#
        assertTrue("Game should be over on checkmate.", game.isGameOver());
        assertEquals("Black should be the winner.", -1, getPrivateField(game, "gameResult"));
    }

    @Test
    public void testStalemateIsDetected() throws Exception {
        setupTestGame("Alice", "Bob");
        board.initializeEmptyBoard();
        board.placePiece(new King("black"), 0, 7);
        board.placePiece(new King("white"), 2, 5);
        board.placePiece(new Queen("white"), 1, 6);
        setPrivateField(game, "currentPlayer", "black");
        callPrivateMethod(game, "checkGameEndConditions");
        assertTrue("Game should be over on stalemate.", game.isGameOver());
    }

    // Special move tests
    @Test
    public void testCastlingKingsideIsSuccessful() throws Exception {
        setupTestGame("Alice", "Bob");
        board.removePiece(7, 5);
        board.removePiece(7, 6);
        game.attemptMove(7, 4, 7, 6);
        assertTrue("King should be at g1 after castling.", board.getPiece(7, 6) instanceof King);
        assertTrue("Rook should be at f1 after castling.", board.getPiece(7, 5) instanceof Rook);
    }

    @Test
    public void testEnPassantCaptureIsSuccessful() throws Exception {
        setupTestGame("Alice", "Bob");
        game.attemptMove(6, 4, 4, 4); // White: e2-e4
        game.attemptMove(1, 0, 3, 0); // Black: a7-a5
        game.attemptMove(4, 4, 3, 4); // White: e4-e5
        game.attemptMove(1, 3, 3, 3); // Black: d7-d5
        game.attemptMove(3, 4, 2, 3); // White captures en passant
        assertTrue("White pawn should be at d6.", board.getPiece(2, 3) instanceof Pawn);
        assertNull("Captured black pawn at d5 should be removed.", board.getPiece(3, 3));
    }    
}