package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int DEFAULT_PORT = 5900;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("неверный формат порта: {}. используется порт по умолчанию: {}", args[0], DEFAULT_PORT, e);    //System.out.println("неверный порт" + DEFAULT_PORT);
            }
        }

        logger.info("запуск сервера на порту {}", port);    //System.out.println("запуск сервера на порту " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("сервер запущен. ждем подключения ");

            while (true) {
                logger.debug("ожидание нового подключения");
                new ConnectionHandler(serverSocket.accept()).start();
                logger.info("новое подключение принято. соединение");
            }
        } catch (IOException | AWTException e) {
            logger.error("критическая ошибка сервера на порту {}: {}", port, e.getMessage(), e);    //System.err.println("ошибка сервера " + e.getMessage());
        } catch (Exception e) {
            logger.error("неожиданная ошибка в работе сервера", e);
        }
    }

}
