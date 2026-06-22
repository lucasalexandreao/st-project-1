package st.project.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class GameplayPO extends GamePageObject {
    private boolean ready = false;
    private JPanel gamePanel;
    private JFrame gameWindow;

    @Override
    public void isReady() {
        // Tenta encontrar a janela "Labirinto" e o painel de jogo ativo
        for (Frame f : Frame.getFrames()) {
            if ("Labirinto".equals(f.getTitle()) && f.isShowing()) {
                gameWindow = (JFrame) f;
                Component[] comps = gameWindow.getContentPane().getComponents();
                if (comps.length > 0 && comps[0] instanceof JPanel) {
                    gamePanel = (JPanel) comps[0];
                    this.ready = true;
                    return;
                }
            }
        }
        throw new IllegalStateException("O Jogo não está em execução na tela.");
    }

    public void pressionarTecla(int keyCode) {
        isReady();
        // Simula o pressionamento da tecla direcionada ao GamePanel
        KeyEvent keyEvent = new KeyEvent(gamePanel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);

        // Dispara o evento de teclado em todos os ouvintes (KeyListeners) do painel
        for (java.awt.event.KeyListener listener : gamePanel.getKeyListeners()) {
            listener.keyPressed(keyEvent);
        }
    }

    public void forcarFechamentoDaJanela() {
        if (gameWindow != null) {
            gameWindow.dispose();
        }
    }
}