package st.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTest {

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
