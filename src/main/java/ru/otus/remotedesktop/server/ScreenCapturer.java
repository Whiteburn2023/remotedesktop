package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.ScreenFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

/**JavaCV */

public class ScreenCapturer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenCapturer.class);
    private final Robot robot;
    private final Rectangle screenRect;

    static {
        loadOpenCV();               // Загружаем OpenCV только при инициализации класса
    }

    public ScreenCapturer() throws AWTException {
        this.robot = new Robot();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenRect = new Rectangle(screenSize);
        logger.info("ScreenCapturer запущен: {}x{}", screenRect.width, screenRect.height);
    }


    private static void loadOpenCV() {
        try {
            String projectPath = System.getProperty("user.dir");
            String nativePath = projectPath + File.separator + "lib" + File.separator + "native";

                    File file = new File(nativePath + File.separator + "opencv_java481.dll");
                    if (file.exists()) {
                        System.load(file.getAbsolutePath());
                        logger.debug("Loaded: {}", "opencv_java481.dll");
                    } else {
                        throw new UnsatisfiedLinkError("DLL not found: " + file.getAbsolutePath());
                    }
            logger.info("OpenCV загружен успешно");
        } catch (UnsatisfiedLinkError e) {
            logger.error("ошибка загрузки OpenCV: {}", e.getMessage());
            throw new RuntimeException( "OpenCV библиотека не найдена ", e);
        }
    }

    public ScreenFrame captureFrame() throws IOException {
        long startTime = System.currentTimeMillis();

        BufferedImage screenshot = robot.createScreenCapture(screenRect);
        Point cursor = MouseInfo.getPointerInfo().getLocation();
        byte[] compressedImage = compressToJpeg(screenshot);

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("кадр захвачен за {} мс, сжат до {} байт", duration, compressedImage.length);

        return new ScreenFrame(
                compressedImage,
                screenRect.width,
                screenRect.height,
                (int) cursor.getX(),
                (int) cursor.getY()
        );
    }

    private byte[] compressToJpeg(BufferedImage image) throws IOException {
        try {
            BufferedImage convertedImage;
            if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                convertedImage = image;
            } else {
                convertedImage = new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_3BYTE_BGR
                );
                Graphics2D g = convertedImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }

            byte[] pixels = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();
            org.opencv.core.Mat mat = new org.opencv.core.Mat(
                    convertedImage.getHeight(),
                    convertedImage.getWidth(),
                    org.opencv.core.CvType.CV_8UC3
            );
            mat.put(0, 0, pixels);

            org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
            org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", mat, mob);

            byte[] result = mob.toArray();

            mat.release();
            mob.release();

            return result;

        } catch (Exception e) {
            throw new IOException("OpenCV JPEG ошибка сжатия: " + e.getMessage(), e);
        }
    }
}
