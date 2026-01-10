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
    private boolean openCVAvailable = false;

    static {
        // Загружаем OpenCV при загрузке класса
        loadOpenCV();
    }

    public ScreenCapturer() throws AWTException {
        this.robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

    }

    private static void loadOpenCV() {
        try {
            // Пробуем разные пути к DLL
            String[] possiblePaths = {
                    System.getProperty("user.dir") + "\\opencv\\build\\java\\x64\\opencv_java481.dll",
                    System.getProperty("user.dir") + "\\opencv\\build\\x64\\vc16\\bin\\opencv_java481.dll",
                    "C:\\OTUS\\RemoteDesktop\\opencv\\build\\java\\x64\\opencv_java481.dll"
            };

            boolean loaded = false;
            for (String path : possiblePaths) {
                try {
                    System.load(path);
                    logger.info("OpenCV loaded from: {}", path);
                    loaded = true;
                    break;
                } catch (UnsatisfiedLinkError e) {
                    logger.debug("Failed to load from {}: {}", path, e.getMessage());
                }
            }

            if (!loaded) {
                // Пробуем загрузить из java.library.path
                System.loadLibrary("opencv_java481");
                logger.info("OpenCV loaded from system library path");
            }

            // Проверяем, что OpenCV загружен корректно
            String version = org.opencv.core.Core.VERSION;
            logger.info("OpenCV version: {}", version);

        } catch (UnsatisfiedLinkError e) {
            logger.error("Failed to load OpenCV: {}", e.getMessage());
            throw new RuntimeException("OpenCV not available. Please install OpenCV 4.8.1", e);
        }
    }

    public ScreenFrame captureFrame() throws IOException {
        long startTime = System.currentTimeMillis();

        BufferedImage screenshot = robot.createScreenCapture(screenRect);   //захват экрана

        Point cursor = MouseInfo.getPointerInfo().getLocation();            // получение позиции курсора

        byte[] compressedImage = compressWithOpenCV(screenshot);

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Frame captured: {}x{} -> {} bytes in {} ms",
                screenRect.width, screenRect.height,
                compressedImage.length, duration);

        return new ScreenFrame(                                             // создание и возврат кадра
                compressedImage,
                screenRect.width,
                screenRect.height,
                (int) cursor.getX(),
                (int) cursor.getY()
        );
    }

    private byte[] compressWithOpenCV(BufferedImage image) throws IOException {
        try {
            // Конвертируем BufferedImage в формат, подходящий для OpenCV
            BufferedImage convertedImage;

            // OpenCV ожидает BGR формат (не RGB!)
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
            // OpenCV использует BGR порядок, высота x ширина x каналы
            org.opencv.core.Mat mat = new org.opencv.core.Mat(
                    convertedImage.getHeight(),
                    convertedImage.getWidth(),
                    org.opencv.core.CvType.CV_8UC3
            );

            // Копируем данные в Mat
            mat.put(0, 0, pixels);

            // Сжимаем в JPEG с указанием качества
            org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
            org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", mat, mob);

            // Получаем результат
            byte[] result = mob.toArray();

            // Освобождаем ресурсы
            mat.release();
            mob.release();

            return result;

        } catch (Exception e) {
            logger.error("OpenCV compression error", e);
            throw new IOException("Failed to compress image with OpenCV", e);
        }
    }


}
