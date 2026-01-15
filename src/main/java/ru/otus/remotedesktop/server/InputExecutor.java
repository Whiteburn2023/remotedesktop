package ru.otus.remotedesktop.server;

import ru.otus.remotedesktop.common.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.InputEvent;

public class InputExecutor {
    private static final Logger logger = LoggerFactory.getLogger(InputExecutor.class);
    private final Robot robot;

    public InputExecutor() throws AWTException {
        this.robot = new Robot();
    }

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
                    logger.warn("неизвестная команда {}", command);
            }
        } catch (IllegalArgumentException e) {
            logger.error("ошибка выполнения команды {}: {}", command, e.getMessage());
        }
    }
    /**
     * Преобразует номер кнопки мыши в маску для Robot.
     * 1 - левая, 2 - средняя, 3 - правая.
     */
    private int getMouseButtonMask(int button) {
        return switch (button) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }
}
