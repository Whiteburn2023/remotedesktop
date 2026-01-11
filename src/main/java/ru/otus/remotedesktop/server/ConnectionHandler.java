package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.Command;
import ru.otus.remotedesktop.common.ScreenFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final ScreenCapturer capturer;
    private final InputExecutor inputExecutor;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private ObjectOutputStream output;
    private ObjectInputStream input;
    private boolean authenticated = false;

    public ConnectionHandler(Socket socket) throws AWTException {
        this.clientSocket = socket;
        this.capturer = new ScreenCapturer();
        this.inputExecutor = new InputExecutor();
    }

    @Override
    public void run() {
        logger.info("Новое подключение от {}", clientSocket.getInetAddress());

        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());

            if (!authenticate()) {
                logger.warn("Авторизация не пройдена для {}", clientSocket.getInetAddress());
                return;
            }

            logger.info("Клиент {} авторизован", clientSocket.getInetAddress());

            Thread senderThread = new Thread(this::sendScreenFrames);
            senderThread.start();

            receiveCommands();

            running.set(false);
            senderThread.join(1000);

        } catch (Exception e) {
            logger.error("ошибка обработки подключения", e);
        } finally {
            closeConnection();
        }
    }

    private boolean authenticate() {
        try {
            Object obj = input.readObject();
            if (!(obj instanceof AuthRequest)) {
                return false;
            }

            AuthRequest auth = (AuthRequest) obj;
            boolean success = "admin".equals(auth.getUsername()) &&
                            "password".equals(auth.getPassword());
            output.writeBoolean(success);
            output.flush();

            authenticated = success;
            return success;
        } catch (Exception e) {
            logger.error("ошибка авторизации {}", e.getMessage());
            return false;
        }
    }

    private void sendScreenFrames() {
        try {
            while (running.get() && authenticated) {
                ScreenFrame frame = capturer.captureFrame();

                synchronized (output) {
                    output.writeObject(frame);
                    output.flush();
                }

                Thread.sleep(50);
            }
        } catch (Exception e) {
            logger.error("ошибка отправки кадров {}", e.getMessage());
            running.set(false);
        }
    }

    private void receiveCommands() {
        try {
            while (running.get() && authenticated) {
                Object obj = input.readObject();

                if (obj instanceof Command) {
                    Command cmd = (Command) obj;

                    int paramCount = input.readInt();
                    int[] params = new int[paramCount];
                    for (int i = 0; i < paramCount; i++) {
                        params[i] = input.readInt();
                    }
                    inputExecutor.executeCommand(cmd, params);
                } else if (obj instanceof String) {
                    logger.info("Сообщение от клиента: {}", obj);
                }
            }
        } catch (EOFException e) {
            logger.info("Клиент отключился{}", clientSocket.getInetAddress());
        } catch (Exception e) {
            logger.error("ошибка приема команд {}", e.getMessage());
        }
    }

    private void closeConnection() {
        running.set(false);

        try {
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.error("ошибка при закрытии соединения {}", e.getMessage()); // System.err.println("ошибка при закрытии соединения " + e.getMessage());
        }
        logger.info("подключение закрыто: {}", clientSocket.getInetAddress());
    }
}
