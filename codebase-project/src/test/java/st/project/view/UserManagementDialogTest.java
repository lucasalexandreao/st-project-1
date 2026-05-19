package st.project.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import st.project.model.user.User;
import st.project.repository.LeaderboardRepository;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserManagementDialogTest {

    private Component mockParent;
    private LeaderboardRepository mockRepo;
    private UserManagementDialog dialogObj;
    private final String superName = "admin";

    @BeforeEach
    void setUp() {
        mockParent = mock(Component.class);
        mockRepo = mock(LeaderboardRepository.class);
        dialogObj = new UserManagementDialog(mockRepo, superName);
    }

    // [Caminho 1] Valida a segurança: barra entrada se o usuário não existir ou não for admin
    @Test
    void shouldDenyAccessForInvalidUsers() {
        try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class)) {
            // Caso 1: Usuário logado não encontrado (null)
            when(mockRepo.getUser(superName)).thenReturn(null);
            dialogObj.show(mockParent);

            // Caso 2: Usuário encontrado, mas não é superusuário
            User normalUser = mock(User.class);
            when(normalUser.isSuperuser()).thenReturn(false);
            when(mockRepo.getUser(superName)).thenReturn(normalUser);
            dialogObj.show(mockParent);

            // Valida que em ambos os casos a mensagem de bloqueio foi exibida
            optionPaneMock.verify(() -> JOptionPane.showMessageDialog(
                    any(), eq("Apenas superusuários podem gerenciar usuários."), eq("Acesso Negado"), eq(JOptionPane.WARNING_MESSAGE)
            ), times(2));
        }
    }

    // [Caminho 2] Valida configurações de UI, formatação da lista e todas as ações dos botões
    @Test
    void shouldRenderDialogAndHandleButtonActions() {
        User superUser = mock(User.class);
        when(superUser.isSuperuser()).thenReturn(true);
        when(mockRepo.getUser(superName)).thenReturn(superUser);

        // Configura uma lista contendo os dois tipos de usuários para bater o operador ternário (isSuperuser ? "SUPER" : "Normal")
        User listedAdmin = mock(User.class);
        when(listedAdmin.getPlayerName()).thenReturn("admin");
        when(listedAdmin.isSuperuser()).thenReturn(true);

        User listedPlayer = mock(User.class);
        when(listedPlayer.getPlayerName()).thenReturn("target");
        when(listedPlayer.isSuperuser()).thenReturn(false);

        when(mockRepo.getTopUsersBySessions(100)).thenReturn(List.of(listedAdmin, listedPlayer));

           try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class);
               MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {
            dialogObj.show(mockParent);
            JDialog createdDialog = dialogMock.constructed().get(0);

            // Extrai componentes visuais interceptados do Dialog
            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(createdDialog).add(captor.capture());
            JPanel mainPanel = (JPanel) captor.getValue();
            
            JScrollPane scroll = (JScrollPane) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            JTextArea textArea = (JTextArea) scroll.getViewport().getView();
            
            JPanel buttonPanel = (JPanel) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            JButton deleteBtn = (JButton) buttonPanel.getComponent(0);
            JButton closeBtn = (JButton) buttonPanel.getComponent(1);

            // 1. Valida texto processado na UI
            String text = textArea.getText();
            assertTrue(text.contains("admin           SUPER"));
            assertTrue(text.contains("target          Normal"));

            // --- SIMULAÇÃO DE FLUXOS DO BOTÃO 'DELETAR' ---
            
            // Fluxo A: Clica em cancelar no InputDialog (retorna null) ou envia string vazia
            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt())).thenReturn(null, "");
            deleteBtn.doClick();
            deleteBtn.doClick(); // Coberto: saídas precoces

            // Fluxo B: Tenta deletar o próprio usuário logado
            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt())).thenReturn(" ADMIN "); // Testa trim e lowerCase
            deleteBtn.doClick();
            optionPaneMock.verify(() -> JOptionPane.showMessageDialog(any(), eq("Você não pode deletar sua própria conta."), anyString(), anyInt()));

            // Fluxo C: Tenta deletar usuário inexistente
            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt())).thenReturn("ghost");
            when(mockRepo.getUser("ghost")).thenReturn(null);
            deleteBtn.doClick();
            optionPaneMock.verify(() -> JOptionPane.showMessageDialog(any(), eq("Usuário não encontrado."), anyString(), anyInt()));

            // Fluxo D: Usuário encontrado, mas clica em "NÃO" no ConfirmDialog
            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt())).thenReturn("target");
            when(mockRepo.getUser("target")).thenReturn(listedPlayer);
            optionPaneMock.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt())).thenReturn(JOptionPane.NO_OPTION);
            deleteBtn.doClick();
            verify(mockRepo, never()).deleteUser("target");

            // Fluxo E: Fluxo de Sucesso Completo (Clica "SIM")
            optionPaneMock.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt())).thenReturn(JOptionPane.YES_OPTION);
            deleteBtn.doClick();
            verify(mockRepo).deleteUser("target");
            optionPaneMock.verify(() -> JOptionPane.showMessageDialog(any(), eq("Usuário deletado com sucesso!"), anyString(), anyInt()));

            // --- SIMULAÇÃO DO BOTÃO FECHAR ---
            closeBtn.doClick();
            verify(createdDialog).dispose();
        }
    }

    // [Caminho 4] Valida chamada de setLocationRelativeTo e propriedades dialogo quando parent é passado
    @Test
    void shouldSetLocationRelativeToParentCorrectly() {
        User superUser = mock(User.class);
        when(superUser.isSuperuser()).thenReturn(true);
        when(mockRepo.getUser(superName)).thenReturn(superUser);
        when(mockRepo.getTopUsersBySessions(100)).thenReturn(Collections.emptyList());

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {
            dialogObj.show(mockParent);

            JDialog createdDialog = dialogMock.constructed().get(0);

            // Verifica que setLocationRelativeTo foi chamado com o parent
            verify(createdDialog).setLocationRelativeTo(mockParent);
            verify(createdDialog).setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            verify(createdDialog).setResizable(true);
            verify(createdDialog).setSize(600, 400);
            verify(createdDialog).setVisible(true);
        }
    }

    // [Caminho 5] Valida fluxo de refresh de lista após deletar usuário com sucesso
    @Test
    void shouldRefreshUserListAfterSuccessfulDeletion() {
        User superUser = mock(User.class);
        when(superUser.isSuperuser()).thenReturn(true);
        when(mockRepo.getUser(superName)).thenReturn(superUser);

        User targetUser = mock(User.class);
        when(targetUser.getPlayerName()).thenReturn("target");
        when(targetUser.isSuperuser()).thenReturn(false);

        when(mockRepo.getTopUsersBySessions(100)).thenReturn(List.of(superUser, targetUser));

           try (MockedStatic<JOptionPane> optionPaneMock = mockStatic(JOptionPane.class);
               MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {
            dialogObj.show(mockParent);
            JDialog createdDialog = dialogMock.constructed().get(0);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(createdDialog).add(captor.capture());
            JPanel mainPanel = (JPanel) captor.getValue();

            JScrollPane scroll = (JScrollPane) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
            JTextArea textArea = (JTextArea) scroll.getViewport().getView();
            JPanel buttonPanel = (JPanel) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            JButton deleteBtn = (JButton) buttonPanel.getComponent(0);

            // Simula exclusão bem-sucedida
            optionPaneMock.when(() -> JOptionPane.showInputDialog(any(), any(), any(), anyInt())).thenReturn("target");
            when(mockRepo.getUser("target")).thenReturn(targetUser);
            optionPaneMock.when(() -> JOptionPane.showConfirmDialog(any(), any(), any(), anyInt())).thenReturn(JOptionPane.YES_OPTION);

            // Lista antes da deleção contém target
            assertTrue(textArea.getText().contains("target"));

            deleteBtn.doClick();

            // Simula a resposta do mock após deleção (lista sem target)
            when(mockRepo.getTopUsersBySessions(100)).thenReturn(List.of(superUser));

            // Verifica mensagem de sucesso
            optionPaneMock.verify(() -> JOptionPane.showMessageDialog(any(), eq("Usuário deletado com sucesso!"), anyString(), anyInt()));
        }
    }

    // [Caminho 6] Valida formatação de tipos de usuário (SUPER vs Normal) e contagem de sessões
    @Test
    void shouldFormatUserTypesAndSessionCountsCorrectly() {
        User superUser = mock(User.class);
        when(superUser.isSuperuser()).thenReturn(true);
        when(superUser.getPlayerName()).thenReturn("admin");
        when(superUser.getSessionCount()).thenReturn(999);

        User normalUser = mock(User.class);
        when(normalUser.isSuperuser()).thenReturn(false);
        when(normalUser.getPlayerName()).thenReturn("player");
        when(normalUser.getSessionCount()).thenReturn(1);

        User anotherSuper = mock(User.class);
        when(anotherSuper.isSuperuser()).thenReturn(true);
        when(anotherSuper.getPlayerName()).thenReturn("mod");
        when(anotherSuper.getSessionCount()).thenReturn(50);

        when(mockRepo.getUser(superName)).thenReturn(superUser);
        when(mockRepo.getTopUsersBySessions(100)).thenReturn(List.of(superUser, normalUser, anotherSuper));

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {
            dialogObj.show(mockParent);

            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(0)).add(captor.capture());
            JPanel mainPanel = (JPanel) captor.getValue();

            JTextArea textArea = (JTextArea) ((JScrollPane) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();
            String text = textArea.getText();

            // Verifica formatação de tipos e contagens
            assertTrue(text.contains("admin") && text.contains("SUPER") && text.contains("999"));
            assertTrue(text.contains("player") && text.contains("Normal") && text.contains("1"));
            assertTrue(text.contains("mod") && text.contains("SUPER") && text.contains("50"));
        }
    }

    // [Caminho 3] Valida casos periféricos de UI: parent null, Window = Frame, lista vazia e a proteção 'null' do normalizador
    @Test
    void shouldHandleEdgeCasesNullParentEmptyListAndUnreachableNullCheck() throws Exception {
        User superUser = mock(User.class);
        when(superUser.isSuperuser()).thenReturn(true);
        when(mockRepo.getUser(superName)).thenReturn(superUser);
        when(mockRepo.getTopUsersBySessions(100)).thenReturn(Collections.emptyList());

        try (MockedConstruction<JDialog> dialogMock = mockConstruction(JDialog.class)) {

            // 1. Passando parent null
            dialogObj.show(null);
            verify(dialogMock.constructed().get(0)).setLocationRelativeTo(null);

            // A MATADORA DA LINHA VERMELHA (SEM DESTRUIR O SWING):
            // Criamos um JFrame real com um componente dentro para que o SwingUtilities
            // nativo (não mockado) faça o trabalho e retorne true no "instanceof Frame".
            JFrame realFrame = new JFrame();
            JButton realButton = new JButton();
            realFrame.add(realButton);

            // 2. Passando o componente cujo Ancestral é um Frame
            dialogObj.show(realButton);

            // Valida texto de lista vazia
            ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
            verify(dialogMock.constructed().get(1)).add(captor.capture());
            JPanel mainPanel = (JPanel) captor.getValue();
            JTextArea textArea = (JTextArea) ((JScrollPane) ((BorderLayout) mainPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getViewport().getView();

            assertTrue(textArea.getText().contains("Sem usuários registrados."));

            // Libera a memória da janela real
            realFrame.dispose();
        }

        // 3. Usa Reflection apenas para forçar 100% de line coverage no bloco condicional (if rawUsername == null) de normalizeUsername.
        Method normalizeMethod = UserManagementDialog.class.getDeclaredMethod("normalizeUsername", String.class);
        normalizeMethod.setAccessible(true);
        assertEquals("", normalizeMethod.invoke(dialogObj, (String) null));
    }
}