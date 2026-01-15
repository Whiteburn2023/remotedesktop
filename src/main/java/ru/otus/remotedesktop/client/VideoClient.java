package ru.otus.remotedesktop.client;

import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoClient {
    private static final Logger logger = LoggerFactory.getLogger(VideoClient.class);

    private Socket videoSocket;
    private FFmpegFrameGrabber grabber;
    private CanvasFrame canvas;
    private Thread playbackThread;
    private final AtomicBoolean playing = new AtomicBoolean(false);
    private FrameListener frameListener;

    public interface FrameListener {
        void onFrameReceived(Frame frame);
    }

    public VideoClient(FrameListener listener) {
        this.frameListener = listener;
    }

    public boolean connect(String host, int port) {
        logger.info("Подключение видеоклиента к {}:{}", host, port);

        try {
            videoSocket = new Socket(host, port);
            videoSocket.setSoTimeout(5000);

            InputStream videoInput = videoSocket.getInputStream();

            // Настраиваем FFmpeg для декодирования H.264 потока
            grabber = new FFmpegFrameGrabber(videoInput);
            grabber.setFormat("h264"); // Принимаем H.264 элементарный поток
            grabber.setOption("rtbufsize", "10M");
            grabber.setOption("fflags", "nobuffer");
            grabber.setOption("flags", "low_delay");

            grabber.start();

            playing.set(true);
            logger.info("Видеоподключение успешно");

            // Запускаем воспроизведение
            playbackThread = new Thread(this::playbackLoop);
            playbackThread.setName("VideoPlayback-Thread");
            playbackThread.start();

            return true;

        } catch (Exception e) {
            logger.error("Ошибка видеоподключения: {}", e.getMessage());
            disconnect();
            return false;
        }
    }

    private void playbackLoop() {
        try {
            while (playing.get()) {
                Frame frame = grabber.grab();
                if (frame != null && frameListener != null) {
                    frameListener.onFrameReceived(frame);
                }

                Thread.sleep(1); // Небольшая пауза
            }
        } catch (Exception e) {
            if (playing.get()) {
                logger.error("Ошибка воспроизведения: {}", e.getMessage());
            }
        }
    }

    public void disconnect() {
        playing.set(false);

        try {
            if (playbackThread != null && playbackThread.isAlive()) {
                playbackThread.join(1000);
            }

            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }

            if (videoSocket != null && !videoSocket.isClosed()) {
                videoSocket.close();
            }

            if (canvas != null) {
                canvas.dispose();
            }

            logger.info("Видеоклиент отключен");
        } catch (Exception e) {
            logger.error("Ошибка отключения видеоклиента: {}", e.getMessage());
        }
    }

    public boolean isPlaying() {
        return playing.get();
    }
}
