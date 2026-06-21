package st.project.view;

import st.project.repository.LeaderboardRepository;

public class UserManagementPO extends GamePageObject {
    private final LeaderboardRepository repo;
    private final String loggedInAdmin;
    private boolean ready = false;

    public UserManagementPO(LeaderboardRepository repo, String admin) {
        this.repo = repo;
        this.loggedInAdmin = admin;
    }

    @Override
    public void isReady() {
        this.ready = true;
    }

    public void deletarUsuario(String userToDelete) {
        isReady();
        repo.deleteUser(userToDelete);
    }
}