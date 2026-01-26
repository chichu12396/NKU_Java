package Gomoku;

import java.util.*;

public class MCTS {

	private static final Random random = new Random();
	private int aiColor;

	public MCTS(int aiColor) {
		this.aiColor = aiColor;
	}

	private static class Node {
		int[][] board;
		int color;
		Node parent;
		List<Node> children = new ArrayList<>();
		int[] move;
		int wins = 0;
		int visits = 0;
		boolean fullyExpanded = false;

		Node(int[][] board, int color, Node parent, int[] move) {
			this.board = board;
			this.color = color;
			this.parent = parent;
			this.move = move;
		}
	}

	// 核心
	public int[] search(int[][] board, int maxIterations, long maxMillis) {
		Node root = new Node(cloneBoard(board), aiColor, null, null);
		long startTime = System.currentTimeMillis();

		for (int i = 0; i < maxIterations
				&& (System.currentTimeMillis() - startTime) < maxMillis; i++) {
			Node leaf = treePolicy(root);
			int winner = defaultPolicy(leaf.board, leaf.color);
			backpropagate(leaf, winner);
		}

		Node best = bestChild(root, 0); // 纯 exploitation
		return (best != null && best.move != null) ? best.move
				: chooseRandomEmpty(board);
	}

	// 选择
	private Node treePolicy(Node node) {
		while (!isTerminal(node.board)) {
			if (!node.fullyExpanded)
				return expand(node);
			Node next = bestChild(node, 1.4);
			if (next == null)
				break;
			node = next;
		}
		return node;
	}

	// 找最好的
	private Node bestChild(Node node, double c) {
		if (node.children.isEmpty())
			return null;
		Node best = null;
		double bestVal = Double.NEGATIVE_INFINITY;

		for (Node child : node.children) {
			double ucb = (child.visits == 0) ? 999999
					: ((double) child.wins / child.visits)
							+ c
							* Math.sqrt(Math.log(node.visits + 1)
									/ child.visits);

			if (child.move != null) {
				int threatScore = isLiveLine(node.board, child.move,
						child.color, 4) ? 1000 : isLiveLine(node.board,
						child.move, child.color, 3) ? 500 : 0;
				ucb += evaluateMove(child.move) * 0.5 + threatScore;
			}

			if (ucb > bestVal) {
				bestVal = ucb;
				best = child;
			}
		}
		return best;
	}

	// 扩展
	private Node expand(Node node) {
		List<int[]> moves = getCandidateMoves(node.board);
		boolean[][] tried = new boolean[moves.size()][2];
		for (Node c : node.children)
			if (c.move != null)
				for (int i = 0; i < moves.size(); i++)
					if (moves.get(i)[0] == c.move[0]
							&& moves.get(i)[1] == c.move[1])
						tried[i][0] = true;

		// 1. 自己连五必下
		for (int i = 0; i < moves.size(); i++) {
			int[] m = moves.get(i);
			if (!tried[i][0] && canWin(node.board, m, node.color))
				return createChild(node, m);
		}

		// 2. 阻挡对手连五
		for (int i = 0; i < moves.size(); i++) {
			int[] m = moves.get(i);
			if (!tried[i][0] && canWin(node.board, m, -node.color))
				return createChild(node, m);
		}

		// 3. 自己活四/活三优先
		List<int[]> strongMoves = new ArrayList<>();
		for (int i = 0; i < moves.size(); i++) {
			int[] m = moves.get(i);
			if (!tried[i][0]
					&& (isLiveLine(node.board, m, node.color, 4) || isLiveLine(
							node.board, m, node.color, 3))) {
				strongMoves.add(m);
			}
		}
		if (!strongMoves.isEmpty())
			return createChild(node,
					pickBestMove(node, strongMoves, node.color));

		// 4. 阻挡对手活四/活三
		List<int[]> blockMoves = new ArrayList<>();
		for (int i = 0; i < moves.size(); i++) {
			int[] m = moves.get(i);
			if (!tried[i][0]
					&& (isLiveLine(node.board, m, -node.color, 4) || isLiveLine(
							node.board, m, -node.color, 3))) {
				blockMoves.add(m);
			}
		}
		if (!blockMoves.isEmpty())
			return createChild(node,
					pickBestMove(node, blockMoves, -node.color));

		// 5. 普通扩展
		for (int i = 0; i < moves.size(); i++) {
			if (!tried[i][0])
				return createChild(node, moves.get(i));
		}

		node.fullyExpanded = true;
		return node;
	}

	// 回溯
	private void backpropagate(Node node, int winner) {
		while (node != null) {
			node.visits++;
			if (winner == aiColor)
				node.wins++;
			else if (winner == 0)
				node.wins += 0.5;
			node = node.parent;
		}
	}

	// 模拟
	private int defaultPolicy(int[][] board, int turn) {
		int[][] sim = cloneBoard(board);
		int color = turn;
		for (int t = 0; t < 40; t++) {
			List<int[]> moves = getCandidateMoves(sim);
			if (moves.isEmpty())
				return 0;

			int[] move = null;
			for (int[] m : moves)
				if (canWin(sim, m, color)) {
					move = m;
					break;
				}
			if (move == null)
				for (int[] m : moves)
					if (canWin(sim, m, -color)) {
						move = m;
						break;
					}

			if (move == null)
				move = pickRandomMove(sim, moves, color);

			sim[move[0]][move[1]] = color;
			int w = checkWinner(sim);
			if (w != Integer.MIN_VALUE)
				return w;

			color = -color;
		}
		return 0;
	}

