package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;

public class Main {
    private static final int BLACK_ITERS = 10000; 
    private static final int WHITE_ITERS = 5000; 

    public static void main(String[] args) {
        System.out.println("Starting Gomoku game - Black first with " + BLACK_ITERS + 
                           " iterations, White with " + WHITE_ITERS + " iterations");
        System.out.println("==========================================");
        
        runOneGame(BLACK_ITERS, WHITE_ITERS);
    }

    private static void runOneGame(int blackIters, int whiteIters) {

        Instant gameStart = Instant.now();
        
        GomokuState state = new GomokuState(new int[Gomoku.SIZE][Gomoku.SIZE], 0);
        
        System.out.println("Initial board:");
        printBoard(state);
        
        int moveCount = 0;

        while (!state.isTerminal()) {
            int pl = state.player();

            int iters = pl == 0 ? blackIters : whiteIters;
            String playerColor = pl == 0 ? "Black" : "White";
            
            Instant stepStart = Instant.now();
            
            GomokuNode root = new GomokuNode(state);
            GomokuMCTS mcts = new GomokuMCTS(root);
            Move<Gomoku> mv = mcts.findNextMove(iters);
            
            Duration stepDur = Duration.between(stepStart, Instant.now());
            
            moveCount++;
            System.out.printf(
                "Move #%d: %s (iters=%d) plays: %s (time: %d ms)%n",
                moveCount, playerColor, iters, mv, stepDur.toMillis());
            
            state = (GomokuState) state.next(mv);

            printBoard(state);
        }
        

        Instant gameEnd = Instant.now();
        Duration gameDur = Duration.between(gameStart, gameEnd);
        long gameDurationMs = gameDur.toMillis();
        
        Optional<Integer> w = state.winner();
        int winner = w.orElse(-1);
        
        System.out.println("==========================================");
        System.out.println("Game finished!");
        if (winner == -1) {
            System.out.println("Result: Draw");
        } else {
            String winnerColor = winner == 0 ? "Black" : "White";
            System.out.println("Result: " + winnerColor + " wins");
        }
        System.out.printf("Total moves: %d%n", moveCount);
        System.out.printf("Total time: %d ms%n", gameDurationMs);
    }

    private static void printBoard(GomokuState state) {
        int[][] b = state.getBoard();
        System.out.print("  ");
        for (int j = 0; j < Gomoku.SIZE; j++) {
            System.out.print((j % 10) + " ");
        }
        System.out.println();
        for (int i = 0; i < Gomoku.SIZE; i++) {
            System.out.print((i % 10) + " ");
            for (int j = 0; j < Gomoku.SIZE; j++) {
                char c = b[i][j] == 0 ? '.'
                       : b[i][j] == 1 ? '●'  
                       : '○';  
                System.out.print(c + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}