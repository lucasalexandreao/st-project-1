package st.project;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RankingDialog {
    public static void show(Component parent, LeaderboardRepository repository) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Ranking", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(parent);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Por Tempo", createTimeRankingPanel(repository));
        tabbedPane.addTab("Por Sessões", createSessionsRankingPanel(repository));

        dialog.add(tabbedPane);
        dialog.setVisible(true);
    }

    private static JPanel createTimeRankingPanel(LeaderboardRepository repository) {
        List<LeaderboardEntry> topResults = repository.getTopScores(10);

        StringBuilder sb = new StringBuilder();
        sb.append("RANKING POR TEMPO\n");
        sb.append("=".repeat(50)).append("\n\n");

        if (topResults.isEmpty()) {
            sb.append("Sem usuários registrados.\n");
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : topResults) {
                sb.append(String.format("%2d. %-15s Tempo: %s\n",
                    rank,
                    entry.getPlayerName(),
                    formatDuration(entry.getCompletionMillis())));
                rank++;
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static String formatDuration(long millis) {
        return String.format("%.2fs", millis / 1000.0);
    }

    private static JPanel createSessionsRankingPanel(LeaderboardRepository repository) {
        List<User> topUsers = repository.getTopUsersBySessions(10);

        StringBuilder sb = new StringBuilder();
        sb.append("RANKING POR SESSÕES\n");
        sb.append("=".repeat(50)).append("\n\n");

        if (topUsers.isEmpty()) {
            sb.append("Sem usuários registrados.\n");
        } else {
            int rank = 1;
            for (User user : topUsers) {
                sb.append(String.format("%2d. %-15s %s Sessões: %d\n",
                    rank,
                    user.getPlayerName(),
                    user.isSuperuser() ? "👑" : "  ",
                    user.getSessionCount()));
                rank++;
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
}
