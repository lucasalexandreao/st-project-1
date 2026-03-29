package st.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerTest {

	private Room createValidRoom() {
		int[][] layout = {
			{Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL},
			{Room.TILE_WALL, Room.TILE_FLOOR, Room.TILE_KEY, Room.TILE_FLOOR, Room.TILE_WALL},
			{Room.TILE_WALL, Room.TILE_KEY, Room.TILE_EXIT_LOCKED, Room.TILE_FLOOR, Room.TILE_WALL},
			{Room.TILE_WALL, Room.TILE_KEY, Room.TILE_FLOOR, Room.TILE_FLOOR, Room.TILE_WALL},
			{Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL, Room.TILE_WALL}
		};

		return new Room(layout);
	}

	@Test
	void shouldCreatePlayerOnValidFloorInsideBounds() {
		Room room = createValidRoom();

		assertDoesNotThrow(() -> new Player(1, 1, room));
	}

	@Test
	void shouldThrowWhenSpawnXIsNegative() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(-1, 1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	@Test
	void shouldThrowWhenSpawnYIsNegative() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(1, -1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	@Test
	void shouldThrowWhenSpawnIsOutOfRoomLowerBounds() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(1, room.getHeight(), room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

    @Test
	void shouldThrowWhenSpawnIsOutOfRoomRighterBounds() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(room.getWidth(), 1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	@Test
	void shouldThrowWhenSpawnTileIsNotFloor() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(2, 1, room));

		assertEquals("Player spawn precisa ser em TILE_FLOOR.", exception.getMessage());
	}

	@Test
	void shouldSetPositionWhenTileIsValid() {
		Room room = createValidRoom();
		Player player = new Player(1, 1, room);

		assertDoesNotThrow(() -> player.setPosition(3, 2, room));
		assertEquals(3, player.getGridX());
		assertEquals(2, player.getGridY());
	}

	@Test
	void shouldThrowWhenSetPositionTileIsInvalid() {
		Room room = createValidRoom();
		Player player = new Player(1, 1, room);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> player.setPosition(2, 2, room));

		assertEquals("Player spawn precisa ser em TILE_FLOOR.", exception.getMessage());
	}

	@Test
	void shouldRequireThreeKeysToHaveAllKeys() {
		Player player = new Player(1, 1);

		assertFalse(player.hasAllKeys());
		player.collectKey();
		player.collectKey();
		assertFalse(player.hasAllKeys());
		player.collectKey();
		assertTrue(player.hasAllKeys());
	}

	@Test
	void shouldMoveUpAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveUp();

		assertEquals(2, player.getGridX());
		assertEquals(1, player.getGridY());
		assertEquals(0, player.getLastDirX());
		assertEquals(-1, player.getLastDirY());
	}

	@Test
	void shouldMoveDownAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveDown();

		assertEquals(2, player.getGridX());
		assertEquals(3, player.getGridY());
		assertEquals(0, player.getLastDirX());
		assertEquals(1, player.getLastDirY());
	}

	@Test
	void shouldMoveLeftAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveLeft();

		assertEquals(1, player.getGridX());
		assertEquals(2, player.getGridY());
		assertEquals(-1, player.getLastDirX());
		assertEquals(0, player.getLastDirY());
	}

	@Test
	void shouldMoveRightAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveRight();

		assertEquals(3, player.getGridX());
		assertEquals(2, player.getGridY());
		assertEquals(1, player.getLastDirX());
		assertEquals(0, player.getLastDirY());
	}


	@Test
	void shouldResetCollectedKeys() {
		Player player = new Player(1, 1);

		player.collectKey();
		player.collectKey();
		assertEquals(2, player.getKeyCount());
		player.resetKeys();

		assertEquals(0, player.getKeyCount());
		assertFalse(player.hasAllKeys());
	}

	@Test
	void shouldStartWithoutShootingAndAmmo() {
		Player player = new Player(1, 1);

		assertFalse(player.canShoot());
		assertEquals(0, player.getAmmo());
		assertFalse(player.hasAmmo());
	}

	@Test
	void shouldUnlockShootingAndFillAmmo() {
		Player player = new Player(1, 1);

		player.unlockShooting();

		assertTrue(player.canShoot());
		assertEquals(10, player.getAmmo());
		assertTrue(player.hasAmmo());
	}

	@Test
	void shouldDecreaseAmmoUntilZeroAndNotGoNegative() {
		Player player = new Player(1, 1);
		player.unlockShooting();

		for (int i = 0; i < 12; i++) {
			player.decreaseAmmo();
		}

		assertEquals(0, player.getAmmo());
		assertFalse(player.hasAmmo());
	}
}
