package Gomoku;

import java.util.Timer;
import java.util.TimerTask;

public class CountdownManager {

    public interface CountdownListener {
        // 每秒触发一次，remainingSeconds >= 0；color 表示当前轮到哪方（Model.BLACK/Model.WHTTE）
        void onTick(int remainingSeconds, int currentTurnColor);

        // 当倒计时被停止（手动 stop）
        void onStopped();

        // 当倒计时结束（时间到）
        void onTimeout(int timeoutColor);
    }

    private Timer timer;
    private TimerTask task;
    private int remainingSeconds;
    private boolean running = false;
    private int currentTurnColor; // 谁的回合（Model.BLACK or Model.WHTTE）
    private final CountdownListener listener;

    // 每秒执行一次
    private static final long PERIOD_MS = 1000L;

    public CountdownManager(int initialSeconds, int startTurnColor, CountdownListener listener) {
        this.remainingSeconds = initialSeconds;
        this.currentTurnColor = startTurnColor;
        this.listener = listener;
    }

    // 启动计时器（如果已经在运行，则先停止再启动）
    public synchronized void start() {
        stopInternal();
        running = true;
        timer = new Timer("CountdownTimer", true);
        task = new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        };
        timer.scheduleAtFixedRate(task, PERIOD_MS, PERIOD_MS);
        // 立即通知一次 UI 显示初始时间
        notifyTick();
    }

    // 线程安全的内部 tick
    private synchronized void tick() {
        if (!running) return;
        remainingSeconds--;
        notifyTick();
        if (remainingSeconds <= 0) {
            // 停止并回调超时（超时颜色是 currentTurnColor）
            stopInternal();
            if (listener != null) {
                try {
                    listener.onTimeout(currentTurnColor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void notifyTick() {
        if (listener != null) {
            try {
                listener.onTick(Math.max(0, remainingSeconds), currentTurnColor);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 停止计时器（不会改变 remainingSeconds）
    public synchronized void stop() {
        stopInternal();
        if (listener != null) {
            try {
                listener.onStopped();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 内部停止实现
    private synchronized void stopInternal() {
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    // 重置剩余时间（并不自动启动）
    public synchronized void reset(int seconds) {
        this.remainingSeconds = seconds;
    }

    // 强制设置当前轮到的颜色（当回合切换或网络消息同步时调用）
    public synchronized void setCurrentTurnColor(int color) {
        this.currentTurnColor = color;
    }

    // 当从网络收到对方的剩余时间（或服务器同步时间）时使用：覆盖本地 remaining 并通知 UI
    public synchronized void forceSetRemainingAndNotify(int seconds, int turnColor) {
        this.remainingSeconds = seconds;
        this.currentTurnColor = turnColor;
        notifyTick();
    }

    // 查询是否在运行
    public synchronized boolean isRunning() {
        return running;
    }

    // 查询剩余时间
    public synchronized int getRemainingSeconds() {
        return remainingSeconds;
    }

    // 查询当前回合颜色
    public synchronized int getCurrentTurnColor() {
        return currentTurnColor;
    }
}
