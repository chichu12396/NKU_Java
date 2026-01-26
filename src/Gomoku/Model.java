package Gomoku;

import java.util.*;


public class Model {
	public static final int BLACK=1;
	public static final int WHITE=-1;
	public static final int SPACE=0;
	public static final int WIDTH=19;
	public static final int DRAW = 2;
	public static final int COUNTDOWN_TIME = 15; // 15秒倒计时
	private int moveCount = 0;
	private int lastRow,lastCol;
    private int[][]panel=new int[WIDTH][WIDTH];
    private ArrayList<int[]> moveHistory = new ArrayList<>(); // [row,col,color]用于悔棋和复盘
    public int currentReviewIndex = -1;//复盘使用
    private ArrayList<int[]> undoneMoves = new ArrayList<>(); // 存储被悔的棋
    private List<String> chatHistory = new ArrayList<>();//聊天使用
    public boolean putchess(int row,int col,int color){
    	if(row>=0&&row<WIDTH&&col>=0&&col<WIDTH&&panel[row][col]==SPACE){
    		panel[row][col]=color;
    		lastRow=row;
    		lastCol=col;
    		moveCount++;
    		moveHistory.add(new int[]{row, col, color}); // 记录落子位置
    		return true;
    	}
    	return false;
    }
	public int getchess(int row,int col){
		if(row>=0&&row<WIDTH && col>=0&& col<WIDTH){
			return panel[row][col];
		}
		return SPACE;
	}
	public int whowin() {
	    // 初始检查：如果还没有落子，直接返回空格
	    if (lastRow == -1 || lastCol == -1) {
	        return SPACE;
	    }
	    
	    // 获取最后一次落子的颜色(BLACK或WHITE)
	    int color = panel[lastRow][lastCol];
	    
	    // 定义四个检查方向：右、下、右下、左下
	    int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
	    // 检查每个方向是否有五连
	    for (int[] dir : directions) {
	        if (checkDirection(lastRow, lastCol, dir[0], dir[1], color) >= 5) {
	            return color; // 如果任一方向有五连，返回当前颜色表示胜利
	        }
	    }
	 // 检查平局
	    if (isBoardFull()) {
	        return DRAW; // 可以定义一个常量如DRAW=2表示平局
	    }
	    return SPACE; // 没有五连，游戏继续
	}
	//辅助函数
	public void discount(){moveCount--;}
	private boolean isBoardFull() {
		return moveCount >= WIDTH * WIDTH;
	}
	private int checkDirection(int row, int col, int rowStep, int colStep, int color) {
	    int count = 1; // 从当前落子点开始计数
	    
	    // 正向检查（沿给定方向）
	    for (int i = 1; i < 5; i++) {
	        int r = row + i * rowStep; // 计算新位置的行
	        int c = col + i * colStep; // 计算新位置的列
	        
	        // 边界检查+颜色检查
	        if (r < 0 || r >= WIDTH || c < 0 || c >= WIDTH || panel[r][c] != color) {
	            break; // 越界或颜色不同则停止
	        }
	        count++; // 相同颜色，计数增加
	    }
	    
	    // 反向检查（相反方向）
	    for (int i = 1; i < 5; i++) {
	        int r = row - i * rowStep;
	        int c = col - i * colStep;
	        
	        if (r < 0 || r >= WIDTH || c < 0 || c >= WIDTH || panel[r][c] != color) {
	            break;
	        }
	        count++;
	    }
	    
	    return count; // 返回该方向的总连续数
	}
	
	// 悔棋操作
    public boolean undoMove() {
        if (moveHistory.isEmpty()) return false;
        
        int[] lastMove = moveHistory.remove(moveHistory.size()-1);
        undoneMoves.add(lastMove); // 记录被悔的棋
        panel[lastMove[0]][lastMove[1]] = SPACE;
        return true;
    }
    //复盘操作
    // 获取纯净对局记录（排除悔棋步）
    public ArrayList<int[]> getPureGameRecord() {
        return new ArrayList<>(moveHistory); 
    }

    // 复盘控制方法
    public int[] nextReviewStep() {
        if (currentReviewIndex + 1 < moveHistory.size()) {
            return moveHistory.get(++currentReviewIndex);
        }
        return null;
    }

    public int[] prevReviewStep() {
        if (currentReviewIndex > 0) {
            return moveHistory.get(--currentReviewIndex);
        }
        return null;
    }

    public void resetReview() {
        currentReviewIndex = -1;
    }
    public boolean shouldRemoveAt(int row, int col) {
        for (int i = currentReviewIndex + 1; i < moveHistory.size(); i++) {
            int[] step = moveHistory.get(i);
            if (step[0] == row && step[1] == col) return true;
        }
        return false;
    }
    //重新开始
    public void resetGame() {
        // 清空棋盘
        for (int row = 0; row < WIDTH; row++) {
            for (int col = 0; col < WIDTH; col++) {
                panel[row][col] = SPACE;
            }
        }
        // 重置状态
        lastRow = -1;
        lastCol = -1;
        moveHistory.clear();
        moveCount = 0;
        chatHistory.clear();
    }
	//单例模式
	  private static Model instance=null;
	  private Model(){}
	  public static Model getInstance() {
		  if(instance==null){
			  instance =new Model();
		  }
		return instance;
	}
	
	public void addChatMessage(String sender, String message) {
		chatHistory.add(sender + ": " + message);
	}
	public int[][] getBoardCopy() {
		int[][] copy = new int[WIDTH][WIDTH];
	    for (int i = 0; i < WIDTH; i++) {
	        System.arraycopy(panel[i], 0, copy[i], 0, WIDTH);
	    }
	    return copy;
	}
	public ArrayList<int[]> getWinningLine() {
	    if (lastRow == -1 || lastCol == -1) {
	        return null;
	    }
	    
	    int color = panel[lastRow][lastCol];
	    int[][] directions = {{0,1}, {1,0}, {1,1}, {1,-1}};
	    
	    for (int[] dir : directions) {
	        ArrayList<int[]> line = checkWinningDirection(lastRow, lastCol, dir[0], dir[1], color);
	        if (line != null && line.size() >= 5) {
	            // 只返回前5个棋子（如果超过5个）
	            return new ArrayList<>(line.subList(0, 5));
	        }
	    }
	    return null;
	}

	// 检查特定方向上的连续棋子
	private ArrayList<int[]> checkWinningDirection(int row, int col, int rowStep, int colStep, int color) {
	    ArrayList<int[]> line = new ArrayList<>();
	    
	    // 向前检查
	    for (int i = 0; i < 5; i++) {
	        int r = row + i * rowStep;
	        int c = col + i * colStep;
	        
	        if (r < 0 || r >= WIDTH || c < 0 || c >= WIDTH || panel[r][c] != color) {
	            break;
	        }
	        line.add(new int[]{r, c, color});
	    }
	    
	    // 向后检查
	    for (int i = 1; i < 5; i++) {
	        int r = row - i * rowStep;
	        int c = col - i * colStep;
	        
	        if (r < 0 || r >= WIDTH || c < 0 || c >= WIDTH || panel[r][c] != color) {
	            break;
	        }
	        line.add(0, new int[]{r, c, color}); // 添加到开头
	    }
	    
	    return line.size() >= 5 ? line : null;
	}
}