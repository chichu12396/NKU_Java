package Gomoku;

import javax.sound.sampled.*;
import java.io.File;

public class Music {
    private static Music instance;
    
    // 音频剪辑
    private Clip backgroundMusic;   // 游戏背景音乐 (game.wav)
    private Clip endMusic;          // 结束音乐 (end.wav)
    private Clip clickSound;        // 按钮点击音效 (click.wav)
    private Clip gameStartSound;    // 游戏开始音效 (clickstart.wav)
    private Clip appStartSound;     // 程序启动音效 (newstart.wav)
    
    // 音频文件路径（相对于项目根目录）
    private static final String SOUNDS_FOLDER = "sounds/";
    private boolean isPlayingEndMusic = false;
    
    private Music() {}
    
    public static Music getInstance() {
        if (instance == null) {
            instance = new Music();
        }
        return instance;
    }
    
    // 初始化音频系统（在程序启动时调用一次）
    public void init() {
        System.out.println("初始化音频系统...");
        loadAllSounds();
        playAppStartSound();
    }
    
    // 加载所有音频文件
    private void loadAllSounds() {
        try {
            // 加载程序启动音效
            appStartSound = loadClip(SOUNDS_FOLDER + "newstart.wav");
            System.out.println(" 程序启动音效加载成功");
            
            // 加载游戏开始音效
            gameStartSound = loadClip(SOUNDS_FOLDER + "clickstart.wav");
            System.out.println(" 游戏开始音效加载成功");
            
            // 加载按钮点击音效
            clickSound = loadClip(SOUNDS_FOLDER + "click.wav");
            System.out.println(" 按钮点击音效加载成功");
            
            // 加载游戏背景音乐
            backgroundMusic = loadClip(SOUNDS_FOLDER + "game.wav");
            System.out.println(" 游戏背景音乐加载成功");
            
            // 加载结束音乐
            endMusic = loadClip(SOUNDS_FOLDER + "end.wav");
            System.out.println(" 结束音乐加载成功");
            
        } catch (Exception e) {
            System.err.println("音频加载错误: " + e.getMessage());
        }
    }
    
    // 从文件加载音频剪辑
    private Clip loadClip(String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                System.err.println("× 音频文件不存在: " + filePath);
                return null;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            // 设置音量控制
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // 调整音量（-20分贝 = 安静，0分贝 = 最大）
                if (filePath.contains("game.wav") || filePath.contains("end.wav")) {
                    gainControl.setValue(-20.0f); // 背景音乐音量低一些
                } else {
                    gainControl.setValue(-5.0f); // 音效音量中等
                }
            }
            
            return clip;
            
        } catch (Exception e) {
            System.err.println("加载音频文件失败 " + filePath + ": " + e.getMessage());
            return null;
        }
    }
    
    // ========== 播放音效的方法 ==========
    
    // 1. 程序启动时播放
    public void playAppStartSound() {
        if (appStartSound != null) {
            stopAllSounds();
            playOnce(appStartSound);
            System.out.println("播放程序启动音效");
        }
    }
    
    // 2. 开始游戏时播放
    public void playGameStartSound() {
        if (gameStartSound != null) {
            playOnce(gameStartSound);
            System.out.println("播放游戏开始音效");
        }
    }
    
    // 3. 按钮点击时播放（除了开始按钮）
    public void playClickSound() {
        if (clickSound != null && !isPlayingEndMusic) {
            // 如果音效正在播放，先停止
            if (clickSound.isRunning()) {
                clickSound.stop();
            }
            clickSound.setFramePosition(0);
            clickSound.start();
            System.out.println("播放按钮点击音效");
        }
    }
    
    // 4. 开始游戏背景音乐
    public void startBackgroundMusic() {
        if (backgroundMusic != null && !isPlayingEndMusic) {
            stopAllSounds();
            backgroundMusic.setFramePosition(0);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
            System.out.println("开始播放游戏背景音乐（循环）");
        }
    }
    
    // 5. 游戏结束音乐
    public void playEndMusic() {
        if (endMusic != null) {
            stopAllSounds();
            isPlayingEndMusic = true;
            endMusic.setFramePosition(0);
            endMusic.start();
            System.out.println("播放游戏结束音乐");
            
            // 监听音乐结束，重置状态
            endMusic.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        isPlayingEndMusic = false;
                        endMusic.removeLineListener(this);
                    }
                }
            });
        }
    }
    
    // ========== 控制方法 ==========
    public boolean hasmusic(){
    	return backgroundMusic!=null;
    }
    // 停止所有声音
    public void stopAllSounds() {
        isPlayingEndMusic = false;
        
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
            backgroundMusic.setFramePosition(0);
        }
        if (appStartSound != null && appStartSound.isRunning()) {
        	appStartSound.stop();
        	appStartSound.setFramePosition(0);
        }
        if (endMusic != null && endMusic.isRunning()) {
            endMusic.stop();
            endMusic.setFramePosition(0);
        }
        System.out.println("结束所有音效成功");
        // 不停止短音效，让它们自然结束
    }
    
    
    // ========== 辅助方法 ==========
    
    // 播放一次音频
    private void playOnce(Clip clip) {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }
}