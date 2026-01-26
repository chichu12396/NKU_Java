package Gomoku;

import javax.swing.*;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
@SuppressWarnings("serial")
public class View {
	private JFrame frame;
	private ArrayList<int[]> winningLine = new ArrayList<>(); // 存储获胜的五连棋子
	// 添加成员变量
	private JLabel countdownLabel;
	private JPanel countdownPanel;
	// 界面控制成员
	private JPanel modePanel;
	private CardLayout cardLayout;
	private JPanel containerPanel; // 主容器面板
	private JPanel startPanel; // 开始界面面板
	private JPanel boardPanel;// 棋盘面板
	// ===== 复盘前保存的获胜高亮 =====
	private ArrayList<int[]> savedWinningLine = new ArrayList<int[]>();
	private int cellSize; // 每个格子的像素大小
	private int marginX, marginY; // 棋盘边距
	private ArrayList<int[]> highlightMoves = new ArrayList<>(); // 高亮的棋步（用于复盘）
	private JButton undoButton;
	private JLabel statusLabel;
	private JPanel reviewPanel;// 复盘面板
	private JButton prevBtn, nextBtn, exitBtn;
	private JLabel stepLabel;
	// 聊天相关组件
	private JPanel chatPanel;
	private JTextArea chatArea;
	private JTextField chatInput;
	private JButton toggleChatBtn;
	private boolean isChatOpen = false;
	// 在GraphicalView类中添加组件
	private JPanel undoRequestPanel;
	private JLabel undoRequestLabel;
	private ImageButton acceptUndoBtn, rejectUndoBtn, cancelUndoBtn;
	// 在GraphicalView类中添加重新开始请求组件
	private JPanel restartRequestPanel;
	private JLabel restartRequestLabel;
	private ImageButton acceptRestartBtn, rejectRestartBtn, cancelRestartBtn;

