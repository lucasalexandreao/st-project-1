package st.project.controller;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MenuControllerTest {

    // ESTRUTURAL E INTEGRAÇÃO DE UI
    @Test
    void shouldTriggerCallbacksWhenButtonsAreClicked() {
        // Ignora em servidores CI/CD que não possuem monitor (Headless)
        if (GraphicsEnvironment.isHeadless()) return;

        // Variáveis atômicas para atuar como espiãs dos lambdas
        AtomicBoolean playClicked = new AtomicBoolean(false);
        AtomicBoolean exitClicked = new AtomicBoolean(false);

        MenuController menu = new MenuController(
                () -> playClicked.set(true),
                () -> exitClicked.set(true)
        );

        menu.showMainMenu();

        // 1. Vasculhamos a memória do Java atrás da janela que o MenuController abriu
        Frame[] frames = Frame.getFrames();
        JFrame menuFrame = null;
        for (Frame f : frames) {
            if (f.getTitle().equals("Labirinto - Menu") && f.isVisible()) {
                menuFrame = (JFrame) f;
                break;
            }
        }

        assertNotNull(menuFrame, "A janela do menu não foi aberta!");

        // 2. Navegamos pelos componentes internos para achar os botões
        JPanel panel = (JPanel) menuFrame.getContentPane().getComponent(0);
        JButton playBtn = (JButton) panel.getComponent(2); // Índice 2 é o botão "Jogar"
        JButton exitBtn = (JButton) panel.getComponent(4); // Índice 4 é o botão "Sair"

        assertEquals("Jogar", playBtn.getText());
        assertEquals("Sair", exitBtn.getText());

        // 3. AÇÃO: Simulamos o clique do usuário
        playBtn.doClick();
        assertTrue(playClicked.get(), "O callback de Play não foi chamado!");

        exitBtn.doClick();
        assertTrue(exitClicked.get(), "O callback de Exit não foi chamado!");

        // Limpeza
        menuFrame.dispose();
    }
}