	private int[] pickRandomMove(int[][] board, List<int[]> moves, int color) {
		List<int[]> strong = new ArrayList<>();
		for (int[] m : moves)
			if (isLiveLine(board, m, color, 4)
					|| isLiveLine(board, m, color, 3))
				strong.add(m);
		if (!strong.isEmpty())
			return strong.get(random.nextInt(strong.size()));

		List<int[]> block = new ArrayList<>();
		for (int[] m : moves)
			if (isLiveLine(board, m, -color, 3))
				block.add(m);
		if (!block.isEmpty())
			return block.get(random.nextInt(block.size()));

		return moves.get(random.nextInt(moves.size()));
	}

	private boolean isTerminal(int[][] board) {
		return checkWinner(board) != Integer.MIN_VALUE
				|| getCandidateMoves(board).isEmpty();
	}

	// 找能下的位置
	private List<int[]> getCandidateMoves(int[][] board) {
		List<int[]> moves = new ArrayList<>();
		int N = board.length;
		int range = 2;
		for (int r = 0; r < N; r++)
			for (int c = 0; c < N; c++) {
				if (board[r][c] != 0)
					continue;
				boolean near = false;
				for (int dr = -range; dr <= range && !near; dr++)
					for (int dc = -range; dc <= range && !near; dc++)
						if (r + dr >= 0 && r + dr < N && c + dc >= 0
								&& c + dc < N && board[r + dr][c + dc] != 0)
							near = true;
				if (near)
					moves.add(new int[] { r, c });
			}
		if (moves.isEmpty())
			for (int r = 0; r < N; r++)
				for (int c = 0; c < N; c++)
					if (board[r][c] == 0)
						moves.add(new int[] { r, c });
		return moves;
	}

	// 检查能不能赢
	private boolean canWin(int[][] board, int[] move, int color) {
		return countLine(board, move, color, 5) >= 5;
	}

	private int checkWinner(int[][] board) {
		int N = board.length;
		for (int r = 0; r < N; r++)
			for (int c = 0; c < N; c++)
				for (int len = 5; len <= 5; len++)
					if (board[r][c] != 0
							&& countLine(board, new int[] { r, c },
									board[r][c], len) >= 5)
						return board[r][c];
		return Integer.MIN_VALUE;
	}

	// 判断活三活四
	private boolean isLiveLine(int[][] board, int[] m, int color, int length) {
		int count = countLine(board, m, color, length);
		if (length == 3)
			return count == 3; // live three
		if (length == 4)
			return count == 4; // live four
		return false;
	}

	private int countLine(int[][] board, int[] m, int color, int length) {
		int[][] dirs = { { 1, 0 }, { 0, 1 }, { 1, 1 }, { 1, -1 } };
		int N = board.length;
		int r = m[0], c = m[1];
		for (int[] d : dirs) {
			int cnt = 1;
			int openEnds = 0;
			for (int k = 1; k < length; k++) {
				int nr = r + d[0] * k, nc = c + d[1] * k;
				if (nr < 0 || nr >= N || nc < 0 || nc >= N)
					break;
				if (board[nr][nc] == color)
					cnt++;
				else if (board[nr][nc] == 0) {
					openEnds++;
					break;
				} else
					break;
			}
			for (int k = 1; k < length; k++) {
				int nr = r - d[0] * k, nc = c - d[1] * k;
				if (nr < 0 || nr >= N || nc < 0 || nc >= N)
					break;
				if (board[nr][nc] == color)
					cnt++;
				else if (board[nr][nc] == 0) {
					openEnds++;
					break;
				} else
					break;
			}
			if (cnt == length && openEnds == 2)
				return cnt;
		}
		return 0;
	}

	private Node createChild(Node parent, int[] move) {
		int[][] newBoard = cloneBoard(parent.board);
		newBoard[move[0]][move[1]] = parent.color;
		Node child = new Node(newBoard, -parent.color, parent, move);
		parent.children.add(child);
		if (parent.children.size() >= getCandidateMoves(parent.board).size())
			parent.fullyExpanded = true;
		return child;
	}

	private int[] pickBestMove(Node node, List<int[]> moves, int color) {
		int[] best = moves.get(0);
		int bestScore = -9999;
		for (int[] m : moves) {
			int score = (isLiveLine(node.board, m, color, 4) ? 1000
					: isLiveLine(node.board, m, color, 3) ? 500 : 0)
					+ evaluateMove(m);
			if (score > bestScore) {
				bestScore = score;
				best = m;
			}
		}
		return best;
	}

	private int evaluateMove(int[] m) {
		int center = 9;
		return -(Math.abs(m[0] - center) + Math.abs(m[1] - center));
	}

	private int[][] cloneBoard(int[][] board) {
		int N = board.length;
		int[][] newB = new int[N][N];
		for (int i = 0; i < N; i++)
			System.arraycopy(board[i], 0, newB[i], 0, N);
		return newB;
	}

	private int[] chooseRandomEmpty(int[][] board) {
		List<int[]> moves = getCandidateMoves(board);
		return moves.get(random.nextInt(moves.size()));
	}
}
