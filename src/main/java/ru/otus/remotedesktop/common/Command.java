package ru.otus.remotedesktop.common;

import java.io.Serializable;

public enum Command implements Serializable {
    MOUSE_MOVE,
    MOUSE_PRESS,
    MOUSE_RELEASE,
    KEY_PRESS,
    KEY_RELEASE,
    MOUSE_WHEEL,
    SET_STREAM_PARAMS,
    STREAM_CONTROL
}
