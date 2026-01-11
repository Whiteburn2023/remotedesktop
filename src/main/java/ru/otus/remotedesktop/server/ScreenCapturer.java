package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.ScreenFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**JavaCV */

public class ScreenCapturer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenCapturer.class);
    private final Robot robot;
    private final Rectangle screenRect;

    static {
        // Загружаем OpenCV при загрузке класса
        OpenCVLoader.load();
    }

    public ScreenCapturer() throws AWTException {
        this.robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenRect = new Rectangle(screenSize);
        logger.info("ScreenCapturer initialized with screen size: {}x{}", screenRect.width, screenRect.height);
    }

    public ScreenFrame captureFrame() throws IOException {
        long startTime = System.currentTimeMillis();

        // Захват экрана
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        // Получение позиции курсора
        Point cursor = MouseInfo.getPointerInfo().getLocation();

        byte[] compressedImage;

        // Используем OpenCV если доступен, иначе ImageIO
        if (OpenCVLoader.isLoaded()) {
            try {
                compressedImage = compressWithOpenCV(screenshot);
                logger.debug("Compressed with OpenCV: {} bytes", compressedImage.length);
            } catch (Exception e) {
                logger.warn("OpenCV compression failed, using ImageIO fallback: {}", e.getMessage());
                compressedImage = compressWithImageIO(screenshot);
            }
        } else {
            compressedImage = compressWithImageIO(screenshot);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Frame captured in {} ms", duration);

        return new ScreenFrame(
                compressedImage,
                screenRect.width,
                screenRect.height,
                (int) cursor.getX(),
                (int) cursor.getY()
        );
    }

    private byte[] compressWithOpenCV(BufferedImage image) throws IOException {
        try {
            // Конвертируем BufferedImage в формат BGR для OpenCV
            BufferedImage convertedImage;

            // OpenCV ожидает BGR формат
            if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                convertedImage = image;
            } else {
                // Конвертируем в TYPE_3BYTE_BGR
                convertedImage = new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR
                );
                Graphics2D g = convertedImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }

            // Получаем байты изображения
            byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();

            // Создаем Mat из байтов
            org.opencv.core.Mat mat = new org.opencv.core.Mat(
                    convertedImage.getHeight(),
                    convertedImage.getWidth(),
                    org.opencv.core.CvType.CV_8UC3
            );

            // Копируем данные
            mat.put(0, 0, pixels);

            // Сжимаем в JPEG
            org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
            org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", mat, mob);

            byte[] result = mob.toArray();

            // Освобождаем ресурсы
            mat.release();
            mob.release();

            return result;

        } catch (Exception e) {
            throw new IOException("OpenCV compression error: " + e.getMessage(), e);
        }
    }

    private byte[] compressWithImageIO(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Используем ImageWriter для лучшего сжатия
        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.8f); // 80% качество
        }

        try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }
}
