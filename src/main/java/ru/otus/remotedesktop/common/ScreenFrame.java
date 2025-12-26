package ru.otus.remotedesktop.common;

import java.io.Serializable;

public class ScreenFrame implements Serializable {
    private byte[] imageData;  //Сжатое изображение jpeg
    private int width;
    private int height;
    private int cursorX;
    private int cursorY;

    public ScreenFrame() {
    }

    public ScreenFrame(byte[] imageData, int width, int height, int cursorX, int cursorY) {
        this.imageData = imageData;
        this.width = width;
        this.height = height;
        this.cursorX = cursorX;
        this.cursorY = cursorY;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCursorX() {
        return cursorX;
    }

    public void setCursorX(int cursorX) {
        this.cursorX = cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    public void setCursorY(int cursorY) {
        this.cursorY = cursorY;
    }
}
