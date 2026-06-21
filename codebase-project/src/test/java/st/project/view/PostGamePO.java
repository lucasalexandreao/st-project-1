package st.project.view;

import org.mockito.MockedStatic;
import st.project.model.user.LeaderboardEntry;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

public class PostGamePO extends GamePageObject {
    private boolean ready = false;

    @Override
    public void isReady() {
        this.ready = true;
    }

    /**
     * Simula a jornada do usuário na tela de Fim de Jogo.
     * @param choiceIndex 0 = Jogar Novamente, 1 = Voltar ao Menu, 2 = Fechar
     */
    public PostGameDialog.PostGameAction interagirComTelaFinal(Component parent, boolean won, List<LeaderboardEntry> leaderboard, long time, int choiceIndex) {
        isReady();

        // CORREÇÃO: Cria uma janela fantasma (dummy) caso o parent seja null,
        // evitando o NullPointerException no SwingUtilities.getWindowAncestor.
        Component safeParent = (parent != null) ? parent : new JFrame();

        // Intercepta a janela do JOptionPane para rodar em modo invisível (Headless)
        // e simula o clique no botão correspondente ao choiceIndex
        try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class)) {

            optionPaneMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(choiceIndex);

            return PostGameDialog.show(safeParent, won, leaderboard, time);
        }
    }
}