package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int CONTROL_PORT = 5900;
    private static final int VIDEO_PORT = 5901;

    public static void main(String[] args) {
        logger.info("Запуск сервера удаленного рабочего стола");
        logger.info("Порт управления: {}, порт видео: {}", CONTROL_PORT, VIDEO_PORT);

        try {
            // Запускаем сервер управления
            Thread controlServer = new Thread(() -> startControlServer());
            controlServer.start();

            // Запускаем сервер видео
            Thread videoServer = new Thread(() -> startVideoServer());
            videoServer.start();

            // Ждем завершения
            controlServer.join();
            videoServer.join();

        } catch (Exception e) {
            logger.error("Критическая ошибка сервера: {}", e.getMessage());
        }
    }

    private static void startControlServer() {
        try (ServerSocket serverSocket = new ServerSocket(CONTROL_PORT)) {
            logger.info("Сервер управления запущен на порту {}", CONTROL_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Новое управляющее подключение от: {}", clientSocket.getInetAddress());

                try {
                    new ConnectionHandler(clientSocket).start();
                } catch (AWTException e) {
                    logger.error("Ошибка создания ConnectionHandler: {}", e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка сервера управления: {}", e.getMessage());
        }
    }

    private static void startVideoServer() {
        try (ServerSocket serverSocket = new ServerSocket(VIDEO_PORT)) {
            logger.info("Сервер видео запущен на порту {}", VIDEO_PORT);

            while (true) {
                Socket videoSocket = serverSocket.accept();
                logger.info("Новое видеоподключение от: {}", videoSocket.getInetAddress());

                new VideoConnectionHandler(videoSocket).start();
            }
        } catch (IOException e) {
            logger.error("Ошибка сервера видео: {}", e.getMessage());
        }
    }
}