package ru.otus.remotedesktop.server;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Frame;
import ru.otus.remotedesktop.common.ScreenFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/**JavaCV */

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_imgcodecs;


public class ScreenCapturer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenCapturer.class);
    private final Robot robot;
    private final Rectangle screenRect;

    private Java2DFrameConverter frameConverter;
    private OpenCVFrameConverter.ToMat matConverter;
    private boolean javaCVAvailable = false;

    public ScreenCapturer() throws AWTException {
        this.robot = new Robot();
        this.screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        initJavaCV();

    }

    private void initJavaCV() {
        try {
            // Проверяем, доступны ли классы JavaCV
            Class.forName("org.bytedeco.opencv.global.opencv_imgcodecs");

            this.frameConverter = new Java2DFrameConverter();
            this.matConverter = new OpenCVFrameConverter.ToMat();
            this.javaCVAvailable = true;

            logger.info("JavaCV успешно инициализирован");
        } catch (Exception e) {
            logger.warn("JavaCV недоступен: {}. Используется стандартное сжатие.", e.getMessage());
            this.javaCVAvailable = false;
        }
    }

    public ScreenFrame captureFrame() throws IOException {
        long startTime = System.currentTimeMillis();

        BufferedImage screenshot = robot.createScreenCapture(screenRect);   //захват экрана

        Point cursor = MouseInfo.getPointerInfo().getLocation();            // получение позиции курсора

        byte[] compressedImage;
        if (javaCVAvailable) {
            try {
                compressedImage = compressWithJavaCV(screenshot);
            } catch (Exception e) {
                logger.warn("JavaCV сжатие не удалось: {}. Используется ImageIO.", e.getMessage());
                compressedImage = compressWithImageIO(screenshot);
            }
        } else {
            compressedImage = compressWithImageIO(screenshot);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Кадр захвачен: {}x{} -> {} байт за {} мс (JavaCV: {})",
                screenRect.width, screenRect.height,
                compressedImage.length, duration, javaCVAvailable);

        return new ScreenFrame(                                             // создание и возврат кадра
                compressedImage,
                screenRect.width,
                screenRect.height,
                (int)cursor.getX(),
                (int)cursor.getY()
        );
    }

    private byte[] compressWithJavaCV(BufferedImage image) throws IOException {
        try {
            /** конвертация BufferedImage - Frame - Mat */
            Frame frame = frameConverter.convert(image);
            Mat mat = matConverter.convert(frame);

            /** параметры сжатия jpeg */
            MatVector params = new MatVector();

            BytePointer buf = new BytePointer();
            opencv_imgcodecs.imencode(".jpg", mat, buf);

            long size = buf.limit() - buf.position();
            byte[] result = new byte[(int)size];
            buf.get(result);

            mat.release();
            buf.deallocate();

            return result;

        } catch (Exception e) {
            logger.error("ошибка сжатия JavaCV", e);
            throw new IOException("ошибка сжатия изображения", e);
        }
    }

    private byte[] compressWithImageIO(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }


}
