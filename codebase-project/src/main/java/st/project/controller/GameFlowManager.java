package st.project.controller;

import st.project.model.game.*;
import st.project.model.user.User;
import st.project.repository.LeaderboardRepository;
import st.project.repository.JdbcLeaderboardRepository;
import st.project.repository.NoOpLeaderboardRepository;
import st.project.view.GamePanelNew;
import st.project.view.PostGameDialog;
import st.project.view.UserManagementDialog;
import javax.swing.*;

public class GameFlowManager {
    public enum GameLifecycleState {
        MENU,
        LOGIN,
        LOADING_GAME,
        PLAYING,
        POST_GAME,
        EXIT
    }

    private final LeaderboardRepository repository;
    private final MenuController menuController;
    private final AuthController authController;
    private GameController gameController;
    private GamePanelNew gamePanel;
    private JFrame gameWindow;
    private String currentPlayerName;

    public GameFlowManager() {
        this.repository = createDefaultRepository();
        this.menuController = new MenuController(this::handlePlayClick, this::handleExitClick);
        this.authController = new AuthController(repository);
    }

    public void start() {
        transitionTo(GameLifecycleState.MENU);
    }

    private void transitionTo(GameLifecycleState newState) {

        switch (newState) {
            case MENU:
                handleMenuState();
                break;
            case LOGIN:
                handleLoginState();
                break;
            case LOADING_GAME:
                handleLoadingGameState();
                break;
            case PLAYING:
                handlePlayingState();
                break;
            case POST_GAME:
                handlePostGameState();
                break;
            default:
                exitApp();
                break;
        }
    }

    protected void exitApp() {
        Runtime.getRuntime().exit(0);
    }

    private void handleMenuState() {
        menuController.showMainMenu();
    }

    private void handlePlayClick() {
        transitionTo(GameLifecycleState.LOGIN);
    }

    private void handleExitClick() {
        transitionTo(GameLifecycleState.EXIT);
    }

    private void handleLoginState() {
        String playerName = authController.promptForLogin();
        String validated = authController.getPlayerNameOrNull(playerName);

        if (validated == null) {
            transitionTo(GameLifecycleState.MENU);
        } else {
            User user = repository.getUser(validated);
            if (user != null && user.isSuperuser()) {
                new UserManagementDialog(repository, validated).show(null);
                transitionTo(GameLifecycleState.LOGIN);
                return;
            }

            currentPlayerName = validated;
            transitionTo(GameLifecycleState.LOADING_GAME);
        }
    }

    private void handleLoadingGameState() {
        int[][] level1Layout = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1},
                {1, 0, 1, 2, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 3},
                {1, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 1, 2, 1, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };

        Room room = new Room(level1Layout);
        Player player = new Player(1, 1, room);
        GameState gameState = new GameState(1, player, room);
        gameController = new GameController(gameState, repository, currentPlayerName);

        transitionTo(GameLifecycleState.PLAYING);
    }

    private void handlePlayingState() {
        createGameWindow();
        gameWindow.setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    private void handlePostGameState() {
        if (gamePanel == null || gameController == null) {
            transitionTo(GameLifecycleState.MENU);
            return;
        }

        gamePanel.stopGameLoop();

        GameState state = gameController.getGameState();
        long elapsed = gameController.getElapsedMillis();

        PostGameDialog.PostGameAction action = PostGameDialog.show(
            gamePanel,
            state.isGameWon(),
            gameController.getLeaderboard(),
            elapsed
        );

        if (gameWindow != null) {
            for (java.awt.event.WindowListener wl : gameWindow.getWindowListeners()) {
                gameWindow.removeWindowListener(wl);
            }
            gameWindow.dispose();
            gameWindow = null; // Boa prática para liberar memória
        }

        switch (action) {
            case PLAY_AGAIN:
                transitionTo(GameLifecycleState.LOADING_GAME);
                break;
            case RETURN_TO_MENU:
                transitionTo(GameLifecycleState.MENU);
                break;
            default:
                transitionTo(GameLifecycleState.EXIT);
                break;
        }
    }

    private void createGameWindow() {
        gameWindow = new JFrame("Labirinto");
        gameWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        gamePanel = new GamePanelNew(gameController, repository, currentPlayerName);
        gamePanel.setOnGameFinished(() -> transitionTo(GameLifecycleState.POST_GAME));
        gameWindow.add(gamePanel);

        gameWindow.pack();
        gameWindow.setSize(620, 480);
        gameWindow.setResizable(false);
        gameWindow.setLocationRelativeTo(null);

        gameWindow.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                transitionTo(GameLifecycleState.MENU);
            }
        });
    }

    private LeaderboardRepository createDefaultRepository() {
        try {
            return new JdbcLeaderboardRepository("leaderboard.db");
        } catch (RuntimeException e) {
            return new NoOpLeaderboardRepository();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFlowManager manager = new GameFlowManager();
            manager.start();
        });
    }
}
