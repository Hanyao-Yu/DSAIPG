package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

public class GomokuState implements State<Gomoku> {
    private final int[][] board;
    private final int player;
    private final Random rnd = new Random();

    public GomokuState(int[][] board, int player) {
        this.board = board;
        this.player = player;
    }

    @Override
    public Gomoku game() { return new Gomoku(); }

    @Override
    public int player() { return player; }

    @Override
    public Optional<Integer> winner() {
        for (int i = 0; i < Gomoku.SIZE; i++) {
            for (int j = 0; j < Gomoku.SIZE; j++) {
                int p = board[i][j];
                if (p == 0) continue;
                if (check(i, j, 1, 0, p) || check(i, j, 0, 1, p)
                        || check(i, j, 1, 1, p) || check(i, j, 1, -1, p))
                    return Optional.of(p - 1);
            }
        }
        return Optional.empty();
    }

    private boolean check(int x, int y, int dx, int dy, int p) {
        for (int k = 0; k < Gomoku.WIN; k++) {
            int nx = x + dx * k, ny = y + dy * k;
            if (nx < 0 || nx >= Gomoku.SIZE || ny < 0 || ny >= Gomoku.SIZE || board[nx][ny] != p)
                return false;
        }
        return true;
    }

    @Override
    public boolean isTerminal() {
        if (winner().isPresent()) return true;
        for (int i = 0; i < Gomoku.SIZE; i++)
            for (int j = 0; j < Gomoku.SIZE; j++)
                if (board[i][j] == 0) return false;
        return true;
    }

    @Override
    public Random random() { return rnd; }

    @Override
    public Collection<Move<Gomoku>> moves(int player) {
        List<Move<Gomoku>> list = new ArrayList<>();
        for (int i = 0; i < Gomoku.SIZE; i++)
            for (int j = 0; j < Gomoku.SIZE; j++)
                if (board[i][j] == 0)
                    list.add(new GomokuMove(i, j, player));

        Optional<Move<Gomoku>> win = findImmediateWin(player);
        if (win.isPresent()) {
            GomokuMove winMove = (GomokuMove) win.get();
            list.removeIf(m -> ((GomokuMove) m).row == winMove.row && ((GomokuMove) m).col == winMove.col);
            list.add(0, win.get());
        } else {
            Optional<Move<Gomoku>> block = findImmediateWin(1 - player);
            if (block.isPresent()) {
                GomokuMove blockMove = (GomokuMove) block.get();
                list.removeIf(m -> ((GomokuMove) m).row == blockMove.row && ((GomokuMove) m).col == blockMove.col);
                list.add(0, new GomokuMove(blockMove.row, blockMove.col, player));
            }
        }
        return list;
    }

    @Override
    public Move<Gomoku> chooseMove(int player) {
        List<Move<Gomoku>> moveList = new ArrayList<>(moves(player));
        if (moveList.isEmpty()) {
            throw new IllegalStateException("No valid moves available but game is not terminal");
        }
        moveList.sort(Comparator.comparingInt(m -> -heuristic((GomokuMove) m)));
        int topN = Math.min(5, moveList.size());
        return moveList.get(random().nextInt(topN));
    }

    @Override
    public State<Gomoku> next(Move<Gomoku> move) {
        GomokuMove m = (GomokuMove) move;
        if (board[m.row][m.col] != 0) {
            System.err.println("Warning: Attempted move to occupied position [" + m.row + "," + m.col + "]");
            for (int i = 0; i < Gomoku.SIZE; i++) {
                for (int j = 0; j < Gomoku.SIZE; j++) {
                    if (board[i][j] == 0) {
                        System.err.println("Using fallback move to [" + i + "," + j + "] instead");
                        m = new GomokuMove(i, j, m.player);
                        break;
                    }
                }
                if (board[m.row][m.col] == 0) break;
            }
        }
        int[][] copy = new int[Gomoku.SIZE][Gomoku.SIZE];
        for (int i = 0; i < Gomoku.SIZE; i++)
            System.arraycopy(board[i], 0, copy[i], 0, Gomoku.SIZE);
        copy[m.row][m.col] = m.player + 1;
        return new GomokuState(copy, 1 - m.player);
    }

    public Optional<Move<Gomoku>> findImmediateWin(int player) {
        int p = player + 1;
        for (int i = 0; i < Gomoku.SIZE; i++) {
            for (int j = 0; j < Gomoku.SIZE; j++) {
                if (board[i][j] != 0) continue;
                board[i][j] = p;
                boolean ok = check(i, j, 1, 0, p) || check(i, j, 0, 1, p)
                        || check(i, j, 1, 1, p) || check(i, j, 1, -1, p);
                board[i][j] = 0;
                if (ok) return Optional.of(new GomokuMove(i, j, player));
            }
        }
        return Optional.empty();
    }

    private int heuristic(GomokuMove move) {
        int center = Gomoku.SIZE / 2;
        int score = (Gomoku.SIZE * 2) - (Math.abs(move.row - center) + Math.abs(move.col - center));
        int neighbors = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = move.row + dx;
                int y = move.col + dy;
                if (x >= 0 && x < Gomoku.SIZE && y >= 0 && y < Gomoku.SIZE) {
                    if (board[x][y] != 0) neighbors++;
                }
            }
        }
        score += neighbors * 10;
        score += detectThreats(move.row, move.col, 1 - move.player) * 15;
        score += detectThreats(move.row, move.col, move.player) * 8;
        return score;
    }

    private int detectThreats(int row, int col, int player) {
        int threatScore = 0;
        int p = player + 1;
        board[row][col] = p;
        int[] dx = {1, 0, 1, 1};
        int[] dy = {0, 1, 1, -1};
        for (int d = 0; d < 4; d++) {
            int count = 1;
            for (int dir = -1; dir <= 1; dir += 2) {
                int step = 1;
                while (true) {
                    int x = row + dir * step * dx[d];
                    int y = col + dir * step * dy[d];
                    if (x < 0 || x >= Gomoku.SIZE || y < 0 || y >= Gomoku.SIZE) break;
                    if (board[x][y] == p) {
                        count++;
                        step++;
                    } else break;
                }
            }
            if (count == 4) threatScore += 3;
            else if (count == 3) threatScore += 2;
            else if (count == 2) threatScore += 1;
        }
        board[row][col] = 0;
        return threatScore;
    }

    public int[][] getBoard() { return board; }
}