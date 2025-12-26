package ru.otus.remotedesktop;

public class ServerTest {
    public static void main(String[] args) {
        Thread serverThread = new Thread(() -> {
            ru.otus.remotedesktop.server.ServerApp.main(new String[]{});
        });
        serverThread.start();

        System.out.println("Сервер тест");
    }
}
