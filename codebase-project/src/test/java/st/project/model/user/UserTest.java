package st.project.model.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    // ESTRUTURAL
    @Test
    void shouldStoreUserData() {
        User user = new User("Admin", "hash123", 5000L, 10, true);

        assertEquals("Admin", user.getPlayerName());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals(5000L, user.getTotalScore());
        assertEquals(10, user.getSessionCount());
        assertTrue(user.isSuperuser());
    }
}