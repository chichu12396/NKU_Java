package Gomoku;

import javax.swing.*;

import java.awt.*;

@SuppressWarnings("serial")
public class ImageButton extends JButton {

    private Image normalImg;
    private Image hoverImg;
    private Image pressedImg;

    public ImageButton(String text) {
        super(text);

        // 加载图片
        normalImg = new ImageIcon("resource/btn_normal.png").getImage();
        hoverImg = new ImageIcon("resource/btn_hover.png").getImage();
        pressedImg = new ImageIcon("resource/btn_pressed.png").getImage();

        // Swing 默认行为全部关掉
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);

        // 文字样式
        setForeground(new Color(184, 134, 11));
        setFont(new Font("微软雅黑", Font.BOLD, 22));

        // 开启 hover 监听
        setRolloverEnabled(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        Image bg;
        ButtonModel model = getModel();
        if (model.isPressed()) {
            bg = pressedImg;
        } else if (model.isRollover()) {
            bg = hoverImg;
        } else {
            bg = normalImg;
        }

        //图片随按钮大小缩放
        g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);

        // 画文字
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent()) / 2 - 6;

        g2.setColor(getForeground());
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}
