package ru.otus.remotedesktop.client;


import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.ScreenFrame;
import ru.otus.remotedesktop.common.Command;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ConnectionManager {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread receiverThread;
    private Consumer<ScreenFrame> frameConsumer;

    public ConnectionManager() {
    }

    public boolean connect(String host, int port, String username, String password) {
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            AuthRequest authRequest = new AuthRequest(username, password);
            output.writeObject(authRequest);
            output.flush();

            boolean authSuccess = input.readBoolean();
            if (!authSuccess){
                disconnect();
                return false;
            }

            connected.set(true);
            return true;

        } catch (IOException e) {
            System.err.println("ошибка подключения: " + e.getMessage());
            return false;
        }
    }

    public void startReceivingFrames(Consumer<ScreenFrame> frameConsumer) {
        this.frameConsumer = frameConsumer;
        receiverThread = new Thread(this::receiveFrames);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void receiveFrames() {
        try {
            while (connected.get()) {
                Object obj = input.readObject();
                if (obj instanceof ScreenFrame) {
                    ScreenFrame frame = (ScreenFrame) obj;
                    if (frameConsumer != null) {
                        frameConsumer.accept(frame);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("сервер отключился");
        } catch (IOException | ClassNotFoundException e) {
            if (connected.get()) {
                System.err.println("ошибка приема кадров " + e.getMessage());
                disconnect();
            }
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
            System.err.println("ошибка отправки команды " + e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        connected.set(false);
        if (receiverThread != null && receiverThread.isAlive()) {
            receiverThread.interrupt();
        }
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("ошибка при отключении " + e.getMessage());
        }
        System.out.println("отключено от сервера");
    }

    public boolean isConnected() {
        return connected.get();
    }
}
