package st.project;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import st.project.repository.JdbcLeaderboardRepository;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.anyString;

class InitializeSuperuserTest {

    // ESTRUTURAL (Uso de Mockito Interceptor)
    @Test
    void shouldCreateAdminUserWithoutModifyingRealDatabase() {
        // Interceptamos a criação do banco de dados para evitar gravar no disco durante o teste
        try (MockedConstruction<JdbcLeaderboardRepository> mocked = mockConstruction(JdbcLeaderboardRepository.class)) {

            // AÇÃO: Executa o script principal
            assertDoesNotThrow(() -> InitializeSuperuser.main(new String[0]));

            // PÓS-CONDIÇÃO
            assertEquals(1, mocked.constructed().size(), "Deveria ter instanciado o repositório");
            JdbcLeaderboardRepository mockRepo = mocked.constructed().get(0);

            // Verifica se o script tentou salvar o superusuário "admin" corretamente
            verify(mockRepo).createUser(eq("admin"), anyString(), eq(true));
        }
    }

    // DOMÍNIO (Teste Criptográfico)
    @Test
    void shouldHashPasswordCorrectlyUsingSHA256() throws Exception {
        // Usamos Reflection para testar o método privado de Hash da senha
        Method hashMethod = InitializeSuperuser.class.getDeclaredMethod("hashPassword", String.class);
        hashMethod.setAccessible(true);

        String hash = (String) hashMethod.invoke(null, "admin");

        // O hash SHA-256 base64 da palavra "admin" é uma constante matemática inquebrável
        assertEquals("jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg=", hash, "O algoritmo de hash foi alterado ou está quebrado!");
    }

    // ESTRUTURAL (Fronteira Extrema / Exceção Forçada via Mock)
    @Test
    void shouldThrowRuntimeExceptionWhenHashingFails() throws Exception {
        // Mockamos a classe de criptografia do Java para simular a falha impossível
        try (MockedStatic<java.security.MessageDigest> mockedDigest = mockStatic(java.security.MessageDigest.class)) {

            // Quando pedirem qualquer algoritmo de Hash, mandamos estourar a exceção
            mockedDigest.when(() -> java.security.MessageDigest.getInstance(anyString()))
                    .thenThrow(new java.security.NoSuchAlgorithmException("Algoritmo inexistente forçado"));

            java.lang.reflect.Method hashMethod = InitializeSuperuser.class.getDeclaredMethod("hashPassword", String.class);
            hashMethod.setAccessible(true);

            // AÇÃO: Tentamos fazer o hash e interceptamos o estouro do erro no Reflection
            java.lang.reflect.InvocationTargetException ex = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> hashMethod.invoke(null, "admin")
            );

            // PÓS-CONDIÇÃO: O erro original deve ser o RuntimeException que você programou
            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Erro ao fazer hash da senha", ex.getCause().getMessage());
        }
    }
}