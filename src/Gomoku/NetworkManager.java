package Gomoku;

import java.io.*;
import java.net.*;

public class NetworkManager {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isConnected = false;
    private static NetworkManager instance;

    // 单例模式
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    private NetworkManager() {} // 私有构造

    // 连接服务器
    public boolean connectToServer(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            isConnected = true;

            // 启动消息监听线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listenMessages();
                }
            }).start();

            return true;
        } catch (Exception e) {
            System.out.println("连接错误: " + e.getMessage());
            return false;
        }
    }

    // 发送落子信息
    public void sendMove(int row, int col, int color) {
        sendMessage("MOVE", row + "," + col + "," + color);
    }

    // 发送聊天消息
    public void sendChat(String message) {
        sendMessage("CHAT", message);
    }

    // 发送悔棋请求
    public void sendUndo() {
        sendMessage("UNDO", "");
    }

    // 通用发送
    public void sendMessage(String type, String content) {
        if (isConnected) {
            writer.println(type + ":" + content);
            System.out.println("发送消息: " + type + ":" + content);
        }
    }

    // 消息监听
    private void listenMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            System.out.println("连接断开");
            isConnected = false;
        }
    }

    // 处理服务器消息
    private void processServerMessage(String message) {
        System.out.println("收到服务器消息: " + message);
        String type = message.contains(":") ? message.split(":", 2)[0] : message;
        String content = message.contains(":") ? message.split(":", 2)[1] : "";

        if ("WELCOME".equals(type)) {
            final boolean isBlack = "BLACK".equals(content);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().setNetworkInfo(true, isBlack);
                }
            });

        } else if ("MOVE".equals(type)) {
            final String[] parts = content.split(",");
            final int row = Integer.parseInt(parts[0]);
            final int col = Integer.parseInt(parts[1]);
            final int color = Integer.parseInt(parts[2]);

            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processNetworkMove(row, col, color);
                }
            });

        } else if ("CHAT".equals(type)) {
            final String chatMsg = content;
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                	View.getInstance().displayChatMessage("对手", chatMsg);
                }
            });

        } else if ("UNDO".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processNetworkUndo();
                }
            });

        } else if ("MESSAGE".equals(type)) {
            final String serverMsg = content;
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                	View.getInstance().displayChatMessage("系统", serverMsg);
                }
            });

        } else if ("RESTART".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processNetworkRestart();
                }
            });

        } else if ("REVIEW".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    //Cont.getInstance().processNetworkReview();
                }
            });

        } else if ("UNDO_REQUEST".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processUndoRequest();
                }
            });

        } else if ("UNDO_ACCEPT".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processUndoAccept();
                }
            });

        } else if ("UNDO_REJECT".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processUndoReject();
                }
            });

        } else if ("GAME_END".equals(type) || "TIMEOUT".equals(type)) {
            final int winner = Integer.parseInt(content);
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processGameEnd(winner);
                }
            });

        } else if ("RESTART_REQUEST".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processRestartRequest();
                }
            });

        } else if ("RESTART_ACCEPT".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processRestartAccept();
                }
            });

        } else if ("RESTART_REJECT".equals(type)) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Cont.getInstance().processRestartReject();
                }
            });
        }
    }

    // 检查连接状态
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public boolean isConnectionAlive() {
        if (!isConnected()) return false;
        try {
            writer.println("PING");
            return true;
        } catch (Exception e) {
            isConnected = false;
            return false;
        }
    }
}
