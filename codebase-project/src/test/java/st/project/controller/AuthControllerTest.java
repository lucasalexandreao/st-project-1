package st.project.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import st.project.view.LoginDialog;
import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    // ESTRUTURAL (Uso de Mockito Interceptor)
    @Test
    void shouldPromptForLoginAndShowDialog() {
        // Se este teste rodar num servidor sem interface gráfica (ex: GitHub Actions),
        // nós ignoramos o teste para não estourar HeadlessException ao criar o JFrame.
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        AuthController controller = new AuthController(null);

        // O MockedConstruction intercepta a criação de qualquer "new LoginDialog()"
        try (MockedConstruction<LoginDialog> mocked = mockConstruction(LoginDialog.class,
                (mock, context) -> {
                    // Dizemos ao dublê: "Quando chamarem o .show(), devolva 'UsuarioMock' imediatamente, sem abrir telas"
                    when(mock.show(any())).thenReturn("UsuarioMock");
                })) {

            // AÇÃO: Executa o método que antes travava a tela
            String result = controller.promptForLogin();

            // PÓS-CONDIÇÃO
            assertEquals("UsuarioMock", result, "O retorno deve ser o valor forçado pelo Mockito");
            assertEquals(1, mocked.constructed().size(), "A classe LoginDialog deveria ter sido instanciada internamente");
        }
    }

    // ESTRUTURAL (Uso de Mockito Interceptor)
    @Test
    void shouldShowLoginDialogDirectly() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        AuthController controller = new AuthController(null);
        JFrame dummyFrame = new JFrame();

        try (MockedConstruction<LoginDialog> mocked = mockConstruction(LoginDialog.class,
                (mock, context) -> {
                    when(mock.show(eq(dummyFrame))).thenReturn("OutroUser");
                })) {

            String result = controller.showLoginDialog(dummyFrame);

            assertEquals("OutroUser", result);
        }
    }

    // DOMÍNIO E FRONTEIRA
    @Test
    void shouldValidatePlayerNameCorrectly() {
        // Passamos null no repositório pois esse método não usa o banco de dados
        AuthController controller = new AuthController(null);

        // Fronteira: Nulo
        assertNull(controller.getPlayerNameOrNull(null), "Nome nulo deve retornar nulo");

        // Fronteira: Apenas espaços em branco
        assertNull(controller.getPlayerNameOrNull("   "), "Nome com apenas espaços deve retornar nulo");

        // Domínio: Nome válido com espaços sobrando
        assertEquals("Ana", controller.getPlayerNameOrNull("  Ana  "), "Deve remover os espaços das pontas (trim)");
    }
}