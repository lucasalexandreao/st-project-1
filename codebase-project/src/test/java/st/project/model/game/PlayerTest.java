package st.project.model.game;

import org.junit.jupiter.api.Test;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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

	// PBT (Teste Baseado em Propriedades) / DOMÍNIO E FRONTEIRA
	@Test
	void propertyBased_AmmoNeverNegativeRegardlessOfUsage() {
		Random rand = new Random();

		// Vamos testar 100 jogadores diferentes apertando o botão de atirar uma quantidade aleatória de vezes
		for (int i = 0; i < 100; i++) {
			// PRÉ-CONDIÇÃO
			Player p = new Player(1, 1);
			p.unlockShooting(); // Garante as 10 munições iniciais

			// AÇÃO (O jogador aperta atirar entre 0 e 50 vezes, tentando bugar a arma)
			int shotsFired = rand.nextInt(51);
			for (int j = 0; j < shotsFired; j++) {
				p.decreaseAmmo();
			}

			// PÓS-CONDIÇÃO (A Propriedade)
			// A munição nunca passar de zero negativamente, mesmo que tente atirar 50 vezes
			assertTrue(p.getAmmo() >= 0, "A munição vazou para o negativo!");

			if (shotsFired >= 10) {
				assertEquals(0, p.getAmmo(), "A arma não descarregou corretamente");
				assertFalse(p.hasAmmo());
			} else {
				assertEquals(10 - shotsFired, p.getAmmo(), "Gasto de munição não foi subtraído corretamente");
				assertTrue(p.hasAmmo());
			}
		}
	}

	// DOMÍNIO
	@Test
	void shouldCreatePlayerOnValidFloorInsideBounds() {
		Room room = createValidRoom();

		assertDoesNotThrow(() -> new Player(1, 1, room));
	}

	// FRONTEIRA
	@Test
	void shouldThrowWhenSpawnXIsNegative() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(-1, 1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	// FRONTEIRA
	@Test
	void shouldThrowWhenSpawnYIsNegative() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(1, -1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	// FRONTEIRA
	@Test
	void shouldThrowWhenSpawnIsOutOfRoomLowerBounds() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(1, room.getHeight(), room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	// FRONTEIRA
	@Test
	void shouldThrowWhenSpawnIsOutOfRoomRighterBounds() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(room.getWidth(), 1, room));

		assertEquals("Player spawn precisa estar dentro da tela: x >= 0, y >= 0, x < largura, y < altura.", exception.getMessage());
	}

	// DOMÍNIO
	@Test
	void shouldThrowWhenSpawnTileIsNotFloor() {
		Room room = createValidRoom();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new Player(2, 1, room));

		assertEquals("Player spawn precisa ser em TILE_FLOOR.", exception.getMessage());
	}

	// DOMÍNIO
	@Test
	void shouldSetPositionWhenTileIsValid() {
		Room room = createValidRoom();
		Player player = new Player(1, 1, room);

		assertDoesNotThrow(() -> player.setPosition(3, 2, room));
		assertEquals(3, player.getGridX());
		assertEquals(2, player.getGridY());
	}

	// DOMÍNIO
	@Test
	void shouldThrowWhenSetPositionTileIsInvalid() {
		Room room = createValidRoom();
		Player player = new Player(1, 1, room);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> player.setPosition(2, 2, room));

		assertEquals("Player spawn precisa ser em TILE_FLOOR.", exception.getMessage());
	}

	// DOMÍNIO E FRONTEIRA
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

	// DOMÍNIO
	@Test
	void shouldReturnInitialLastDirection() {
		Player player = new Player(1, 1);
		// O jogador por padrão deve iniciar olhando para a direita (X = 1, Y = 0)
		assertEquals(1, player.getLastDirX());
		assertEquals(0, player.getLastDirY());
	}

	// DOMÍNIO
	@Test
	void shouldMoveUpAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveUp();

		assertEquals(2, player.getGridX());
		assertEquals(1, player.getGridY());
		assertEquals(0, player.getLastDirX());
		assertEquals(-1, player.getLastDirY());
	}

	// DOMÍNIO
	@Test
	void shouldMoveDownAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveDown();

		assertEquals(2, player.getGridX());
		assertEquals(3, player.getGridY());
		assertEquals(0, player.getLastDirX());
		assertEquals(1, player.getLastDirY());
	}

	// DOMÍNIO
	@Test
	void shouldMoveLeftAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveLeft();

		assertEquals(1, player.getGridX());
		assertEquals(2, player.getGridY());
		assertEquals(-1, player.getLastDirX());
		assertEquals(0, player.getLastDirY());
	}

	// DOMÍNIO
	@Test
	void shouldMoveRightAndUpdateLastDirection() {
		Player player = new Player(2, 2);

		player.moveRight();

		assertEquals(3, player.getGridX());
		assertEquals(2, player.getGridY());
		assertEquals(1, player.getLastDirX());
		assertEquals(0, player.getLastDirY());
	}

	// DOMÍNIO
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

	// DOMÍNIO
	@Test
	void shouldStartWithoutShootingAndAmmo() {
		Player player = new Player(1, 1);

		assertFalse(player.canShoot());
		assertEquals(0, player.getAmmo());
		assertFalse(player.hasAmmo());
	}

	// DOMÍNIO
	@Test
	void shouldUnlockShootingAndFillAmmo() {
		Player player = new Player(1, 1);

		player.unlockShooting();

		assertTrue(player.canShoot());
		assertEquals(10, player.getAmmo());
		assertTrue(player.hasAmmo());
	}

	// DOMÍNIO E FRONTEIRA
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