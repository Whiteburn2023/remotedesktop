package ru.otus.remotedesktop.server;

import java.net.Socket;

public class ConnectionHandler extends Thread {
    private final Socket clientSocket;

    public ConnectionHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("Новое подключение " + clientSocket.getInetAddress());
        //здесь будет обработка подключения
        try {
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
