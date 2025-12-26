package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.Command;
import ru.otus.remotedesktop.common.ScreenFrame;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHandler extends Thread {
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
        System.out.println("Новое подключение " + clientSocket.getInetAddress());

        try {
            output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(clientSocket.getInputStream());

            if (!authenticate()) {
                System.out.println("авторизация не пройдена " + clientSocket.getInetAddress());
                return;
            }

            System.out.println("клиент " +clientSocket.getInetAddress() + " авторизован");

            Thread senderThread = new Thread(this::sendScreenFrames);
            senderThread.start();

            receiveCommands();

            running.set(false);
            senderThread.join(1000);

        } catch (Exception e) {
            System.err.println("ошибка обработки подключения " + e.getMessage());
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
            System.err.println("ошибка авторизации " + e.getMessage());
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
            System.err.println("ошибка отправки кадров " + e.getMessage());
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
                    System.out.println("сообщение от клиента " + obj);
                }
            }
        } catch (EOFException e) {
            System.out.println("клиент отключился " + clientSocket.getInetAddress());
        } catch (Exception e) {
            System.err.println("ошибка приема команд " + e.getMessage());
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
            System.err.println("ошибка при закрытии соединения " + e.getMessage());
        }
        System.out.println("подключение закрыто " + clientSocket.getInetAddress());
    }
}
