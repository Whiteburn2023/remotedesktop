package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.Command;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private final Socket clientSocket;
    private final InputExecutor inputExecutor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    private ObjectOutputStream output;
    private ObjectInputStream input;

    public ConnectionHandler(Socket socket) throws AWTException {
        this.clientSocket = socket;
        this.inputExecutor = new InputExecutor(); // Убрали ScreenCapturer, так как видео идет отдельно
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getInetAddress().toString();
        logger.info("Новое управляющее подключение от {}", clientAddress);

        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());

            if (!authenticate()) {
                logger.warn("Авторизация не пройдена для {}", clientAddress);
                return;
            }

            logger.info("Клиент {} авторизован", clientAddress);
            authenticated.set(true);

            // Только прием команд, видео идет отдельным потоком через VideoConnectionHandler
            receiveCommands();

        } catch (Exception e) {
            logger.error("Ошибка обработки подключения от {}: {}", clientAddress, e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private boolean authenticate() {
        try {
            Object obj = input.readObject();
            if (!(obj instanceof AuthRequest)) {
                logger.error("Получен неверный объект при авторизации");
                return false;
            }

            AuthRequest auth = (AuthRequest) obj;
            boolean success = "admin".equals(auth.getUsername()) &&
                    "password".equals(auth.getPassword());

            output.writeBoolean(success);
            output.flush();

            if (success) {
                logger.info("Успешная авторизация для пользователя: {}", auth.getUsername());
            } else {
                logger.warn("Неудачная авторизация для пользователя: {}", auth.getUsername());
            }

            return success;

        } catch (Exception e) {
            logger.error("Ошибка авторизации: {}", e.getMessage());
            return false;
        }
    }

    private void receiveCommands() {
        try {
            while (running.get() && authenticated.get()) {
                Object obj = input.readObject();

                if (obj instanceof Command) {
                    Command cmd = (Command) obj;

                    int paramCount = input.readInt();
                    int[] params = new int[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        params[i] = input.readInt();
                    }

                    logger.debug("Получена команда: {} с {} параметрами", cmd, paramCount);
                    inputExecutor.executeCommand(cmd, params);
                } else {
                    logger.warn("Получен неизвестный объект: {}", obj.getClass().getName());
                }
            }
        } catch (EOFException e) {
            logger.info("Клиент отключился");
        } catch (Exception e) {
            if (running.get()) {
                logger.error("Ошибка приема команд: {}", e.getMessage());
            }
        }
    }

    private void closeConnection() {
        running.set(false);
        authenticated.set(false);

        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка при закрытии input: {}", e.getMessage());
        }

        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка при закрытии output: {}", e.getMessage());
        }

        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.debug("Ошибка при закрытии сокета: {}", e.getMessage());
        }

        logger.info("Управляющее подключение закрыто");
    }
}
