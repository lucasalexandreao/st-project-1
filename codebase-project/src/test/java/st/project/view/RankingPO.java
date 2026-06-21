package st.project.view;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.awt.*;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class RankingPO extends GamePageObject {
    private final LeaderboardRepository repo;
    private boolean ready = false;

    public RankingPO(LeaderboardRepository repo) {
        this.repo = repo;
    }

    @Override
    public void isReady() {
        if (repo == null) throw new IllegalStateException("O repositório do Ranking não foi injetado.");
        this.ready = true;
    }

    public String extrairTextoDoRankingPorTempo() {
        return extrairTextoDaAba(0); // Aba 1
    }

    public String extrairTextoDoRankingPorSessoes() {
        return extrairTextoDaAba(1); // Aba 2
    }

    private String extrairTextoDaAba(int tabIndex) {
        isReady();

        // Intercepta a janela para não piscar na tela (modo Headless via Mockito)
        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class, (mock, context) -> {
            doNothing().when(mock).setVisible(anyBoolean());
        })) {
            JFrame dummyParent = new JFrame();
            RankingDialog.show(dummyParent, repo);

            // Captura o componente JTabbedPane que foi injetado na tela durante o método show()
            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(0)).add(captor.capture());
            JTabbedPane tabbedPane = (JTabbedPane) captor.getValue();

            // Navega na árvore de componentes (Painel -> ScrollPane -> TextArea) para extrair o texto limpo
            JPanel panel = (JPanel) tabbedPane.getComponentAt(tabIndex);
            JScrollPane scrollPane = (JScrollPane) panel.getComponent(0);
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

            return textArea.getText();
        }
    }
}