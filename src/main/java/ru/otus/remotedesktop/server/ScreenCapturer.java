package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.ScreenFrame;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ScreenCapturer {
    private final Robot robot;
    private final Rectangle screenRect;

    public ScreenCapturer() throws AWTException {
        this.robot = new Robot();
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    public ScreenFrame captureFrame() throws IOException {
        BufferedImage screenshot = robot.createScreenCapture(screenRect);   //захват экрана

        Point cursor = MouseInfo.getPointerInfo().getLocation();            // получение позиции курсора

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenshot, "jpg", baos);

        return new ScreenFrame(                                             // создание и возврат кадра
                baos.toByteArray(),
                screenRect.width,
                screenRect.height,
                (int)cursor.getX(),
                (int)cursor.getY()
        );
    }
}
