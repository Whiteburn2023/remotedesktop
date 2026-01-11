package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int DEFAULT_PORT = 5900;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("неверный формат порта: {}. используется порт по умолчанию: {}", args[0], DEFAULT_PORT, e);
            }
        }

        logger.info("запуск сервера на порту {}", port);


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("сервер запущен. ждем подключения");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("новое подключение от: {}", clientSocket.getInetAddress());

                try {
                    new ConnectionHandler(clientSocket).start();
                } catch (Exception e) {
                    logger.error("ошибка создания ConnectionHandler: {}", e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            logger.error("критическая ошибка сервера на порту {}: {}", port, e.getMessage());
        } catch (Exception e) {
            logger.error("неожиданная ошибка в работе сервера", e);
        }

    }

}
