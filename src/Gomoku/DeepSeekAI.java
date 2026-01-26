package Gomoku;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DeepSeekAI {

    private DeepSeekClient client;
    private String difficulty;   // simple / medium / hard
    private int maxRetries = 2;
    private Random random = new Random();

    public DeepSeekAI(String apiKey, String difficulty) {
        this.client = new DeepSeekClient(apiKey);
        this.difficulty = difficulty;
    }
 
    public int[] computeAIMove(int[][] board, int color) {

        List<int[]> moves = getCandidateMoves(board);
        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        // 检查自己连五/阻挡对手连四/潜在威胁
        int[] immediate = checkImmediateThreats(board, moves, color);
        if (immediate != null) return immediate;

        if ("hard".equalsIgnoreCase(difficulty)) {
            return runMCTS(board, color);
        } else if ("medium".equalsIgnoreCase(difficulty)) {
            return callDeepSeek(board, color, "hard");
        } else if ("simple".equalsIgnoreCase(difficulty)) {
            return chooseSimpleMove(moves);
        }

        return chooseRandomEmpty(board);
    }

    // ==========================
    // 公共逻辑：检查直接威胁
    // ==========================
    private int[] checkImmediateThreats(int[][] board, List<int[]> moves, int color) {
        // 1. 自己能直接赢
        for (int[] m : moves) {
            if (canWin(board, m, color)) return m;
        }

        // 2. 对手可能连四 → 必堵
        List<int[]> oppCritical = new ArrayList<>();
        for (int[] m : moves) {
            if (canWin(board, m, -color)) oppCritical.add(m);
        }
        if (!oppCritical.isEmpty()) return chooseHighestThreat(board, oppCritical, -color);

        // 3. 对手活三/潜在连四 → 提前堵
        List<int[]> oppThreat = new ArrayList<>();
        for (int[] m : moves) {
            if (evaluateThreat(board, m, -color) > 0) oppThreat.add(m);
        }
        if (!oppThreat.isEmpty()) return chooseHighestThreat(board, oppThreat, -color);

        return null;
    }

    // ==========================
    // 简单难度落子
    // ==========================
    private int[] chooseSimpleMove(List<int[]> moves) {
        // 靠近中心优先
        for (int i = 0; i < moves.size() - 1; i++) {
            for (int j = i + 1; j < moves.size(); j++) {
                if (evaluateMove(moves.get(j)) > evaluateMove(moves.get(i))) {
                    int[] tmp = moves.get(i);
                    moves.set(i, moves.get(j));
                    moves.set(j, tmp);
                }
            }
        }
        int limit = Math.min(5, moves.size());
        return moves.get(random.nextInt(limit));
    }

    // ==========================
    // MCTS 调用
    // ==========================
    private int[] runMCTS(int[][] board, int color) {
        MCTS mcts = new MCTS(color);
        int[] action = mcts.search(board, 1200, 2000);
        if (action != null && isValid(board, action[0], action[1])) return action;
        return chooseRandomEmpty(board);
    }

    // ==========================
    // 调用 DeepSeek
    // ==========================
    private int[] callDeepSeek(int[][] board, int color, String level) {
        String prompt = buildPrompt(board, color, level);
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                JSONObject response = client.queryAI(prompt);
                String content = extractContentFromResponse(response);
                if (content == null) { attempt++; continue; }

                content = cleanJSON(content);
                String jsonText = extractFirstJsonObject(content);
                if (jsonText == null) { attempt++; continue; }

                JSONObject obj = new JSONObject(jsonText);
                int r = obj.getInt("row");
                int c = obj.getInt("col");

                if (isValid(board, r, c)) return new int[]{r, c};

            } catch (Exception e) {
                e.printStackTrace();
            }

            attempt++;
            try { Thread.sleep(100); } catch (Exception ignore) {}
        }

        return chooseRandomEmpty(board);
    }

    // ==========================
    // 助手方法
    // ==========================
    private boolean isValid(int[][] board, int row, int col) {
        return row >= 0 && row < Model.WIDTH &&
               col >= 0 && col < Model.WIDTH &&
               board[row][col] == Model.SPACE;
    }

    private int[] chooseRandomEmpty(int[][] board) {
        List<int[]> empty = getCandidateMoves(board);
        if (empty.isEmpty()) return new int[]{0, 0};
        return empty.get(random.nextInt(empty.size()));
    }

    private List<int[]> getCandidateMoves(int[][] board) {
        List<int[]> moves = new ArrayList<>();
        int N = board.length;
        int range = 2;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (board[r][c] != 0) continue;
                boolean near = false;
                for (int dr=-range; dr<=range && !near; dr++){
                    for (int dc=-range; dc<=range && !near; dc++){
                        int nr = r+dr, nc=c+dc;
                        if (nr>=0 && nr<N && nc>=0 && nc<N && board[nr][nc]!=0) near=true;
                    }
                }
                if (near) moves.add(new int[]{r,c});
            }
        }
        if (moves.isEmpty()) {
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    if (board[r][c]==0) moves.add(new int[]{r,c});
        }
        return moves;
    }

    private boolean canWin(int[][] board, int[] move, int color) {
        int N = board.length;
        int r = move[0], c = move[1];
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int cnt = 1;
            for (int k=1;k<5;k++){
                int nr = r+d[0]*k, nc=c+d[1]*k;
                if(nr<0||nr>=N||nc<0||nc>=N||board[nr][nc]!=color) break;
                cnt++;
            }
            for (int k=1;k<5;k++){
                int nr = r-d[0]*k, nc=c-d[1]*k;
                if(nr<0||nr>=N||nc<0||nc>=N||board[nr][nc]!=color) break;
                cnt++;
            }
            if(cnt>=5) return true;
        }
        return false;
    }

    private int evaluateThreat(int[][] board, int[] move, int oppColor) {
        int threat = 0;
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int count=0;
            for(int k=1;k<5;k++){
                int r=move[0]+d[0]*k, c=move[1]+d[1]*k;
                if(r<0||r>=board.length||c<0||c>=board.length) break;
                if(board[r][c]==oppColor) count++; else break;
            }
            for(int k=1;k<5;k++){
                int r=move[0]-d[0]*k, c=move[1]-d[1]*k;
                if(r<0||r>=board.length||c<0||c>=board.length) break;
                if(board[r][c]==oppColor) count++; else break;
            }
            if(count>=3) threat += (count==3?1:3);
        }
        return threat;
    }

    private int[] chooseHighestThreat(int[][] board, List<int[]> moves, int oppColor){
        int max=-1;
        int[] best = moves.get(0);
        for(int[] m:moves){
            int t = evaluateThreat(board,m,oppColor);
            if(t>max){ max=t; best=m; }
        }
        return best;
    }
    //从回复里提取位置
    private String extractContentFromResponse(JSONObject response) {
        try {
            if (response.has("choices")) {
                org.json.JSONArray arr = response.getJSONArray("choices");
                if (arr.length() == 0) return null;
                JSONObject c0 = arr.getJSONObject(0);
                if (c0.has("message")) {
                    JSONObject msg = c0.getJSONObject("message");
                    if (msg.has("content")) return msg.getString("content");
                }
                if (c0.has("content")) return c0.getString("content");
            }
        } catch(Exception ignored){}
        return null;
    }
    //AI有时候会在JSON外面加一些废话
    private String cleanJSON(String s){
        if(s==null) return null;
        s = s.replaceAll("(?i)```json","").replaceAll("```","");
        return s.trim();
    }

    private String extractFirstJsonObject(String s){
        if(s==null) return null;
        int open = s.indexOf('{');
        if(open<0) return null;
        int depth=0;
        for(int i=open;i<s.length();i++){
            char c = s.charAt(i);
            if(c=='{') depth++;
            else if(c=='}') { depth--; if(depth==0) return s.substring(open,i+1); }
        }
        return null;
    }
    //创建提示词
    private String buildPrompt(int[][] board,int aiColor,String level){
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名五子棋 AI，请根据不同难度作出落子。\n");
        if("medium".equalsIgnoreCase(level))
            sb.append("难度：中级策略模式。请提前评估对手威胁，避免随机落子。\n");
        if("hard".equalsIgnoreCase(level))
            sb.append("难度：高级策略模式。请深入分析棋局，包括：\n- 冲四、活三判断\n- 防守对方威胁\n- 找出可能形成五连的点\n- 对双方关键点评分\n");
        sb.append("棋盘值：0=空，1=黑，-1=白。\n");
        sb.append("你执棋：" + (aiColor==1?"黑(1)":"白(-1)") + "\n\n");
        sb.append("棋盘如下：\n");
        for(int r=0;r<19;r++){
            for(int c=0;c<19;c++){
                sb.append(board[r][c]);
                if(c<18) sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("\n返回格式要求：只能返回 JSON {\"row\": 行, \"col\": 列}, 不能有解释文字, 必须是空位, 行列从0开始, 禁止 Markdown\n");
        sb.append("请直接返回 JSON：");
        return sb.toString();
    }

    private int evaluateMove(int[] m) {
        int center = 9;
        return -(Math.abs(m[0]-center) + Math.abs(m[1]-center));
    }

}
