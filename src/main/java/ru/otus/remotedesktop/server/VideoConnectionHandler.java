package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

public class VideoConnectionHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(VideoConnectionHandler.class);
    private final Socket videoSocket;
    private ScreenStreamer screenStreamer;

    public VideoConnectionHandler(Socket videoSocket) {
        this.videoSocket = videoSocket;
    }

    @Override
    public void run() {
        String clientAddress = videoSocket.getInetAddress().toString();
        logger.info("Видеоподключение от {}", clientAddress);

        try (OutputStream output = videoSocket.getOutputStream()) {
            screenStreamer = new ScreenStreamer(output);
            screenStreamer.start();

            // Ждем, пока клиент подключен
            while (!videoSocket.isClosed()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.error("Ошибка видеоподключения: {}", e.getMessage());
        } finally {
            stopStreaming();
            closeSocket();
        }
    }

    private void stopStreaming() {
        if (screenStreamer != null) {
            screenStreamer.stop();
        }
    }

    private void closeSocket() {
        try {
            if (videoSocket != null && !videoSocket.isClosed()) {
                videoSocket.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка закрытия видеосокета: {}", e.getMessage());
        }
    }
}
