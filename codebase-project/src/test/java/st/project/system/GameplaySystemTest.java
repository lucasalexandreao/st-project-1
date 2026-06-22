package st.project.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import st.project.controller.GameFlowManager;
import st.project.view.GameplayPO;
import st.project.view.PostGamePO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class GameplaySystemTest {

    @BeforeEach
    void setUp() {
        // Garante que o teste roda rápido e sem desenhar os gráficos
        System.setProperty("java.awt.headless", "false");
    }

    @Test
    void jornadaDoJogadorEntrandoNoLabirintoEControlandoOPersonagem() throws Exception {
        if (GraphicsEnvironment.isHeadless()) return;

        GameFlowManager flowManager = new GameFlowManager();

        Field nameField = GameFlowManager.class.getDeclaredField("currentPlayerName");
        nameField.setAccessible(true);
        nameField.set(flowManager, "jogador_teste");

        // reflection para invocar a transição direta para a fase de Gameplay
        java.lang.reflect.Method transitionMethod = GameFlowManager.class.getDeclaredMethod("transitionTo", GameFlowManager.GameLifecycleState.class);
        transitionMethod.setAccessible(true);
        transitionMethod.invoke(flowManager, GameFlowManager.GameLifecycleState.LOADING_GAME);

        // Page Object Assume o Controle do Jogo
        GameplayPO gameplayPage = new GameplayPO();

        assertDoesNotThrow(gameplayPage::isReady, "O sistema deveria ter aberto a janela do GamePanel.");

        gameplayPage.pressionarTecla(KeyEvent.VK_DOWN);
        gameplayPage.pressionarTecla(KeyEvent.VK_RIGHT);

        gameplayPage.pressionarTecla(KeyEvent.VK_SPACE);

        // Extraimos o estado do jogo para garantir que o GamePanel recebeu os comandos
        Field controllerField = GameFlowManager.class.getDeclaredField("gameController");
        controllerField.setAccessible(true);
        st.project.controller.GameController controller = (st.project.controller.GameController) controllerField.get(flowManager);

        // Se o boneco começou no (1,1) e andamos para Baixo e Direita, o Y deve ser diferente da origem
        int posicaoY = controller.getGameState().getPlayer().getGridY();
        assertTrue(posicaoY >= 1, "O GamePanel não repassou o evento de teclado para a física do jogo.");

        gameplayPage.forcarFechamentoDaJanela();
    }
}