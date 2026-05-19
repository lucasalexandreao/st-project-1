package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import st.project.model.user.LeaderboardEntry;
import st.project.model.user.User;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankingDialogTest {

    private Component mockParent;
    private LeaderboardRepository mockRepository;

    @BeforeEach
    void setUp() {
        mockParent = mock(Component.class);
        mockRepository = mock(LeaderboardRepository.class);
    }

    // [Caminho 1] Valida a renderização quando não há dados registrados (Listas vazias)
    @Test
    void shouldRenderEmptyRankingCorrectly() {
        when(mockRepository.getTopScores(10)).thenReturn(Collections.emptyList());
        when(mockRepository.getTopUsersBySessions(10)).thenReturn(Collections.emptyList());

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            RankingDialog.show(mockParent, mockRepository);

            JDialog dialog = dialogMock.constructed().get(0);
            verify(dialog).setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            verify(dialog).setResizable(true);
            verify(dialog).setSize(600, 400);
            verify(dialog).setVisible(true);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialog).add(captor.capture());
            JTabbedPane tabbedPane = (JTabbedPane) captor.getValue();

            assertEquals(2, tabbedPane.getTabCount());
            assertEquals("Por Tempo", tabbedPane.getTitleAt(0));
            assertEquals("Por Sessões", tabbedPane.getTitleAt(1));

            JTextArea timeArea = getTextAreaFromTab(tabbedPane, 0);
            assertTrue(timeArea.getText().contains("Sem usuários registrados."));

            JTextArea sessionArea = getTextAreaFromTab(tabbedPane, 1);
            assertTrue(sessionArea.getText().contains("Sem usuários registrados."));
        }
    }

    // [Caminho 2] Valida a renderização com dados preenchidos, formatações e conversões de tempo
    @Test
    void shouldRenderPopulatedRankingCorrectly() {
        LeaderboardEntry mockEntry = mock(LeaderboardEntry.class);
        when(mockEntry.getPlayerName()).thenReturn("Speedrunner");
        when(mockEntry.getCompletionMillis()).thenReturn(2500L); 
        when(mockRepository.getTopScores(10)).thenReturn(List.of(mockEntry));

        User superUser = mock(User.class);
        when(superUser.getPlayerName()).thenReturn("Admin");
        when(superUser.isSuperuser()).thenReturn(true);
        when(superUser.getSessionCount()).thenReturn(50);

        User normalUser = mock(User.class);
        when(normalUser.getPlayerName()).thenReturn("Player");
        when(normalUser.isSuperuser()).thenReturn(false);
        when(normalUser.getSessionCount()).thenReturn(5);

        when(mockRepository.getTopUsersBySessions(10)).thenReturn(List.of(superUser, normalUser));

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            RankingDialog.show(mockParent, mockRepository);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(0)).add(captor.capture());
            JTabbedPane tabbedPane = (JTabbedPane) captor.getValue();

            JTextArea timeArea = getTextAreaFromTab(tabbedPane, 0);
            String timeText = timeArea.getText();
            assertTrue(timeText.contains("RANKING POR TEMPO"));
            assertTrue(timeText.contains("1. Speedrunner"));
            assertTrue(timeText.contains("2") && timeText.contains("50s"));

            JTextArea sessionArea = getTextAreaFromTab(tabbedPane, 1);
            String sessionText = sessionArea.getText();
            assertTrue(sessionText.contains("RANKING POR SESSÕES"));
            assertTrue(sessionText.contains("1. Admin           👑 Sessões: 50"));
            assertTrue(sessionText.contains("2. Player             Sessões: 5")); 
        }
    }

    // [Caminho 3] Valida a chamada de setLocationRelativeTo e configurações de resizable/visible
    @Test
    void shouldConfigureDialogPropertiesAndLocationCorrectly() {
        when(mockRepository.getTopScores(10)).thenReturn(Collections.emptyList());
        when(mockRepository.getTopUsersBySessions(10)).thenReturn(Collections.emptyList());

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            RankingDialog.show(mockParent, mockRepository);

            JDialog dialog = dialogMock.constructed().get(0);

            verify(dialog).setLocationRelativeTo(mockParent);

            verify(dialog).setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            verify(dialog).setResizable(true);
            verify(dialog).setSize(600, 400);
            verify(dialog).setVisible(true);
        }
    }

    // [Caminho 4] Valida formatação de tempo com diferentes valores 
    @Test
    void shouldFormatTimesCorrectlyWithVariousValues() {
        LeaderboardEntry zeroMillis = mock(LeaderboardEntry.class);
        when(zeroMillis.getPlayerName()).thenReturn("Instant");
        when(zeroMillis.getCompletionMillis()).thenReturn(0L);

        LeaderboardEntry largeMillis = mock(LeaderboardEntry.class);
        when(largeMillis.getPlayerName()).thenReturn("Slow");
        when(largeMillis.getCompletionMillis()).thenReturn(350500L);

        when(mockRepository.getTopScores(10)).thenReturn(List.of(zeroMillis, largeMillis));
        when(mockRepository.getTopUsersBySessions(10)).thenReturn(Collections.emptyList());

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            RankingDialog.show(mockParent, mockRepository);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(0)).add(captor.capture());
            JTabbedPane tabbedPane = (JTabbedPane) captor.getValue();

            JTextArea timeArea = getTextAreaFromTab(tabbedPane, 0);
            String timeText = timeArea.getText();

            assertTrue(timeText.contains("1. Instant") && (timeText.contains("0.00s") || timeText.contains("0,00s") || timeText.contains("0.0s") || timeText.contains("0,0s") || timeText.contains("0s")));
            assertTrue(timeText.contains("2. Slow") && (timeText.contains("350.50s") || timeText.contains("350,50s") || timeText.contains("350.5s") || timeText.contains("350,5s") || timeText.contains("350s")));
        }
    }

    // [Caminho 5] Valida renderização com muitos usuários na aba de sessões
    @Test
    void shouldRenderManyUsersInSessionsRanking() {
        when(mockRepository.getTopScores(10)).thenReturn(Collections.emptyList());

        List<User> manyUsers = List.of(
                createMockUser("User1", 100, false),
                createMockUser("User2", 90, false),
                createMockUser("User3", 80, true),
                createMockUser("User4", 70, false),
                createMockUser("User5", 60, true)
        );
        when(mockRepository.getTopUsersBySessions(10)).thenReturn(manyUsers);

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            RankingDialog.show(mockParent, mockRepository);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(0)).add(captor.capture());
            JTabbedPane tabbedPane = (JTabbedPane) captor.getValue();

            JTextArea sessionArea = getTextAreaFromTab(tabbedPane, 1);
            String sessionText = sessionArea.getText();

            assertTrue(sessionText.contains("1. User1") && sessionText.contains("100"));
            assertTrue(sessionText.contains("5. User5") && sessionText.contains("👑") && sessionText.contains("60"));
        }
    }

    private User createMockUser(String name, int sessions, boolean superuser) {
        User user = mock(User.class);
        when(user.getPlayerName()).thenReturn(name);
        when(user.getSessionCount()).thenReturn(sessions);
        when(user.isSuperuser()).thenReturn(superuser);
        return user;
    }

    private JTextArea getTextAreaFromTab(JTabbedPane tabbedPane, int index) {
        JPanel panel = (JPanel) tabbedPane.getComponentAt(index);
        JScrollPane scrollPane = (JScrollPane) panel.getComponent(0); 
        JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

        
        assertFalse(textArea.isEditable());
        assertEquals(Font.MONOSPACED, textArea.getFont().getFamily());

        return textArea;
    }
}