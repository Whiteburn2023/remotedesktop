package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.ScreenFrame;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;


public class ScreenCapturer {
    private final Robot robot;
    private final Rectangle screenRect;
    private final float imageQuality;

    public ScreenCapturer(float imageQuality) throws AWTException {
        this.robot = new Robot();
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        this.imageQuality = Math.max(0.1f, Math.min(1.0f, imageQuality));
    }

    public ScreenCapturer() throws AWTException {
        this(0.7f); // качество картинки 70%
    }

    public ScreenFrame captureFrame() throws IOException {
        BufferedImage screenshot = robot.createScreenCapture(screenRect);   //захват экрана

        Point cursorPos = MouseInfo.getPointerInfo().getLocation();         // получение позиции курсора
        int cursorX = (int) cursorPos.getX();
        int cursorY = (int) cursorPos.getY();

        byte[] compressedImage = compressToJpeg(screenshot);                // сжатие изображения

        return new ScreenFrame(                                             // создание и возврат кадра
                compressedImage,
                screenRect.width,
                screenRect.height,
                cursorX,
                cursorY
        );
    }

    private byte[] compressToJpeg(BufferedImage image) throws IOException {
        BufferedImage rgbImage = ensureRGB(image);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("jpeg кодировщик не найден");
            }

            ImageWriter writer = writers.next();
            ImageWriteParam params = writer.getDefaultWriteParam();

            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.getCompressionQuality();

            writer.setOutput(new MemoryCacheImageOutputStream(baos));
            writer.write(null, new IIOImage(rgbImage, null, null), params);
            writer.dispose();

            return baos.toByteArray();
        }
    }

    private BufferedImage ensureRGB(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        BufferedImage rgbImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return rgbImage;
    }

    public Robot getRobot() {
        return robot;
    }

    public Rectangle getScreenRect() {
        return screenRect;
    }

    public float getImageQuality() {
        return imageQuality;
    }
}
