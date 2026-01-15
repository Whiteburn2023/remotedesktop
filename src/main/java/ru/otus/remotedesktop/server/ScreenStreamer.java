package ru.otus.remotedesktop.server;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenStreamer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenStreamer.class);

    private FFmpegFrameGrabber grabber;
    private FFmpegFrameRecorder recorder;
    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private Thread streamingThread;
    private OutputStream videoOutput;

    // Параметры потока
    private int width;
    private int height;
    private int fps = 20;
    private int bitrate = 2_000_000; // 2 Mbps
    private String codec = "libx264"; // По умолчанию программный кодек

    public ScreenStreamer(OutputStream outputStream) {
        this.videoOutput = outputStream;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = screenSize.width / 2; // Уменьшаем для производительности
        this.height = screenSize.height / 2;
    }

    public void start() throws Exception {
        if (streaming.get()) {
            return;
        }

        logger.info("Запуск видеопотока: {}x{} @ {}fps, битрейт: {} bps",
                width, height, fps, bitrate);

        // 1. Захват экрана через FFmpeg
        grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab");
        grabber.setImageWidth(width);
        grabber.setImageHeight(height);
        grabber.setFrameRate(fps);
        grabber.setOption("draw_mouse", "1");
        grabber.setOption("fflags", "nobuffer");

        grabber.start();
        logger.info("Захват экрана запущен");

        // 2. Кодирование в H.264
        // Используем PipedOutputStream для отправки в сеть
        recorder = new FFmpegFrameRecorder(new PipedOutputStreamWrapper(videoOutput),
                grabber.getImageWidth(), grabber.getImageHeight());

        recorder.setFormat("h264"); // Формат H.264 элементарный поток
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFrameRate(fps);
        recorder.setVideoBitrate(bitrate);
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
        recorder.setGopSize(fps * 2); // Ключевой кадр каждые 2 секунды

        // Настройки для низкой задержки
        recorder.setVideoOption("preset", "ultrafast");
        recorder.setVideoOption("tune", "zerolatency");
        recorder.setVideoOption("crf", "23");

        recorder.start();
        logger.info("Кодировщик запущен");

        streaming.set(true);

        // 3. Запуск потока трансляции
        streamingThread = new Thread(this::streamLoop);
        streamingThread.setName("ScreenStreamer-Thread");
        streamingThread.start();
    }

    private void streamLoop() {
        try {
            while (streaming.get()) {
                Frame frame = grabber.grab();
                if (frame != null) {
                    recorder.record(frame);
                }

                // Контроль FPS
                Thread.sleep(1000 / fps);
            }
        } catch (Exception e) {
            if (streaming.get()) {
                logger.error("Ошибка в потоке трансляции: {}", e.getMessage());
            }
        }
    }

    public void stop() {
        streaming.set(false);

        try {
            if (streamingThread != null && streamingThread.isAlive()) {
                streamingThread.join(1000);
            }

            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }

            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }

            logger.info("Видеопоток остановлен");
        } catch (Exception e) {
            logger.error("Ошибка остановки видеопотока: {}", e.getMessage());
        }
    }

    // Обертка для OutputStream
    private static class PipedOutputStreamWrapper extends OutputStream {
        private final OutputStream wrapped;

        public PipedOutputStreamWrapper(OutputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void write(int b) throws IOException {
            wrapped.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            wrapped.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            wrapped.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            wrapped.flush();
        }
    }
}
