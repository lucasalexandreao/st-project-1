package st.project.view;

import st.project.controller.MenuController;

import javax.swing.*;
import java.awt.*;

public class MenuPO extends GamePageObject {
    private final MenuController controller;
    private boolean ready = false;

    public MenuPO(MenuController controller) {
        this.controller = controller;
    }

    @Override
    public void isReady() {
        if (controller == null) throw new IllegalStateException("O Controller do Menu não foi injetado.");
        this.ready = true;
    }

    public void clicarJogar() {
        interagirComBotao("Jogar");
    }

    public void clicarSair() {
        interagirComBotao("Sair");
    }

    private void interagirComBotao(String textoBotao) {
        isReady();

        // Dispara a criação da janela pela controladora real
        controller.showMainMenu();

        JButton targetButton = null;
        JFrame targetFrame = null;

        // Vasculha a memória do Java Swing em busca da janela que acabou de ser criada
        for (Frame f : Frame.getFrames()) {
            if ("Labirinto - Menu".equals(f.getTitle())) {
                targetFrame = (JFrame) f;
                JPanel panel = (JPanel) targetFrame.getContentPane().getComponent(0);

                // Procura o botão desejado dentro do painel
                for (Component c : panel.getComponents()) {
                    if (c instanceof JButton && textoBotao.equals(((JButton) c).getText())) {
                        targetButton = (JButton) c;
                        break;
                    }
                }
            }
        }

        if (targetButton != null) {
            targetButton.doClick(); // Simula o clique do usuário
        } else {
            throw new RuntimeException("Botão '" + textoBotao + "' não encontrado no Menu.");
        }

        // Limpa a janela da memória para o próximo teste
        if (targetFrame != null) {
            targetFrame.dispose();
        }
    }
}