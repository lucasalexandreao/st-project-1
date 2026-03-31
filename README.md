# Labirinto 2D - A Busca pela Chave

Este projeto é uma evolução completa do clássico jogo de terminal "World of Zuul", transformado em um jogo 2D top-down com interface gráfica nativa em Java (Swing/AWT). O jogador deve navegar por um labirinto, coletar itens, desviar de inimigos e gerenciar seu tempo e munição para escapar.

## 🎮 Como Jogar

### O Objetivo
Você está preso em um labirinto e o relógio está correndo! Você tem **30 segundos** para escapar de cada fase. 
* **Fase 1:** Encontre os 3 fragmentos de chave espalhados pelo mapa para destrancar a porta de saída.
* **Fase 2:** Sobreviva aos inimigos! Ao chegar aqui, você desbloqueia a habilidade de atirar. Use sua munição com sabedoria para derrotar as ameaças e encontrar a saída.

### Legenda do Mapa
* 🟩 **Quadrado Verde:** Você (O Jogador).
* 🟨 **Círculo Amarelo:** Fragmento de Chave (Colete 3 para abrir a porta).
* 🟥 **Quadrado Vermelho:** Porta Trancada.
* 🟦 **Quadrado Azul Claro:** Porta Aberta (A saída!).
* 🟧 **Triângulo Laranja:** Inimigo (Eles atiram a cada 2 segundos, cuidado!).

### Controles
* **Setas do Teclado (⬆️ ⬇️ ⬅️ ➡️):** Movem o personagem pelo labirinto.
* **Barra de Espaço:** Dispara um projétil na direção do seu último movimento (Requer munição e só é desbloqueado na Fase 2).

---

## 🚀 Como Executar o Projeto

**Pré-requisitos:**
* Java Development Kit (JDK) instalado (versão 8 ou superior).
* Uma IDE de sua preferência (IntelliJ IDEA, Eclipse, VS Code).

**Passo a passo:**
1. Clone ou faça o download deste repositório.
2. Abra a pasta do projeto na sua IDE.
3. Localize o arquivo principal: `src/st/project/Main.java`.
4. Clique com o botão direito no arquivo e selecione **Run 'Main.main()'**.
5. A janela do jogo se abrirá automaticamente e começará a rodar a 30 FPS. Clique na janela para garantir que ela tenha o foco do seu teclado e divirta-se!

---

## 🧪 Engenharia de Software e Testes

Este projeto foi construído com fortes princípios de Engenharia de Software, atingindo a cobiçada marca de **100% de Cobertura de Código (Code Coverage)**.

A suíte de testes automatizados utiliza **JUnit 5** e **Mockito** e cobre diversos cenários críticos:

* **Testes de Domínio:** Garantem as regras de negócio do jogo, como a necessidade de ter exatamente 3 chaves para abrir uma porta e o gerenciamento correto da mochila de munição.
* **Testes de Fronteira:** O jogo é blindado contra anomalias. Há testes rigorosos garantindo que entidades não nasçam dentro de paredes, que o tempo não fique negativo e que os projéteis sejam destruídos imediatamente ao cruzarem as fronteiras máximas e mínimas do mapa (`x < 0` ou `x >= largura`).
* **Testes Estruturais (Caixa Branca) e MC/DC:** Utilizamos *Java Reflection* para manipular o loop temporal do jogo (`GamePanel`) e testar a Interface Gráfica sem necessidade de um monitor ligado. Além disso, a lógica de colisão foi submetida a critérios **MC/DC (Modified Condition/Decision Coverage)**, com testes provando de forma isolada que a diferença em uma única coordenada (X ou Y) é capaz de alterar o resultado de uma colisão.
