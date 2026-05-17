package st.project;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

import st.project.repository.JdbcLeaderboardRepository;

public class InitializeSuperuser {
    public static void main(String[] args) {
        JdbcLeaderboardRepository repo = new JdbcLeaderboardRepository("leaderboard.db");

        String passwordHash = hashPassword("admin");
        String normalizedUsername = "admin".toLowerCase(Locale.ROOT);

        repo.createUser(normalizedUsername, passwordHash, true);
        System.out.println("Superusuário 'admin' criado com sucesso!");
        System.out.println("Senha: admin");
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer hash da senha", e);
        }
    }
}
