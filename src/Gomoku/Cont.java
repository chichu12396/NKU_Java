package Gomoku;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Cont {
	private boolean aiMode = false;
	private DeepSeekAI aiPlayer = null;
	private int aiColor = Model.WHITE; // 默认玩家是黑棋先手，AI 白棋
	private String aiDifficulty; // 默认 AI 难度
	// ========== 游戏状态字段 ==========
	public int currentcolor = Model.BLACK;
	public boolean flag = false; // 游戏结束标志
	private boolean isReviewing = false;

	// ========== 网络相关字段 ==========
	public boolean isOnlineMode = false;
	public int myColor = Model.BLACK;
	public boolean isMyTurn = false;

	// ========== 悔棋请求相关字段 ==========
	private boolean undoRequested = false;
	private boolean waitingUndoResponse = false;

	// ========== 倒计时管理器（替代原 Timer） ==========
	private CountdownManager countdownManager = null;

	// ========== 单例 ==========
	private static Cont instance = null;

	private Cont() {
		// 初始 currentcolor 保持 Model.BLACK
		currentcolor = Model.BLACK;
	}

	public static Cont getInstance() {
		if (instance == null) {
			instance = new Cont();
		}
		return instance;
	}

	// ========== 确保倒计时管理器存在并初始化监听 ==========
	private void ensureCountdownManager() {
		if (countdownManager != null)
			return;

		countdownManager = new CountdownManager(Model.COUNTDOWN_TIME,
				currentcolor, new CountdownManager.CountdownListener() {

					@Override
					public void onTick(final int remainingSeconds,
							final int currentTurnColor) {
						// 在 Swing 线程更新 UI
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								View.getInstance().updateCountdown(
										remainingSeconds, currentTurnColor);
							}
						});
					}

					@Override
					public void onStopped() {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								// -1 表示隐藏或等待
								View.getInstance().updateCountdown(-1,
										Model.BLACK);
							}
						});
					}

					@Override
					public void onTimeout(final int timeoutColor) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								handleTimeoutFromCountdown(timeoutColor);
							}
						});
					}
				});
	}

	private final Object moveLock = new Object(); // 防止并发或递归落子

	public void userputchess(final int row, final int col) {

		synchronized (moveLock) {

			// 游戏结束或复盘中禁止落子
			if (flag || isReviewing)
				return;

			// 联机但不是自己回合
			if (isOnlineMode && !isMyTurn) {
				View.getInstance().displayChatMessage("系统", "请等待对手落子");
				return;
			}

			// ========== 实际落子 ==========
			boolean success = Model.getInstance().putchess(row, col,
					currentcolor);
			if (!success)
				return;
			Music.getInstance().playClickSound();
			if(!Music.getInstance().hasmusic())
				{System.out.println("hhhh");
				Music.getInstance().startBackgroundMusic();
				}
			stopCountdown();

			// 网络模式：发送真实落子
			if (isOnlineMode) {
				NetworkManager.getInstance().sendMove(row, col, currentcolor);
				isMyTurn = false;
			}

			// 切换颜色
			currentcolor = -currentcolor;

			// 重启倒计时
			resetAndStartCountdown();

			// ========== 检查胜负 ==========
			int winner = Model.getInstance().whowin();
			if (winner != Model.SPACE) {
				Music.getInstance().stopAllSounds();
				Music.getInstance().playEndMusic();
				flag = true;
				stopCountdown();

				if (isOnlineMode) {
					NetworkManager.getInstance().sendMessage("GAME_END",
							String.valueOf(winner));
					View.getInstance().displayChatMessage("系统",
							winner == myColor ? "恭喜！你获胜了！" : "对手获胜！");
				}

				final int w = winner;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (!isReviewing) {
						    ArrayList<int[]> winningLine = Model.getInstance().getWinningLine();
						    View.getInstance().setWinningLine(winningLine);
						    View.getInstance().showGameEndDialog(w);
						}
					}
				});

				View.getInstance().updateBoard();
				return; // 禁止 AI 再走
			}

			View.getInstance().updateBoard();

			// ========== AI 自动落子 ==========
			if (aiMode && currentcolor == aiColor && !flag) {

				new Thread(new Runnable() {
					@Override
					public void run() {

						synchronized (moveLock) {

							if (flag)
								return; // 胜负后不走

							int[][] board = Model.getInstance().getBoardCopy();
							int[] move = aiPlayer.computeAIMove(board, aiColor);

							if (move != null) {
								final int r = move[0];
								final int c = move[1];

								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										if (!flag) {
											userputchess(r, c);
										}
									}
								});
							}
						}

					}
				}).start();
			}
		}
	}
	public void showGameEndDialogAfterHighlight(final int winner) {
	    // 获取获胜的五连棋
	    ArrayList<int[]> winningLine = Model.getInstance().getWinningLine();
	    
	    // 设置到视图进行高亮显示
	    View.getInstance().setWinningLine(winningLine);
	    
	    // 短暂延迟后显示弹窗
	    Timer timer = new Timer(1000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				View.getInstance().showGameEndDialog(winner);
				
			}
	    });
	    timer.setRepeats(false);
	    timer.start();
	}

	// ========== 悔棋流程 ==========
	public void undo() {
		if (flag) {
			System.out.println("游戏已结束，不能悔棋");
			return;
		}
		if (isOnlineMode) {
			if (undoRequested) {
				View.getInstance().displayChatMessage("系统", "已经发送过悔棋请求，等待对方响应");
				return;
			}
			if (Model.getInstance().getPureGameRecord().size() == 0) {
				View.getInstance().displayChatMessage("系统", "还没有落子，不能悔棋");
				return;
			}

			// 标记请求状态并暂停本地倒计时（避免超时）
			undoRequested = true;
			waitingUndoResponse = true;

			ensureCountdownManager();
			// 停止倒计时，等待对方响应
			countdownManager.stop();

			NetworkManager.getInstance().sendMessage("UNDO_REQUEST", "");
			View.getInstance().displayChatMessage("系统", "已向对手发送悔棋请求，等待对方同意");
			View.getInstance().showUndoRequest(false, true);
			System.out.println("发送悔棋请求，当前undoRequested=" + undoRequested);
		} else {
			performSingleUndo();
		}
	}

	public void processUndoRequest() {
		if (isOnlineMode && !flag) {
			// 收到对手请求，立即暂停本地倒计时，等待用户同意或拒绝
			ensureCountdownManager();
			countdownManager.stop();

			View.getInstance().displayChatMessage("系统", "对手请求悔棋");
			View.getInstance().showUndoRequest(true, false);
		}
	}

	public void acceptUndo() {
		if (!isOnlineMode)
			return;

		if (undoRequested) {
			// 我是发起方，收到对方的同意
			performUndo();
			undoRequested = false;
			waitingUndoResponse = false;
			View.getInstance().hideUndoRequest();
		} else {
			// 我是接收方，同意对方的悔棋请求：通知对方并执行本地回退
			NetworkManager.getInstance().sendMessage("UNDO_ACCEPT", "");
			View.getInstance().displayChatMessage("系统", "已同意对手悔棋");
			View.getInstance().hideUndoRequest();

			performOpponentUndo();
		}
	}

	public void rejectUndo() {
		if (!isOnlineMode)
			return;

		if (undoRequested) {
			// 我是发起方，取消自己的请求（理论上少见）
			undoRequested = false;
			waitingUndoResponse = false;

			// 发起方取消请求后，需要恢复当前回合倒计时
			ensureCountdownManager();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();
		} else {
			// 我是接收方，拒绝对方请求并通知对方
			NetworkManager.getInstance().sendMessage("UNDO_REJECT", "");
			View.getInstance().displayChatMessage("系统", "已拒绝对手悔棋");

			// 接收方拒绝后，恢复原先的倒计时（继续当前回合）
			ensureCountdownManager();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();
		}
		View.getInstance().hideUndoRequest();
	}

	// 发起方实际执行悔棋（收到对方同意）
	private void performUndo() {
		boolean success = Model.getInstance().undoMove();
		if (success) {
			Music.getInstance().playClickSound();
			// 切换当前颜色（回退一步）
			currentcolor = -currentcolor;
			// 发起方悔棋后，回合也要切换（我方/对方标志互换）
			isMyTurn = !isMyTurn;

			// --- 倒计时同步（关键修复） ---
			ensureCountdownManager();
			// 先停止旧计时
			countdownManager.stop();
			// 将计时器的当前回合颜色设置为新的 currentcolor
			countdownManager.setCurrentTurnColor(currentcolor);
			// 重置剩余时间为初始值
			countdownManager.reset(Model.COUNTDOWN_TIME);
			// 重新启动计时器
			countdownManager.start();
			// --- end 倒计时同步 ---

			View.getInstance().updateBoard();
			Model.getInstance().discount();
			View.getInstance().displayChatMessage("系统", "悔棋成功");
		}
	}

	// 接收方同意后执行对手悔棋（本地回退）
	private void performOpponentUndo() {
		boolean success = Model.getInstance().undoMove();
		if (success) {
			Music.getInstance().playClickSound();
			currentcolor = -currentcolor;
			isMyTurn = !isMyTurn;

			// 倒计时同步：轮到谁就显示谁并重置时间
			ensureCountdownManager();
			countdownManager.stop();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();

			View.getInstance().updateBoard();
			Model.getInstance().discount();
			View.getInstance().displayChatMessage("系统", "对手悔棋，已回退一步");
		}
	}

	public void processUndoAccept() {
		if (isOnlineMode && waitingUndoResponse) {
			// 对方同意了我的悔棋请求：执行发起方的回退流程
			performUndo();
			undoRequested = false;
			waitingUndoResponse = false;
			View.getInstance().hideUndoRequest();
			View.getInstance().displayChatMessage("系统", "对手同意了你的悔棋请求");
		}
	}

	public void processUndoReject() {
		if (isOnlineMode && waitingUndoResponse) {
			// 对方拒绝了我的悔棋请求：恢复倒计时并清理状态
			undoRequested = false;
			waitingUndoResponse = false;
			View.getInstance().hideUndoRequest();
			View.getInstance().displayChatMessage("系统", "对手拒绝了你的悔棋请求");

			// 恢复倒计时（继续当前回合）
			ensureCountdownManager();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();
		}
	}


	private void performSingleUndo() {
		boolean success = Model.getInstance().undoMove();
		if (success) {
			currentcolor = -currentcolor;
			Music.getInstance().playClickSound();
			// 本地模式也需要重置倒计时（切换回合）
			ensureCountdownManager();
			countdownManager.stop();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();

			View.getInstance().updateBoard();
			Model.getInstance().discount();
			View.getInstance().displayChatMessage("系统", "悔棋成功");
			System.out.println("单机悔棋成功");
		} else {
			View.getInstance().displayChatMessage("系统", "无法悔棋");
			System.out.println("单机悔棋失败");
		}
	}

	// ========== 复盘 ==========
	public void startReview() {
		if (!flag) {
			System.out.println("游戏尚未结束");
			return;
		}
		isReviewing = true;
		Model.getInstance().resetReview();
		View.getInstance().initReviewUI();
		if (isOnlineMode) {
			View.getInstance().displayChatMessage("系统", "开始复盘模式");
		}
	}

	public void reviewStep(boolean direction) {
		if (!isReviewing)
			return;
		int[] step = direction ? Model.getInstance().nextReviewStep() : Model
				.getInstance().prevReviewStep();
		if (step != null) {
			View.getInstance().displayReviewStep(step[0], step[1], step[2],
					Model.getInstance().currentReviewIndex + 1, direction);
		}
	}

	public void exitReview() {
		isReviewing = false;
		View.getInstance().hideReviewUI();

		if (flag) {
			View.getInstance().showGamePanel();
			View.getInstance().updateBoard();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					int winner = Model.getInstance().whowin();
					View.getInstance().showGameEndDialog(winner);
				}
			});
		}
	}

	// ========== 重新开始 / 返回主菜单 ==========
	public void restartGame() {
		Model.getInstance().resetGame();
		currentcolor = Model.BLACK;
		flag = false;
		isOnlineMode = false;
		isMyTurn = false;
		undoRequested = false;
		waitingUndoResponse = false;

		// 启动倒计时
		ensureCountdownManager();
		countdownManager.reset(Model.COUNTDOWN_TIME);
		if (!aiMode)
			countdownManager.start();
		Music.getInstance().stopAllSounds();
		Music.getInstance().startBackgroundMusic();
		View.getInstance().showGamePanel();
		View.getInstance().updateBoard();
		View.getInstance().resetchat();
		View.getInstance().hideUndoRequest();
		View.getInstance().displayChatMessage("系统", "游戏重新开始！");
		if (aiMode) {
			View.getInstance().showAISetupDialog();
		}
	}

	public void startNewGame() {
		Model.getInstance().resetGame();
		currentcolor = Model.BLACK;
		flag = false;
		isOnlineMode = false;
		isMyTurn = true;
		undoRequested = false;
		waitingUndoResponse = false;
		isReviewing = false;

		View.getInstance().hideUndoRequest();
		View.getInstance().hideRestartRequest();
		View.getInstance().resetchat();

		ensureCountdownManager();
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.reset(Model.COUNTDOWN_TIME);
		countdownManager.start();
		Music.getInstance().stopAllSounds();
		Music.getInstance().startBackgroundMusic();
		View.getInstance().showGamePanel();
		View.getInstance().updateBoard();
	}

	public void returnToMainMenu() {
		stopCountdown();
		Model.getInstance().resetGame();
		currentcolor = Model.BLACK;
		flag = false;
		isOnlineMode = false;
		isMyTurn = false;
		undoRequested = false;
		waitingUndoResponse = false;
		isReviewing = false;
		Music.getInstance().stopAllSounds();
		Music.getInstance().playAppStartSound();
		View.getInstance().hideUndoRequest();
		View.getInstance().hideRestartRequest();
		View.getInstance().resetchat();
		View.getInstance().showStartPanel();
		View.getInstance().updateBoard();
	}

	// ========== 聊天 ==========
	public void sendChatMessage(String message) {
		if (isOnlineMode) {
			NetworkManager.getInstance().sendChat(message);
			View.getInstance().displayChatMessage("我", message);
		} else {
			String sender = currentcolor == Model.BLACK ? "黑棋方" : "白棋方";
			Model.getInstance().addChatMessage(sender, message);
			View.getInstance().displayChatMessage(sender, message);
		}
		Music.getInstance().playClickSound();

	}

	// ========== 网络整合 ==========
	public void setNetworkInfo(boolean online, boolean isBlack) {
		this.isOnlineMode = online;
		this.myColor = isBlack ? Model.BLACK : Model.WHITE;
		this.isMyTurn = isBlack;
		this.flag = false;
		this.currentcolor = Model.BLACK;
		this.undoRequested = false;
		this.waitingUndoResponse = false;

		Model.getInstance().resetGame();

		ensureCountdownManager();
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.reset(Model.COUNTDOWN_TIME);
		// 启动倒计时以保持双方界面同步（显示由 listener 决定）
		countdownManager.start();
		Music.getInstance().stopAllSounds();
		Music.getInstance().startBackgroundMusic();
		
		View.getInstance().updateBoard();
		View.getInstance().displayChatMessage("系统",
				isBlack ? "你执黑先行，请落子" : "你执白后行，等待对手落子");

		System.out.println("网络模式设置: 在线=" + online + ", 我的颜色="
				+ (isBlack ? "黑棋" : "白棋") + ", 我的回合=" + isMyTurn);
	}

	public void processNetworkMove(int row, int col, int color) {
		if (!isOnlineMode)
			return;

		System.out.println("处理网络落子: " + row + "," + col + ", 颜色:" + color
				+ ", 我的颜色:" + myColor);

		if (color != myColor) {
			Music.getInstance().playClickSound();
			Model.getInstance().putchess(row, col, color);
			currentcolor = -currentcolor;
			isMyTurn = true;

			// 重置并启动倒计时（轮到我）
			ensureCountdownManager();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();

			View.getInstance().updateBoard();
			View.getInstance().displayChatMessage("系统", "对手已落子，轮到你了");

			int winner = Model.getInstance().whowin();
			if (winner != Model.SPACE) {
				flag = true;
				stopCountdown();
				View.getInstance().displayChatMessage("系统",
						winner == myColor ? "恭喜！你获胜了！" : "对手获胜！");
			}
		} else {
			System.out.println("忽略自己的落子回传");
		}
	}

	public void processTimeout(final int winner) {
		if (!isOnlineMode)
			return;

		flag = true;
		stopCountdown();
		Music.getInstance().playEndMusic();
		String message = (winner == myColor) ? "对手超时！你获胜！" : "你超时！对手获胜！";
		View.getInstance().displayChatMessage("系统", message);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				View.getInstance().showGameEndDialog(winner);
			}
		});

		View.getInstance().updateBoard();
	}

	public void processNetworkUndo() {
		if (!isOnlineMode)
			return;

		boolean success = Model.getInstance().undoMove();
		if (success) {
			Music.getInstance().playClickSound();
			currentcolor = -currentcolor;
			isMyTurn = true;
			View.getInstance().updateBoard();
			View.getInstance().displayChatMessage("系统", "对手请求悔棋");
			Model.getInstance().discount();

			// 重置倒计时给我
			ensureCountdownManager();
			countdownManager.setCurrentTurnColor(currentcolor);
			countdownManager.reset(Model.COUNTDOWN_TIME);
			countdownManager.start();
		}
	}

	public void processGameEnd(final int winner) {
		if (!isOnlineMode)
			return;

		flag = true;
		stopCountdown();
		String resultMsg = winner == myColor ? "恭喜！你获胜了！" : "对手获胜！";
		View.getInstance().displayChatMessage("系统", resultMsg);
		Music.getInstance().playEndMusic();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ArrayList<int[]> winningLine = Model.getInstance().getWinningLine();
				View.getInstance().setWinningLine(winningLine);
				View.getInstance().showGameEndDialog(winner);
			}
		});

		View.getInstance().updateBoard();
	}

	public void connectToServer(String ip, int port, int colorChoice) {
		boolean success = NetworkManager.getInstance()
				.connectToServer(ip, port);
		if (success) {
			Music.getInstance().startBackgroundMusic();
			View.getInstance().showGamePanel();
			if (colorChoice == 1) {
				setNetworkInfo(true, true);
			} else if (colorChoice == 2) {
				setNetworkInfo(true, false);
			} else {
				setNetworkInfo(true, true);
			}
			View.getInstance().displayChatMessage("系统", "连接服务器成功！");
		} else {
			javax.swing.JOptionPane.showMessageDialog(null,
					"连接服务器失败！请检查服务器是否启动", "连接错误",
					javax.swing.JOptionPane.ERROR_MESSAGE);
		}
	}

	public void restartOnlineGame() {
		if (!isOnlineMode) {
			restartGame();
			return;
		}

		if (!NetworkManager.getInstance().isConnected()) {
			View.getInstance().displayChatMessage("系统", "对方已经断开连接，无法重新开始");
			returnToMainMenu();
			return;
		}

		NetworkManager.getInstance().sendMessage("RESTART_REQUEST", "");
		View.getInstance().displayChatMessage("系统", "已向对手发送重新开始请求，等待对方同意");
		View.getInstance().showRestartRequest(false, true);
	}

	public void processRestartRequest() {
		if (!isOnlineMode)
			return;
		Music.getInstance().playClickSound();
		View.getInstance().displayChatMessage("系统", "对手请求重新开始游戏");
		View.getInstance().showRestartRequest(true, false);
	}

	public void acceptRestart() {
		if (!isOnlineMode)
			return;
		Music.getInstance().playClickSound();
		NetworkManager.getInstance().sendMessage("RESTART_ACCEPT", "");
		View.getInstance().displayChatMessage("系统", "已同意重新开始游戏");
		View.getInstance().hideRestartRequest();
		performRestart();
	}

	public void rejectRestart() {
		if (!isOnlineMode)
			return;
		Music.getInstance().playClickSound();
		NetworkManager.getInstance().sendMessage("RESTART_REJECT", "");
		View.getInstance().displayChatMessage("系统", "已拒绝对手重新开始请求");
		View.getInstance().hideRestartRequest();
		showGameEndDialogAfterReject();
	}

	public void processRestartAccept() {
		if (!isOnlineMode)
			return;
		Music.getInstance().playClickSound();
		View.getInstance().displayChatMessage("系统", "对手同意了重新开始请求");
		View.getInstance().hideRestartRequest();
		performRestart();
	}

	public void processRestartReject() {
		if (!isOnlineMode)
			return;
		Music.getInstance().playClickSound();
		View.getInstance().displayChatMessage("系统", "对手拒绝了重新开始请求");
		View.getInstance().hideRestartRequest();
		showGameEndDialogAfterReject();
	}

	private void showGameEndDialogAfterReject() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				int winner = Model.getInstance().whowin();
				View.getInstance().showGameEndDialog(winner);
			}
		});
	}

	// 执行重新开始（保持颜色和先后手不变）
	private void performRestart() {
		Model.getInstance().resetGame();
		currentcolor = Model.BLACK;
		flag = false;
		isMyTurn = (myColor == Model.BLACK);
		undoRequested = false;
		waitingUndoResponse = false;
		Music.getInstance().startBackgroundMusic();
		// 重置并启动倒计时
		ensureCountdownManager();
		countdownManager.stop();
		countdownManager.reset(Model.COUNTDOWN_TIME);
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.start();

		if (isOnlineMode) {
			if (myColor == Model.BLACK) {
				View.getInstance().displayChatMessage("系统", "游戏重新开始！你执黑先行");
			} else {
				View.getInstance().displayChatMessage("系统",
						"游戏重新开始！你执白后行，等待黑棋落子");
			}
		} else {
			if (isMyTurn) {
				countdownManager.start();
			}
		}
		// 刷新棋盘面板和倒计时显示
		View.getInstance().showGamePanel(); // 确保游戏面板在前端
		View.getInstance().updateBoard();
		View.getInstance().resetchat();
		View.getInstance().hideUndoRequest();
		updateCountdownDisplay();
	}

	public void startOnlineReview() {
		startReview();
	}

	public void processNetworkRestart() {
		if (!isOnlineMode)
			return;

		Model.getInstance().resetGame();
		currentcolor = Model.BLACK;
		flag = false;
		isMyTurn = (myColor == Model.BLACK);
		undoRequested = false;
		waitingUndoResponse = false;

		View.getInstance().showStartPanel();
		View.getInstance().resetchat();
		View.getInstance().hideUndoRequest();
		View.getInstance().displayChatMessage("系统", "对手请求重新开始游戏，已返回主菜单");
	}
	public int getMyColor() {
		return myColor;
	}

	public boolean isMyTurn() {
		return isMyTurn;
	}

	public boolean isOnlineMode() {
		return isOnlineMode;
	}

	// ========== 倒计时包装方法 ==========
	public void startCountdown() {
		ensureCountdownManager();
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.reset(Model.COUNTDOWN_TIME);
		countdownManager.start();
	}

	public void stopCountdown() {
		if (countdownManager != null)
			countdownManager.stop();
	}

	public void resetAndStartCountdown() {
		ensureCountdownManager();
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.reset(Model.COUNTDOWN_TIME);
		countdownManager.start();
	}

	private void updateCountdownDisplay() {
		ensureCountdownManager();
		int rem = countdownManager.getRemainingSeconds();
		int turn = countdownManager.getCurrentTurnColor();
		View.getInstance().updateCountdown(rem, turn);
	}

	// 由 CountdownManager 回调触发
	private void handleTimeoutFromCountdown(int timeoutColor) {
		if (flag || isReviewing)
			return;

		final int winner = -timeoutColor;
		flag = true;
		stopCountdown();

		if (isOnlineMode) {
			NetworkManager.getInstance().sendMessage("TIMEOUT",
					String.valueOf(winner));
			String timeoutMsg = (timeoutColor == myColor) ? "你超时了！对手获胜！"
					: "对手超时！你获胜！";
			View.getInstance().displayChatMessage("系统", timeoutMsg);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ArrayList<int[]> winningLine = Model.getInstance().getWinningLine();
					View.getInstance().setWinningLine(winningLine);
					View.getInstance().showGameEndDialog(winner);
				}
			});
		} else {
			String message = (timeoutColor == Model.BLACK) ? "黑棋超时！白棋获胜！"
					: "白棋超时！黑棋获胜！";
			View.getInstance().displayChatMessage("系统", message);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ArrayList<int[]> winningLine = Model.getInstance().getWinningLine();
					View.getInstance().setWinningLine(winningLine);
					View.getInstance().showGameEndDialog(winner);
				}
			});
		}

		View.getInstance().updateBoard();
	}

	// 新增： 带参的人机启动（在 Cont 类中）
	public void startAIGame(String difficulty, boolean playerIsBlack) {
		aiMode = true;
		isOnlineMode = false;
		flag = false;
		Music.getInstance().stopAllSounds();
		Music.getInstance().playClickSound();
		Music.getInstance().startBackgroundMusic();
		// 玩家先手或后手
		if (playerIsBlack) {
			// 玩家执黑（先手）
			currentcolor = Model.BLACK;
			aiColor = Model.WHITE;
		} else {
			// 玩家执白（后手），AI 执黑（先手）
			currentcolor = Model.BLACK; // 游戏默认黑先
			aiColor = Model.BLACK;
		}

		// DeepSeek API KEY：请替换成你自己的 key 或从配置读取
		String apiKey = "sk-20ec51e392844c8c81d6dcd01f1394f5";
		aiPlayer = new DeepSeekAI(apiKey, difficulty);

		Model.getInstance().resetGame();
		View.getInstance().showGamePanel();
		View.getInstance().updateBoard();

		ensureCountdownManager();
		countdownManager.setCurrentTurnColor(currentcolor);
		countdownManager.reset(Model.COUNTDOWN_TIME);
		countdownManager.start();

		// 如果 AI 是先手（即 aiColor == currentcolor），让 AI 立即落子
		if (aiColor == currentcolor && !flag) {
			// 启动一个线程去请求 AI 并落子（注意线程安全）
			new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (moveLock) {
						if (flag)
							return;

						int[][] board = Model.getInstance().getBoardCopy();
						int[] move = null;
						try {
							move = aiPlayer.computeAIMove(board, aiColor);
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						// 验证 AI 返回的坐标是否合法、空位；否则做兜底（找一个随机空位）
						if (move == null
								|| move.length < 2
								|| move[0] < 0
								|| move[0] >= Model.WIDTH
								|| move[1] < 0
								|| move[1] >= Model.WIDTH
								|| Model.getInstance().getchess(move[0],
										move[1]) != Model.SPACE) {
							// 兜底：找第一个空位，或随机空位
							int[] fallback = findFallbackEmptyCell();
							if (fallback != null)
								move = fallback;
						}

						if (move != null) {
							final int r = move[0];
							final int c = move[1];
							// 在 Swing 线程调用落子（复用已有 userputchess）
							javax.swing.SwingUtilities
									.invokeLater(new Runnable() {
										@Override
										public void run() {
											if (!flag) {
												userputchess(r, c);
											}
										}
									});
						}
					}
				}
			}).start();
		}
	}

	// 保留无参版本（向后兼容，默认 medium，玩家执黑）
	public void startAIGame() {
		startAIGame("medium", true);
	}

	// 辅助方法：在 Model 中查找空位（首个空位 - 也可以改成随机）
	private int[] findFallbackEmptyCell() {
		// 尝试随机化以避免总是同一点
		ArrayList<int[]> empties = new ArrayList<int[]>();
		for (int r = 0; r < Model.WIDTH; r++) {
			for (int c = 0; c < Model.WIDTH; c++) {
				if (Model.getInstance().getchess(r, c) == Model.SPACE) {
					empties.add(new int[] { r, c });
				}
			}
		}
		if (empties.isEmpty())
			return null;
		// 随机选择一个空位作为兜底
		Random rnd = new Random();
		return empties.get(rnd.nextInt(empties.size()));
	}

	public void setAIDifficulty(String difficulty) {
		if (difficulty == null)
			return;

		// 统一成内部使用的三种 key
		switch (difficulty.toLowerCase()) {
		case "simple":
			this.aiDifficulty = "easy";
			break;

		case "medium":
			this.aiDifficulty = "medium";
			break;

		case "hard":
			this.aiDifficulty = "hard";
			break;
		default:
			// 其它非法输入直接忽略
			break;
		}
	}

}
