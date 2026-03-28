package st.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomTest {

    @Test
    void shouldTakeKeyAndReplaceTileWithFloor() {
        int[][] layout = {
            {Room.TILE_FLOOR, Room.TILE_KEY},
            {Room.TILE_FLOOR, Room.TILE_FLOOR}
        };

        Room room = new Room("Sala com chave", layout);

        int item = room.takeItemAt(1, 0);

        assertEquals(Room.TILE_KEY, item);
        assertEquals(Room.TILE_FLOOR, room.getMapLayout()[0][1]);
    }

    @Test
    void shouldReturnNoItemAtPosition() {
        int[][] layout = {
            {Room.TILE_FLOOR, Room.TILE_WALL},
            {Room.TILE_EXIT_LOCKED, Room.TILE_FLOOR}
        };

        Room room = new Room("Sala sem item", layout);

        int item = room.takeItemAt(0, 0);

        assertEquals(-1, item);
    }

    @Test
    void shouldOpenLockedExit() {
        int[][] layout = {
            {Room.TILE_FLOOR, Room.TILE_EXIT_LOCKED},
            {Room.TILE_WALL, Room.TILE_FLOOR}
        };

        Room room = new Room("Sala com saida", layout);

        room.openExit(1, 0);

        assertEquals(Room.TILE_EXIT_OPEN, room.getMapLayout()[0][1]);
    }

    @Test
    void shouldNotChangeTileWhenExitIsNotLocked() {
        int[][] layout = {
            {Room.TILE_FLOOR, Room.TILE_EXIT_OPEN},
            {Room.TILE_WALL, Room.TILE_FLOOR}
        };

        Room room = new Room("Sala com saida aberta", layout);

        room.openExit(1, 0);

        assertArrayEquals(new int[]{Room.TILE_FLOOR, Room.TILE_EXIT_OPEN}, room.getMapLayout()[0]);
    }
}
