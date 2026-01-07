package ru.otus.remotedesktop.server;

import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerApp {
    private static final int DEFAULT_PORT = 5900;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("неверный порт" + DEFAULT_PORT);
            }
        }

        System.out.println("запуск сервера на порту " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("сервер запущен. ждем подключения ");

            while (true) {
                new ConnectionHandler(serverSocket.accept()).start();
            }
        } catch (IOException | AWTException e) {
            System.err.println("ошибка сервера " + e.getMessage());
        }
    }
}
