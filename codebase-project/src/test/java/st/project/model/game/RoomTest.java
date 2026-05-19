package st.project.model.game;

import org.junit.jupiter.api.Test;
import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;


class RoomTest {

    // PBT (Teste Baseado em Propriedades) / ESTRUTURAL
    @Property
    void propertyBased_RoomDimensionsAlwaysMatchInputMatrix(
            @ForAll @IntRange(min = 3, max = 22) int width,
            @ForAll @IntRange(min = 2, max = 21) int height
    ) {

        // PRÉ-CONDIÇÕES (Larguras e alturas aleatórias geradas e controladas pelo JQWik)
        int[][] layout = new int[height][width];

        // Forçamos as chaves mínimas e a porta para não dar erro no construtor
        layout[0][0] = Room.TILE_KEY;
        layout[0][1] = Room.TILE_KEY;
        layout[0][2] = Room.TILE_KEY;
        layout[1][0] = Room.TILE_EXIT_LOCKED;

        // AÇÃO
        Room room = new Room(layout);

        // PÓS-CONDIÇÃO (A Propriedade)
        // A classe tem que conseguir processar qualquer mapa sem corromper o cálculo da matriz interna
        assertEquals(width, room.getWidth(), "Largura calculada de forma incorreta!");
        assertEquals(height, room.getHeight(), "Altura calculada de forma incorreta!");
    }

    // FRONTEIRA
    @Test
    void roomEmpty() {
        int[][] layout = {};

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Room(layout));

        assertEquals("A room nao pode ter 0 blocos.", exception.getMessage());
    }

    // DOMÍNIO
    @Test
    void getTheRightWidthFromRoom(){
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_EXIT_OPEN, Room.TILE_KEY},
                {Room.TILE_KEY, Room.TILE_EXIT_LOCKED, Room.TILE_KEY},
        };

        Room room = new Room(layout);

        assertEquals(room.getWidth(),3);
    }

    // DOMÍNIO
    @Test
    void getTheRightHeightFromRoom(){
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_EXIT_OPEN, Room.TILE_KEY},
                {Room.TILE_KEY, Room.TILE_EXIT_LOCKED, Room.TILE_KEY},
        };

        Room room = new Room(layout);

        assertEquals(room.getHeight(),2);
    }

    // DOMÍNIO E FRONTEIRA
    @Test
    void shouldThrowWhenRoomHasLessThanFiveBlocks() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_WALL},
                {Room.TILE_FLOOR, Room.TILE_KEY}
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Room(layout));

        assertEquals("A room precisa ter ao menos 5 blocos disponíveis", exception.getMessage());
    }

    // DOMÍNIO E FRONTEIRA
    @Test
    void shouldAllowRoomWithExactlyFiveBlocks() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_KEY, Room.TILE_KEY, Room.TILE_EXIT_LOCKED}
        };

        assertDoesNotThrow(() -> new Room(layout));
    }

    // DOMÍNIO E FRONTEIRA
    @Test
    void shouldThrowWhenRoomHasLessThanThreeKeys() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_WALL},
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_EXIT_LOCKED}
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Room(layout));

        assertEquals("A room precisa ter ao menos 3 chaves.", exception.getMessage());
    }

    // DOMÍNIO E FRONTEIRA
    @Test
    void shouldThrowWhenRoomDoesNotHaveExactlyOneLockedExit() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_KEY},
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_FLOOR}
        };

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Room(layout));

        assertEquals("A room precisa ter exatamente 1 porta de saida trancada.", exception.getMessage());
    }

    // DOMÍNIO
    @Test
    void shouldTakeKeyAndReplaceTileWithFloor() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_KEY},
                {Room.TILE_KEY, Room.TILE_EXIT_LOCKED, Room.TILE_FLOOR}
        };

        Room room = new Room(layout);

        int item = room.takeItemAt(1, 0);

        assertEquals(Room.TILE_KEY, item);
        assertEquals(Room.TILE_FLOOR, room.getMapLayout()[0][1]);
    }

    // DOMÍNIO
    @Test
    void shouldReturnNoItemAtPosition() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_WALL, Room.TILE_KEY},
                {Room.TILE_EXIT_LOCKED, Room.TILE_KEY, Room.TILE_FLOOR},
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_FLOOR}
        };

        Room room = new Room(layout);

        int item = room.takeItemAt(0, 0);

        assertEquals(-1, item);
    }

    // DOMÍNIO
    @Test
    void shouldOpenLockedExit() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_EXIT_LOCKED, Room.TILE_KEY},
                {Room.TILE_WALL, Room.TILE_FLOOR, Room.TILE_KEY},
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_FLOOR}
        };

        Room room = new Room(layout);

        room.openExit(1, 0);

        assertEquals(Room.TILE_EXIT_OPEN, room.getMapLayout()[0][1]);
    }

    // DOMÍNIO E ESTRUTURAL
    @Test
    void shouldNotChangeTileWhenExitIsNotLocked() {
        int[][] layout = {
                {Room.TILE_FLOOR, Room.TILE_EXIT_OPEN, Room.TILE_KEY},
                {Room.TILE_WALL, Room.TILE_EXIT_LOCKED, Room.TILE_KEY},
                {Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_FLOOR}
        };

        Room room = new Room(layout);

        room.openExit(1, 0);

        assertArrayEquals(new int[]{Room.TILE_FLOOR, Room.TILE_EXIT_OPEN, Room.TILE_KEY}, room.getMapLayout()[0]);
    }
}