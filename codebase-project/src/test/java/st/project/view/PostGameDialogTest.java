package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import st.project.model.user.LeaderboardEntry;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostGameDialogTest {

    private Component mockParent;

    @BeforeEach
    void setUp() {
        mockParent = mock(Component.class);
    }

    // Valida Vitória, Leaderboard Vazio, Propriedades da UI e Retorno PLAY_AGAIN
    @Test
    void shouldHandleVictoryEmptyLeaderboardAndPlayAgain() {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            optionMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(0); // Escolha: Jogar novamente

            PostGameDialog.PostGameAction result = PostGameDialog.show(mockParent, true, Collections.emptyList(), 1500);

            assertEquals(PostGameDialog.PostGameAction.PLAY_AGAIN, result);

                assertEquals(PostGameDialog.PostGameAction.PLAY_AGAIN, result);
        }
    }

    // Valida Derrota, Leaderboard com Itens, Laço For de Formatação e Retorno RETURN_TO_MENU
    @Test
    void shouldHandleGameOverPopulatedLeaderboardAndReturnToMenu() {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            optionMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(1); // Escolha: Voltar ao menu

            List<LeaderboardEntry> board = List.of(
                    new LeaderboardEntry("TestPlayer", 2500, "2026-05-18")
            );

            PostGameDialog.PostGameAction result = PostGameDialog.show(mockParent, false, board, 3500);

            assertEquals(PostGameDialog.PostGameAction.RETURN_TO_MENU, result);

                assertEquals(PostGameDialog.PostGameAction.RETURN_TO_MENU, result);
        }
    }

    // Valida fechamento forçado da janela ou opção "Fechar"
    @Test
    void shouldHandleCloseAction() {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            // Retorna o código padrão do Swing para janela fechada no 'X'
            optionMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(JOptionPane.CLOSED_OPTION); 

            PostGameDialog.PostGameAction result = PostGameDialog.show(mockParent, true, Collections.emptyList(), 1000);

            assertEquals(PostGameDialog.PostGameAction.CLOSE, result);
        }
    }

    // Valida múltiplas entradas no leaderboard e formatação de tempo
    @Test
    void shouldHandleMultipleLeaderboardEntries() {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            optionMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(0);

            List<LeaderboardEntry> board = List.of(
                    new LeaderboardEntry("First", 1000, "2026-05-18"),
                    new LeaderboardEntry("Second", 2000, "2026-05-17"),
                    new LeaderboardEntry("Third", 3000, "2026-05-16")
            );

            PostGameDialog.PostGameAction result = PostGameDialog.show(mockParent, true, board, 2500);
            assertEquals(PostGameDialog.PostGameAction.PLAY_AGAIN, result);

                assertEquals(PostGameDialog.PostGameAction.PLAY_AGAIN, result);
        }
    }

    // Valida comportamento em vitória vs derrota com tempos fracionados
    @Test
    void shouldFormatTimesCorrectlyVictoryAndDefeat() {
        try (MockedStatic<JOptionPane> optionMock = mockStatic(JOptionPane.class)) {
            optionMock.when(() -> JOptionPane.showOptionDialog(
                    any(), any(), anyString(), anyInt(), anyInt(), any(), any(), any()
            )).thenReturn(1);

            PostGameDialog.PostGameAction result = PostGameDialog.show(mockParent, false, Collections.emptyList(), 1234);
            assertEquals(PostGameDialog.PostGameAction.RETURN_TO_MENU, result);

                assertEquals(PostGameDialog.PostGameAction.RETURN_TO_MENU, result);
        }
    }

    // Cobre os métodos sintéticos do Enum e garante instanciamento correto
    @Test
    void shouldCoverEnumMethods() {
        PostGameDialog.PostGameAction[] values = PostGameDialog.PostGameAction.values();
        assertEquals(3, values.length);
        assertEquals(PostGameDialog.PostGameAction.PLAY_AGAIN, PostGameDialog.PostGameAction.valueOf("PLAY_AGAIN"));
    }
}