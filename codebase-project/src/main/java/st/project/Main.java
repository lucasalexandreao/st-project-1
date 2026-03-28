package st.project;

import javax.swing.*;

public class Main {
    public static void main(String[] args){
        // Configuração da Janela (JFrame)
        JFrame window = new JFrame("World of Zuul 2D - A Busca pela Chave");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Adiciona o nosso GamePanel visual
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);

        // Define o tamanho da janela baseado no tamanho do mapa (TILE_SIZE * grade)
        window.pack(); // Faz a janela se ajustar ao painel interno
        window.setSize(620, 480); // Ajuste manual para caber o mapa de teste

        window.setResizable(false); // Não permite redimensionar
        window.setLocationRelativeTo(null); // Centraliza na tela
        window.setVisible(true);

        gamePanel.requestFocusInWindow();
    }
}