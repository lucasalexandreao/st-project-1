package st.project.view;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpriteSheetTest {

    @Test
    void shouldHandleInvalidPathGracefullyAndTestGetters() {
        // Ação: Passamos um caminho que não existe para forçar o Exception / InputStream nulo
        SpriteSheet sheet = new SpriteSheet("/caminho/falso/nao_existe.png", 4, 3);

        // Asserções: Garante que a classe tratou o erro silenciosamente sem quebrar o jogo
        assertFalse(sheet.isLoaded(), "O SpriteSheet não deveria estar carregado");
        assertNull(sheet.getFrame(0, 0), "Deveria retornar nulo ao pedir frame de sheet quebrado");
        assertNull(sheet.getFrameFlipped(0, 0), "Deveria retornar nulo ao pedir frame invertido de sheet quebrado");

        // Testa os getters padrão
        assertEquals(4, sheet.getCols());
        assertEquals(3, sheet.getRows());
    }

    // [ESTRUTURAL] Cobre o catch de falha extrema de leitura de arquivo
    @Test
    void shouldHitCatchBlockWhenPathIsNull() {
        // Ao mandar null, o getResourceAsStream dispara NullPointerException,
        // caindo no bloco 'catch (Exception ignored)' e cobrindo a linha vermelha.
        SpriteSheet sheet = new SpriteSheet(null, 1, 1);

        assertFalse(sheet.isLoaded(), "O SpriteSheet deve lidar com o erro de forma segura");
    }
}