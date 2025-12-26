package ru.otus.remotedesktop;

import ru.otus.remotedesktop.common.AuthRequest;
import ru.otus.remotedesktop.common.Command;
import ru.otus.remotedesktop.common.ScreenFrame;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) {
        try {
            System.out.println("Подключение к серверу...");
            Socket socket = new Socket("localhost", 5900);

            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            // Авторизация
            output.writeObject(new AuthRequest("admin", "password"));
            output.flush();

            boolean authSuccess = input.readBoolean();
            if (!authSuccess) {
                System.out.println("Ошибка авторизации!");
                return;
            }

            System.out.println("Авторизация успешна!");

            // Читаем и выводим информацию о первом кадре
            Object obj = input.readObject();
            if (obj instanceof ScreenFrame) {
                ScreenFrame frame = (ScreenFrame) obj;
                System.out.printf("Получен кадр: %dx%d, курсор: (%d, %d), размер данных: %d байт%n",
                        frame.getWidth(), frame.getHeight(),
                        frame.getCursorX(), frame.getCursorY(),
                        frame.getImageData().length);
            }

            // Отправляем тестовую команду перемещения мыши
            output.writeObject(Command.MOUSE_MOVE);
            output.writeInt(2); // 2 параметра
            output.writeInt(100); // x
            output.writeInt(100); // y
            output.flush();

            System.out.println("Тестовая команда отправлена");

            Thread.sleep(2000);

            socket.close();
            System.out.println("Тест завершен");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
