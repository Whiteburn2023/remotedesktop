package ru.otus.remotedesktop.server;


import ru.otus.remotedesktop.common.Command;

import java.awt.*;
import java.awt.event.InputEvent;

public class InputExecutor {
    private final Robot robot;

    public InputExecutor() throws AWTException {
        this.robot = new Robot();
    }

    /**
     * Выполняет команду с переданными параметрами.
     * @param command Тип команды (из enum Command)
     * @param params Параметры команды:
     *               - Для MOUSE_MOVE: [x, y]
     *               - Для MOUSE_PRESS/RELEASE: [button]
     *               - Для KEY_PRESS/RELEASE: [keyCode]
     *               - Для MOUSE_WHEEL: [wheelAmt]
     */
    public void executeCommand(Command command, int... params) {
        try {
            switch (command) {
                case MOUSE_MOVE:
                    if (params.length >= 2) {
                        robot.mouseMove(params[0], params[1]);
                    }
                    break;

                case MOUSE_PRESS:
                    if (params.length >= 1) {
                        robot.mousePress(getMouseButtonMask(params[0]));
                    }
                    break;

                case MOUSE_RELEASE:
                    if (params.length >= 1) {
                        robot.mouseRelease(getMouseButtonMask(params[0]));
                    }
                    break;

                case KEY_PRESS:
                    if (params.length >= 1) {
                        robot.keyPress(params[0]);
                    }
                    break;

                case KEY_RELEASE:
                    if (params.length >= 1) {
                        robot.keyRelease(params[0]);
                    }
                    break;

                case MOUSE_WHEEL:
                    if (params.length >= 1) {
                        robot.mouseWheel(params[0]);
                    }
                    break;

                default:
                    System.err.println("неизвестная команда " + command);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("ошибка выполнения команды " + command + " " + e.getMessage());
        }
    }
    /**
     * Преобразует номер кнопки мыши в маску для Robot.
     * 1 - левая, 2 - средняя, 3 - правая.
     */
    private int getMouseButtonMask(int button) {
        switch (button) {
            case 1: return InputEvent.BUTTON1_DOWN_MASK;
            case 2: return InputEvent.BUTTON2_DOWN_MASK;
            case 3: return InputEvent.BUTTON3_DOWN_MASK;
            default: return InputEvent.BUTTON1_DOWN_MASK;
        }
    }
}
