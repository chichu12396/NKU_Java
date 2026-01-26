package Gomoku;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<ClientHandler>());
    
    public void start(int port) {
        try {
            System.out.println("开始启动服务器...");
            serverSocket = new ServerSocket(port);
            System.out.println("服务器启动成功！端口: " + port);
            System.out.println("等待玩家连接...");
            
            ExecutorService pool = Executors.newFixedThreadPool(10);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("检测到客户端连接请求...");
                
                ClientHandler client = new ClientHandler(clientSocket, clients.size());
                clients.add(client);
                pool.execute(client);
                
                System.out.println("新客户端连接，ID: " + client.clientId + 
                                 ", IP: " + clientSocket.getInetAddress() +
                                 ", 当前在线: " + clients.size());
                // 匹配玩家
                matchPlayers();
            }
        } catch (IOException e) {
            System.out.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void matchPlayers() {
        System.out.println("检查玩家匹配，当前玩家数: " + clients.size());
        // 只匹配还没有开始游戏的玩家
        List<ClientHandler> availablePlayers = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (!client.isInGame) {
                availablePlayers.add(client);
            }
        }
        
        System.out.println("可用玩家数: " + availablePlayers.size());
        
        if (availablePlayers.size() >= 2) {
            ClientHandler player1 = availablePlayers.get(0);
            ClientHandler player2 = availablePlayers.get(1);
            
            player1.assignColor("BLACK");
            player2.assignColor("WHITE");
            player1.setOpponent(player2);
            player2.setOpponent(player1);
            
            System.out.println("匹配成功！游戏开始: 玩家" + player1.clientId + "(黑) vs 玩家" + player2.clientId + "(白)");
            
            // 注意：这里不要从clients列表中移除，否则会破坏循环
        }
    }
    
    private class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        public int clientId;
        private String playerColor;
        private ClientHandler opponent;
        public boolean isInGame = false;
        
        public ClientHandler(Socket socket, int id) {
            this.socket = socket;
            this.clientId = id;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                System.out.println("客户端Handler创建成功 ID: " + id);
            } catch (IOException e) {
                System.out.println("客户端Handler创建失败: " + e.getMessage());
            }
        }
        
        public void assignColor(String color) {
            this.playerColor = color;
            this.isInGame = true;
            writer.println("WELCOME:" + color);
            System.out.println("分配颜色: 玩家" + clientId + " = " + color);
        }
        
        public void setOpponent(ClientHandler opponent) {
            this.opponent = opponent;
            System.out.println("设置对手: 玩家" + clientId + " vs 玩家" + opponent.clientId);
        }
        
        @Override
        public void run() {
            try {
                System.out.println("开始监听玩家" + clientId + "的消息...");
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("玩家" + clientId + "(" + playerColor + ") 发送: " + message);
                    
                    if (opponent != null && opponent.writer != null) {
                        // 转发消息给对手
                        opponent.writer.println(message);
                        System.out.println("转发消息给对手");
                    }
                }
            } catch (IOException e) {
                System.out.println("玩家" + clientId + " 断开连接: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    System.out.println("关闭玩家" + clientId + "的Socket");
                } catch (IOException e) {}
                // 从列表中移除断开的客户端
                clients.remove(this);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("启动五子棋服务器...");
        GameServer server = new GameServer();
        server.start(12345);
    }
}