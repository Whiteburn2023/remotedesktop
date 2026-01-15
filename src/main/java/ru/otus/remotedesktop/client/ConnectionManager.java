package ru.otus.remotedesktop.client;

import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    // Убрали frameConsumer и receiverThread, так как видео идет отдельным потоком

    public ConnectionManager() {
    }

    public boolean connect(String host, int port, String username, String password) {
        logger.info("Подключение к {}:{}", host, port);
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            socket.setSoTimeout(5000);
            input = new ObjectInputStream(socket.getInputStream());

            AuthRequest authRequest = new AuthRequest(username, password);
            output.writeObject(authRequest);
            output.flush();

            boolean authSuccess = input.readBoolean();
            if (!authSuccess) {
                logger.warn("Сервер отклонил авторизацию");
                disconnect();
                return false;
            }

            connected.set(true);
            logger.info("Подключение успешно");
            return true;

        } catch (java.net.SocketTimeoutException e) {
            logger.error("Таймаут подключения: сервер не ответил за 5 секунд");
            return false;
        } catch (java.net.ConnectException e) {
            logger.error("Не удалось подключиться: {}", e.getMessage());
            return false;
        } catch (EOFException e) {
            logger.error("Сервер закрыл соединение");
            return false;
        } catch (IOException e) {
            logger.error("Ошибка ввода/вывода: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Неизвестная ошибка: {}", e.getMessage());
            return false;
        }
    }

    public void sendCommand(Command cmd, int... params) {
        if (!connected.get()) {
            return;
        }

        try {
            synchronized (output) {
                output.writeObject(cmd);
                output.writeInt(params.length);
                for (int param : params) {
                    output.writeInt(param);
                }
                output.flush();
            }
        } catch (IOException e) {
            logger.error("Ошибка отправки команды {}", e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        connected.set(false);

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("Ошибка при отключении {}", e.getMessage());
        }
        logger.info("Отключено от сервера");
    }

    public boolean isConnected() {
        return connected.get();
    }
}