	// 初始化重新开始请求面板
	private void initRestartRequestPanel() {
		restartRequestPanel = new JPanel(new FlowLayout()) {
			private Image bg = new ImageIcon("resource/bb.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		restartRequestPanel.setBorder(BorderFactory
				.createTitledBorder("重新开始请求"));
		restartRequestPanel.setBorder(BorderFactory.createEmptyBorder());
		restartRequestPanel.setVisible(false);

		restartRequestLabel = new JLabel("等待对方响应...");
		restartRequestLabel.setOpaque(false);
		acceptRestartBtn = new ImageButton("同意");
		rejectRestartBtn = new ImageButton("拒绝");
		cancelRestartBtn = new ImageButton("取消");

		acceptRestartBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().acceptRestart();
			}
		});

		rejectRestartBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().rejectRestart();
			}
		});

		cancelRestartBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().rejectRestart();
			}
		});

		restartRequestPanel.add(restartRequestLabel);
		restartRequestPanel.add(acceptRestartBtn);
		restartRequestPanel.add(rejectRestartBtn);
		restartRequestPanel.add(cancelRestartBtn);
	}

	// 显示重新开始请求
	public void showRestartRequest(boolean isReceiver, boolean showCancel) {
		if (isReceiver) {
			restartRequestLabel.setText("对手请求重新开始游戏");
			acceptRestartBtn.setVisible(true);
			rejectRestartBtn.setVisible(true);
			cancelRestartBtn.setVisible(false);
		} else {
			restartRequestLabel.setText("等待对方响应重新开始请求...");
			acceptRestartBtn.setVisible(false);
			rejectRestartBtn.setVisible(false);
			cancelRestartBtn.setVisible(showCancel);
		}
		restartRequestPanel.setVisible(true);
		frame.revalidate();
		frame.repaint();
	}

	// 隐藏重新开始请求
	public void hideRestartRequest() {
		restartRequestPanel.setVisible(false);
		frame.revalidate();
		frame.repaint();
	}

	// 初始化悔棋请求面板
	private void initUndoRequestPanel() {
		undoRequestPanel = new JPanel(new FlowLayout()) {
			private Image bg = new ImageIcon("resource/bb.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		undoRequestPanel.setBorder(BorderFactory.createTitledBorder("悔棋请求"));
		undoRequestPanel.setBorder(BorderFactory.createEmptyBorder());
		undoRequestPanel.setVisible(false);

		undoRequestLabel = new JLabel();

		acceptUndoBtn = new ImageButton("同意");
		rejectUndoBtn = new ImageButton("拒绝");
		cancelUndoBtn = new ImageButton("取消");

		acceptUndoBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().acceptUndo();
			}
		});

		rejectUndoBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().rejectUndo();
			}
		});

		cancelUndoBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().rejectUndo();
			}
		});

		undoRequestPanel.add(undoRequestLabel);
		undoRequestPanel.add(acceptUndoBtn);
		undoRequestPanel.add(rejectUndoBtn);
		undoRequestPanel.add(cancelUndoBtn);
	}

	// 显示悔棋请求
	public void showUndoRequest(boolean isReceiver, boolean showCancel) {
		if (isReceiver) {
			undoRequestLabel.setText("对手请求悔棋");
			acceptUndoBtn.setVisible(true);
			rejectUndoBtn.setVisible(true);
			cancelUndoBtn.setVisible(false);
		} else {
			undoRequestLabel.setText("等待对方响应...");
			acceptUndoBtn.setVisible(false);
			rejectUndoBtn.setVisible(false);
			cancelUndoBtn.setVisible(showCancel);
		}
		undoRequestPanel.setVisible(true);
		frame.revalidate();
		frame.repaint();
	}

	// 隐藏悔棋请求
	public void hideUndoRequest() {
		undoRequestPanel.setVisible(false);
		frame.revalidate();
		frame.repaint();
	}

	// 初始化复盘UI (JDK 1.7兼容版)
	public void initReviewUI() {
		savedWinningLine.clear();
		for (int[] move : winningLine) {
			savedWinningLine.add(Arrays.copyOf(move, move.length));
		}
		reviewPanel = new JPanel() {
			private Image bg = new ImageIcon("resource/bt.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		prevBtn = new ImageButton("上一步");
		nextBtn = new ImageButton("下一步");
		exitBtn = new ImageButton("退出复盘");
		stepLabel = new JLabel("", JLabel.CENTER);
		stepLabel.setForeground(Color.white);
		// 使用匿名内部类替代lambda
		prevBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().reviewStep(false);
			}
		});

		nextBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().reviewStep(true);
			}
		});

		exitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().exitReview();
			}
		});

		reviewPanel.add(prevBtn);
		reviewPanel.add(stepLabel);
		reviewPanel.add(nextBtn);
		reviewPanel.add(exitBtn);

		// 添加到主窗口的北边
		frame.add(reviewPanel, BorderLayout.NORTH);
		frame.revalidate();
		frame.repaint();
		highlightMoves.clear();
	}

	public void displayReviewStep(int row, int col, int color, int stepNum,
			boolean isForward) {
		// 更新高亮记录
		if (isForward) {
			// 前进：添加当前步到高亮
			highlightMoves.add(new int[] { row, col, color });
		} else {
			// 后退：移除最后一步
			if (!highlightMoves.isEmpty()) {
				highlightMoves.remove(highlightMoves.size() - 1);
			}
		}

		// 更新状态显示
		stepLabel.setText(String.format("第%d步: %s", stepNum,
				color == Model.BLACK ? "黑棋" : "白棋"));

		// 重绘棋盘
		updateBoard();
	}

	// 隐藏复盘UI (无需修改，保持原样)
	public void hideReviewUI() {
		if (reviewPanel != null) {
			frame.remove(reviewPanel);
			frame.revalidate();
			frame.repaint();
		}
		// 清空高亮记录
		highlightMoves.clear();
		// ===== 恢复获胜高亮 =====
		winningLine.clear();
		for (int[] move : savedWinningLine) {
			winningLine.add(Arrays.copyOf(move, move.length));
		}
		// 恢复棋盘显示
		updateBoard();
	}

	private View() {
		frame = new JFrame("五子棋游戏");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 800);

		// 初始化多面板容器
		cardLayout = new CardLayout();
		containerPanel = new JPanel(cardLayout);
		frame.add(containerPanel);

		// 初始化各界面
		initStartPanel();
		initModePanel();
		initGamePanel();

		// 默认显示开始界面
		showStartPanel();
		frame.setVisible(true);
	}

	private static View instance = null;

	public static View getInstance() {
		if (instance == null) {
			instance = new View();
		}
		return instance;
	}

	// 初始化开始界面
	private void initStartPanel() {

		startPanel = new JPanel() {

			private Image bg = new ImageIcon("resource/start.jpg").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
			}
		};

		startPanel.setLayout(null);

		// ===== 背景图原始尺寸 =====
		final Image bgImage = new ImageIcon("resource/start.jpg").getImage();
		final int bgWidth = bgImage.getWidth(null);
		final int bgHeight = bgImage.getHeight(null);

		// ===== 你现在的设计尺寸（基于原图）=====
		final int designBtnW = 480;
		final int designBtnH = 190;
		final int designMarginW = 81;
		final int designMarginH = 16;

		// 原图中按钮的位置（右下角）
		final int designBtnX = bgWidth - designBtnW - designMarginW;
		final int designBtnY = bgHeight - designBtnH - designMarginH;

		final JButton startBtn = new JButton();
		startBtn.setContentAreaFilled(false);
		startBtn.setFocusPainted(false);
		startBtn.setFont(new Font("微软雅黑", Font.BOLD, 24));

		startBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playGameStartSound();
				cardLayout.show(containerPanel, "Mode");
			}
		});

		startPanel.add(startBtn);

		// ===== 核心：面板缩放时，按钮按比例变化 =====
		startPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {

				int panelW = startPanel.getWidth();
				int panelH = startPanel.getHeight();

				if (panelW <= 0 || panelH <= 0)
					return;

				double scaleX = panelW * 1.0 / bgWidth;
				double scaleY = panelH * 1.0 / bgHeight;

				int x = (int) (designBtnX * scaleX);
				int y = (int) (designBtnY * scaleY);
				int w = (int) (designBtnW * scaleX);
				int h = (int) (designBtnH * scaleY);

				startBtn.setBounds(x, y, w, h);
			}
		});

		containerPanel.add(startPanel, "Start");
	}

	// 新的模式选择界面
	private void initModePanel() {
		modePanel = new JPanel(new GridBagLayout()) {
			private Image bg = new ImageIcon("resource/module.jpg").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
		btnPanel.setPreferredSize(new Dimension(400, 500));
		btnPanel.setOpaque(false);

		// 单机
		ImageButton localBtn = new ImageButton("单机游戏");
		localBtn.setFont(new Font("微软雅黑", Font.BOLD, 50));
		localBtn.setPreferredSize(new Dimension(220, 70));
		localBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				showGamePanel();
				Cont.getInstance().startNewGame(); // 原逻辑
			}
		});

		// 人机
		JButton aiBtn = new ImageButton("人机对战");
		aiBtn.setFont(new Font("微软雅黑", Font.BOLD, 50));
		aiBtn.setPreferredSize(new Dimension(220, 70));
		aiBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				showAISetupDialog();
			}

		});

		// 联网
		JButton onlineBtn = new ImageButton("联网对战");
		onlineBtn.setFont(new Font("微软雅黑", Font.BOLD, 50));
		onlineBtn.setPreferredSize(new Dimension(220, 70));
		onlineBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				showNetworkDialog();
			}
		});

		localBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		aiBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		onlineBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		btnPanel.add(localBtn);
		btnPanel.add(Box.createVerticalStrut(20));
		btnPanel.add(aiBtn);
		btnPanel.add(Box.createVerticalStrut(20));
		btnPanel.add(onlineBtn);

		modePanel.add(btnPanel);
		containerPanel.add(modePanel, "Mode");
	}

	
	public void showAISetupDialog() {
		// 创建自定义面板
		final JDialog dialog = new JDialog(frame, "人机对战设置", true);
		JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8)) {
			private Image bg = new ImageIcon("resource/b1.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// 创建标签和下拉框
		JLabel difficultyLabel = new JLabel("AI 难度:");
		difficultyLabel.setForeground(Color.WHITE);
		difficultyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));
		JLabel colorLabel = new JLabel("你的执子:");
		colorLabel.setForeground(Color.WHITE);
		colorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 20));

		final JComboBox<String> difficultyCombo = new JComboBox<>(new String[] {
				"低级", "中级", "高级" });
		difficultyCombo.setOpaque(false);
		difficultyCombo.setBorder(BorderFactory.createEmptyBorder());
		final JComboBox<String> colorCombo = new JComboBox<>(new String[] {
				"我执黑（先手）", "我执白（后手）" });
		colorCombo.setOpaque(false);
		colorCombo.setBorder(BorderFactory.createEmptyBorder());

		panel.add(difficultyLabel);
		panel.add(difficultyCombo);
		panel.add(colorLabel);
		panel.add(colorCombo);

		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.setOpaque(false);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));

		JButton okBtn = new ImageButton("确定");
		JButton cancelBtn = new ImageButton("取消");

		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);

		panel.add(buttonPanel);
		panel.setPreferredSize(new Dimension(450, 200));
		dialog.setContentPane(panel);

		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(frame);

		// 按钮事件
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String diff = (String) difficultyCombo.getSelectedItem();
				boolean playerIsBlack = colorCombo.getSelectedIndex() == 0;

				String difficultyKey = "simple";
				if ("中级".equals(diff))
					difficultyKey = "medium";
				else if ("高级".equals(diff))
					difficultyKey = "hard";

				Cont.getInstance().startAIGame(difficultyKey, playerIsBlack);
				dialog.dispose();
				showGamePanel();
			}
		});

		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		dialog.setVisible(true);
	}

	private void showNetworkDialog() {
		// 创建自定义对话框
		final JDialog dialog = new JDialog(frame, "网络连接设置", true);

		// 背景面板
		JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10)) {
			private Image bg = new ImageIcon("resource/b1.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		panel.setBorder(BorderFactory.createEmptyBorder());

		// 标签
		JLabel ipLabel = new JLabel("服务器IP:");
		JLabel portLabel = new JLabel("端口:");
		JLabel colorLabel = new JLabel("选择颜色:");
		JLabel infoLabel = new JLabel(""); // 占位，可选

		JLabel[] labels = { ipLabel, portLabel, colorLabel, infoLabel };
		for (JLabel l : labels) {
			l.setForeground(Color.WHITE);
			l.setFont(new Font("微软雅黑", Font.PLAIN, 20));
		}

		// 文本框
		final JTextField ipField = new JTextField("127.0.0.1");
		final JTextField portField = new JTextField("12345");

		// 下拉框
		final JComboBox<String> colorCombo = new JComboBox<>(new String[] {
				"自动分配", "黑棋", "白棋" });
		colorCombo.setOpaque(false);
		colorCombo.setBorder(BorderFactory.createEmptyBorder());

		// 添加组件
		panel.add(ipLabel);
		panel.add(ipField);
		panel.add(portLabel);
		panel.add(portField);
		panel.add(colorLabel);
		panel.add(colorCombo);

		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.setOpaque(false);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder());
		JButton okBtn = new ImageButton("确定");
		JButton cancelBtn = new ImageButton("取消");
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);
		//JButton[] buttons = { okBtn, cancelBtn };
		panel.add(buttonPanel);
		panel.setPreferredSize(new Dimension(450, 250));
		// 对话框设置
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(frame);

		// 按钮事件
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String ip = ipField.getText();
				int port = Integer.parseInt(portField.getText());
				int colorChoice = colorCombo.getSelectedIndex();

				dialog.dispose();
				// 显示连接中提示
				// JOptionPane.showMessageDialog(frame, "正在连接服务器...", "连接中",
				// JOptionPane.INFORMATION_MESSAGE);

				// 调用网络连接
				Cont.getInstance().connectToServer(ip, port, colorChoice);
			}
		});

		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		dialog.setVisible(true);
	}

	// 界面切换方法
	public void showStartPanel() {
		cardLayout.show(containerPanel, "Start");
		// 确保棋盘被清空
		highlightMoves.clear();
		// 重置状态标签
		statusLabel.setText("黑棋回合");
		// 隐藏倒计时
		if (countdownPanel != null) {
			countdownPanel.setVisible(false);
		}
	}

	public void showGamePanel() {
		cardLayout.show(containerPanel, "Game");
		// 显示倒计时
		if (countdownPanel != null) {
			countdownPanel.setVisible(true);
		}
	}

	// 初始化游戏界面
	private void initGamePanel() {
		JPanel gamePanel = new JPanel(new BorderLayout()) {
			private Image bg = new ImageIcon("resource/b2.png").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};

		// 创建顶部面板，包含倒计时和请求面板
		JPanel topPanel = new JPanel(new BorderLayout());

		topPanel.setOpaque(false);
		// 创建倒计时面板（左上角）
		countdownPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawCountdown(g);
			}
		};
		countdownPanel.setPreferredSize(new Dimension(100, 100));
		countdownPanel.setOpaque(false);

		countdownLabel = new JLabel("15", JLabel.CENTER);
		countdownLabel.setFont(new Font("Arial", Font.BOLD, 24));
		countdownLabel.setForeground(Color.RED);
		countdownPanel.setLayout(new GridBagLayout());
		countdownPanel.add(countdownLabel);

		topPanel.add(countdownPanel, BorderLayout.WEST);

		// 创建北方面板容器
		JPanel northPanel = new JPanel();
		northPanel.setOpaque(false);
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

		// 初始化悔棋请求面板
		initUndoRequestPanel();
		northPanel.add(undoRequestPanel);

		// 初始化重新开始请求面板
		initRestartRequestPanel();
		northPanel.add(restartRequestPanel);

		topPanel.add(northPanel, BorderLayout.CENTER);

		// 创建自定义绘制的棋盘面板
		boardPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g.create();
		        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		                RenderingHints.VALUE_ANTIALIAS_ON);

		        // ===== 乳白色半透明背景 =====
		        Color milkyWhite = new Color(255, 255, 240, 100); // 最后一个是透明度
		        g2d.setColor(milkyWhite);
		        g2d.fillRoundRect(
		                10, 10,
		                getWidth() - 20,
		                getHeight() - 20,
		                30, 30
		        );

		        g2d.dispose();

				drawBoard(g);
			}
		};
		boardPanel.setPreferredSize(new Dimension(800, 600));
		boardPanel.setOpaque(false);
		// 添加鼠标监听器
		boardPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int[] pos = convertPixelToBoard(e.getX(), e.getY());
				if (pos != null) {
					Cont.getInstance().userputchess(pos[0], pos[1]);
				}
			}
		});

		JPanel controlPanel = new JPanel();
		controlPanel.setOpaque(false);
		undoButton = new ImageButton("悔棋");
		undoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				Cont.getInstance().undo();
			}
		});

		statusLabel = new JLabel("黑棋回合", JLabel.CENTER);
		statusLabel.setForeground(Color.yellow);
		controlPanel.add(undoButton);
		controlPanel.add(statusLabel);

		gamePanel.add(topPanel, BorderLayout.NORTH);
		gamePanel.add(boardPanel, BorderLayout.CENTER);
		gamePanel.add(controlPanel, BorderLayout.SOUTH);

		initChatComponents();
		gamePanel.add(createChatContainer(), BorderLayout.EAST);
		containerPanel.add(gamePanel, "Game");
	}

	// 绘制倒计时圆形
	private void drawCountdown(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		int diameter = 80;
		int x = (countdownPanel.getWidth() - diameter) / 2;
		int y = (countdownPanel.getHeight() - diameter) / 2;

		// 绘制外圆
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(3));
		g2d.drawOval(x, y, diameter, diameter);

		// 根据剩余时间设置颜色
		int time = Integer.parseInt(countdownLabel.getText());
		if (time > 10) {
			g2d.setColor(Color.GREEN);
		} else if (time > 5) {
			g2d.setColor(Color.ORANGE);
		} else if (time == -1) {
			g2d.setColor(Color.BLUE);
		} else {
			g2d.setColor(Color.RED);
		}

		// 填充内圆
		g2d.fillOval(x + 3, y + 3, diameter - 5, diameter - 5);
	}

	// 更新倒计时显示
	public void updateCountdown(int time, int currentPlayer) {
		countdownLabel.setText(String.valueOf(time));
		// time < 0 表示：暂停、未开始、等待悔棋、倒计时停止
		if (time < 0) {
			// countdownLabel.setText(""); // 不显示数字
			countdownLabel.setForeground(Color.blue); // 或者默认颜色
			countdownPanel.repaint();
			return;
		}

		// 根据当前玩家设置标签颜色
		if (currentPlayer == Model.BLACK) {
			countdownLabel.setForeground(Color.BLACK);
		} else {
			countdownLabel.setForeground(Color.WHITE);
		}

		// 重绘倒计时面板
		countdownPanel.repaint();
	}

	// 绘制棋盘
	private void drawBoard(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		int boardSize = Model.WIDTH;
		int panelWidth = boardPanel.getWidth();
		int panelHeight = boardPanel.getHeight();

		// 计算格子大小
		cellSize = Math.min(panelWidth / (boardSize + 1), panelHeight
				/ (boardSize + 1));

		// 计算边距，使棋盘居中
		marginX = (panelWidth - (boardSize - 1) * cellSize) / 2;
		marginY = (panelHeight - (boardSize - 1) * cellSize) / 2;
		// ===== 绘制棋盘外框（加粗黑框）=====
		int boardPixelSize = (boardSize - 1) * cellSize;

		// 外扩像素（让黑框包住网格）
		int padding = cellSize / 4;

		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(5)); // 线宽

		g2d.drawRect(
		        marginX - padding,
		        marginY - padding,
		        boardPixelSize + padding * 2,
		        boardPixelSize + padding * 2
		);
		// 绘制网格线
		g2d.setStroke(new BasicStroke(1)); 
		g2d.setColor(Color.BLACK);
		for (int i = 0; i < boardSize; i++) {
			// 横线
			g2d.drawLine(marginX, marginY + i * cellSize, marginX
					+ (boardSize - 1) * cellSize, marginY + i * cellSize);
			// 竖线
			g2d.drawLine(marginX + i * cellSize, marginY, marginX + i
					* cellSize, marginY + (boardSize - 1) * cellSize);
		}

		// 绘制星位（天元和四个角星）
		drawStarPoints(g2d, boardSize);

		// 绘制棋子
		for (int row = 0; row < boardSize; row++) {
			for (int col = 0; col < boardSize; col++) {
				int color = Model.getInstance().getchess(row, col);
				if (color != Model.SPACE) {
					drawChessPiece(g2d, row, col, color);
				}
			}
		}

		// 绘制复盘高亮
		for (int[] move : highlightMoves) {
			drawHighlightedChessPiece(g2d, move[0], move[1], move[2]);
		}
		// 绘制获胜线高亮
		for (int[] move : winningLine) {
			drawWinningHighlight(g2d, move[0], move[1], move[2]);
		}
	}

	private void drawWinningHighlight(Graphics2D g2d, int row, int col,
			int color) {
		int x = marginX + col * cellSize;
		int y = marginY + row * cellSize;
		int radius = cellSize / 2;

		// 绘制更粗的红色圆环
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		g2d.drawOval(x - radius - 2, y - radius - 2, (radius + 1) * 2,
				(radius + 1) * 2);

		// 再次绘制，让圆圈更明显
		g2d.drawOval(x - radius - 1, y - radius - 1, radius * 2, radius * 2);

		// 绘制棋子（确保棋子在上面）
		drawChessPiece(g2d, row, col, color);
	}

	public void clearWinningLine() {
		winningLine.clear();
		updateBoard();
	}

	// 设置获胜线并高亮显示
	public void setWinningLine(ArrayList<int[]> winningMoves) {
		winningLine.clear();
		if (winningMoves != null) {
			winningLine.addAll(winningMoves);
		}
		updateBoard();
	}

	// 绘制棋子
	private void drawChessPiece(Graphics2D g2d, int row, int col, int color) {
	    int x = marginX + col * cellSize;
	    int y = marginY + row * cellSize;
	    int radius = cellSize / 2 - 2;

	    // === 立体渐变棋子 ===
	    RadialGradientPaint paint;

	    if (color == Model.BLACK) {
	        // 黑棋：深灰 → 黑
	        paint = new RadialGradientPaint(
	                new Point(x - radius / 3, y - radius / 3), // 高光偏左上
	                radius,
	                new float[]{0.0f, 1.0f},
	                new Color[]{
	                        new Color(80, 80, 80),
	                        new Color(10, 10, 10)
	                }
	        );
	    } else {
	        // 白棋：亮白 → 米白
	        paint = new RadialGradientPaint(
	                new Point(x - radius / 3, y - radius / 3),
	                radius,
	                new float[]{0.0f, 1.0f},
	                new Color[]{
	                        Color.WHITE,
	                        new Color(220, 220, 220)
	                }
	        );
	    }

	    Paint oldPaint = g2d.getPaint();
	    g2d.setPaint(paint);
	    g2d.fillOval(x - radius, y - radius, radius * 2, radius * 2);
	    g2d.setPaint(oldPaint);
	}


	// 绘制高亮棋子（用于复盘）
	private void drawHighlightedChessPiece(Graphics2D g2d, int row, int col,
			int color) {
		int x = marginX + col * cellSize;
		int y = marginY + row * cellSize;
		int radius = cellSize / 2 - 2;

		// 绘制高亮圆环
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(2));
		g2d.drawOval(x - radius - 1, y - radius - 1, (radius + 1) * 2,
				(radius + 1) * 2);

		// 绘制棋子
		drawChessPiece(g2d, row, col, color);
	}

	// 绘制星位
	private void drawStarPoints(Graphics2D g2d, int boardSize) {
		int dotSize = 8;
		int[] starPoints = { 3, 9, 15 }; // 19路棋盘的星位：3、9、15

		for (int row : starPoints) {
			for (int col : starPoints) {
				int x = marginX + col * cellSize;
				int y = marginY + row * cellSize;
				g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
			}
		}
	}

	// 将鼠标像素坐标转换为棋盘坐标
	private int[] convertPixelToBoard(int mouseX, int mouseY) {
		int boardSize = Model.WIDTH;

		// 计算最近的交叉点
		float col = (float) (mouseX - marginX) / cellSize;
		float row = (float) (mouseY - marginY) / cellSize;

		int nearestCol = Math.round(col);
		int nearestRow = Math.round(row);

		// 检查是否在有效范围内
		if (nearestRow >= 0 && nearestRow < boardSize && nearestCol >= 0
				&& nearestCol < boardSize) {

			// 计算精确的交叉点坐标
			int exactX = marginX + nearestCol * cellSize;
			int exactY = marginY + nearestRow * cellSize;
			int tolerance = cellSize / 3; // 点击容差为格子大小的1/3

			// 检查是否在交叉点的有效点击范围内
			if (Math.abs(mouseX - exactX) <= tolerance
					&& Math.abs(mouseY - exactY) <= tolerance) {
				return new int[] { nearestRow, nearestCol };
			}
		}
		return null;
	}

	// 聊天界面
	private JPanel createChatContainer() {
		JPanel container = new JPanel(new BorderLayout());
		container.setOpaque(false);
		container.add(toggleChatBtn, BorderLayout.NORTH);
		container.add(chatPanel, BorderLayout.CENTER);
		return container;
	}

	private void initChatComponents() {
		// 聊天区域
		chatArea = new JTextArea(10, 20);
		chatArea.setEditable(false);
		chatArea.setOpaque(false);
		chatArea.setBackground(new Color(0, 0, 0, 0));
		chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 16)); // 第三个参数是字体大小
		chatArea.setForeground(Color.WHITE); // 白色字体
		chatArea.setBorder(BorderFactory.createEmptyBorder());
		JScrollPane chatScroll = new JScrollPane(chatArea);
		chatScroll.setOpaque(false);
		chatScroll.setBorder(new LineBorder(Color.BLACK, 2, true));
		chatScroll.getViewport().setOpaque(false);
		// 输入框
		chatInput = new JTextField();
		chatInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendChatMessage();
			}
		});
		chatInput.setFont(new Font("微软雅黑", Font.PLAIN, 16)); // 第三个参数是字体大小
		chatInput.setForeground(Color.WHITE); // 白色字体
		chatInput.setCaretColor(Color.WHITE); // 白色光标
		chatInput.setBorder(new LineBorder(Color.BLACK, 2, true));
		chatInput.setOpaque(false);
		// 发送按钮
		JButton sendBtn = new ImageButton("发送");
		sendBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				sendChatMessage();
			}
		});

		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.setOpaque(false);
		inputPanel.setBorder(BorderFactory.createEmptyBorder());
		inputPanel.add(chatInput, BorderLayout.CENTER);
		inputPanel.add(sendBtn, BorderLayout.EAST);
		// 折叠按钮
		toggleChatBtn = new ImageButton(">");
		toggleChatBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				toggleChat();
			}
		});

		chatPanel = new JPanel(new BorderLayout());
		chatPanel.setOpaque(false);
		chatPanel.add(chatScroll, BorderLayout.CENTER);
		chatPanel.add(inputPanel, BorderLayout.SOUTH);
		chatPanel.setVisible(false);
	}

	private void toggleChat() {
		isChatOpen = !isChatOpen;
		chatPanel.setVisible(isChatOpen);
		toggleChatBtn.setText(isChatOpen ? ">" : "<");
		frame.revalidate();
	}

	private void sendChatMessage() {
		String message = chatInput.getText();
		if (!message.isEmpty()) {
			Cont.getInstance().sendChatMessage(message);
			chatInput.setText("");
		}
	}

	public void displayChatMessage(String sender, String message) {
		chatArea.append(sender + ": " + message + "\n");
		chatArea.setCaretPosition(chatArea.getDocument().getLength());
	}

	public void resetchat() {
		chatArea.setText(""); // 清空聊天文本区域
	}

	// 显示游戏结束弹窗
	public void showGameEndDialog(final int winner) {
		// 确保在 Swing 事件线程中执行
		if (!java.awt.EventQueue.isDispatchThread()) {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					showGameEndDialog(winner);
				}
			});
			return;
		}

		// 停止倒计时显示
		if (countdownPanel != null)
			countdownPanel.setVisible(false);

		// 隐藏可能干扰的面板
		if (restartRequestPanel.isVisible())
			restartRequestPanel.setVisible(false);
		if (undoRequestPanel.isVisible())
			undoRequestPanel.setVisible(false);

		// 生成消息
		String message;
		if (!Cont.getInstance().isOnlineMode()) {
			if (winner == Model.BLACK)
				message = "黑棋获胜！";
			else if (winner == Model.WHITE)
				message = "白棋获胜！";
			else
				message = "平局！";
		} else {
			message = (winner == Cont.getInstance().getMyColor() ? "恭喜你获胜！"
					: "对手获胜！");
		}

		// 创建自定义对话框面板
		final JDialog dialog = new JDialog(frame, "游戏结束", true); // final
																	// 保证匿名类可访问
		JPanel panel = new JPanel() {
			private Image bg = new ImageIcon("resource/end.jpg").getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 拉伸填满
			}
		};
		
		// panel.setBackground(new Color(30, 30, 30));
		panel.setLayout(new BorderLayout(0, 20));
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// 消息标签
		JLabel label = new JLabel(message, SwingConstants.CENTER);
		label.setForeground(Color.black);
		label.setFont(new Font("微软雅黑", Font.BOLD, 25));
		label.setOpaque(false);
		panel.add(label, BorderLayout.CENTER);

		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.setOpaque(false);
		// buttonPanel.setBackground(new Color(30, 30, 30));
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 0));

		JButton restartBtn = new ImageButton("重新开始");
		JButton reviewBtn = new ImageButton("复盘");
		JButton menuBtn = new ImageButton("返回主菜单");

		buttonPanel.add(restartBtn);
		buttonPanel.add(reviewBtn);
		buttonPanel.add(menuBtn);

		panel.add(buttonPanel, BorderLayout.SOUTH);

		dialog.setContentPane(panel);
		dialog.setPreferredSize(new Dimension(500, 500));
		dialog.pack();
		dialog.setLocationRelativeTo(frame);

		// 按钮事件 - 重新开始
		restartBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				clearWinningLine();
				dialog.dispose();
				if (Cont.getInstance().isOnlineMode()) {
					Cont.getInstance().restartOnlineGame();
				} else {
					Cont.getInstance().restartGame();
				}
			}
		});

		// 按钮事件 - 复盘
		reviewBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				dialog.dispose();
				Cont.getInstance().startReview();
				clearWinningLine();
			}
		});

		// 按钮事件 - 返回主菜单
		menuBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Music.getInstance().playClickSound();
				clearWinningLine();
				dialog.dispose();
				Cont.getInstance().returnToMainMenu();
			}
		});

		// 显示弹窗
		dialog.setVisible(true);
	}

	public void updateBoard() {
		if (boardPanel != null) {
			boardPanel.repaint(); // 触发重绘
		}
		// 只更新状态标签，不触发弹窗
		if (Cont.getInstance().flag) {
			int result = Model.getInstance().whowin();
			if (result == Model.BLACK) {
				statusLabel.setText("黑棋胜利！");
			} else if (result == Model.WHITE) {
				statusLabel.setText("白棋胜利！");
			} else if (result == Model.DRAW) {
				statusLabel.setText("平局！");
			}
		} else {
			String player = (Cont.getInstance().currentcolor == Model.BLACK) ? "黑棋"
					: "白棋";
			statusLabel.setText(player + "回合");
		}
	}
}