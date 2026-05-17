package st.project;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PostGameDialog {
    public enum PostGameAction {
        PLAY_AGAIN,
        RETURN_TO_MENU,
        CLOSE
    }

    public static PostGameAction show(Component parent, boolean won, List<LeaderboardEntry> leaderboard, long currentRunMillis) {
        StringBuilder sb = new StringBuilder();
        if (won) {
            sb.append("VOCÊ ZEROU O JOGO!\n\n");
        } else {
            sb.append("GAME OVER\n\n");
        }
        sb.append("Tempo desta corrida: ").append(String.format("%.2fs", currentRunMillis / 1000.0)).append("\n\n");
        sb.append("Top resultados:\n");
        if (leaderboard.isEmpty()) {
            sb.append("(sem tempos registrados)\n");
        } else {
            for (int i = 0; i < leaderboard.size(); i++) {
                LeaderboardEntry e = leaderboard.get(i);
                sb.append(String.format("%d. %s - %.2fs\n", i + 1, e.getPlayerName(), e.getCompletionMillis() / 1000.0));
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(400, 300));

        Object[] options = {"Jogar novamente", "Voltar ao menu inicial", "Fechar"};
        int choice = JOptionPane.showOptionDialog(
            SwingUtilities.getWindowAncestor(parent),
            scroll,
            won ? "Resultados" : "GAME OVER",
            JOptionPane.DEFAULT_OPTION,
            won ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
            return PostGameAction.PLAY_AGAIN;
        }

        if (choice == 1) {
            return PostGameAction.RETURN_TO_MENU;
        }

        return PostGameAction.CLOSE;
    }
}
