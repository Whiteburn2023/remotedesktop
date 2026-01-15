package ru.otus.remotedesktop.common;

public class StreamParams {

    private int width;        // Ширина видео
    private int height;       // Высота видео
    private int fps;          // Частота кадров
    private int bitrate;      // Битрейт (бит/с)
    private String codec;     // Кодек (h264, h265, vp8)
    private boolean useHWAccel; // Использовать аппаратное ускорение

    public StreamParams() {
    }

    public StreamParams(int width, int height, int fps, int bitrate, String codec, boolean useHWAccel) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrate = bitrate;
        this.codec = codec;
        this.useHWAccel = useHWAccel;
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

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public boolean isUseHWAccel() {
        return useHWAccel;
    }

    public void setUseHWAccel(boolean useHWAccel) {
        this.useHWAccel = useHWAccel;
    }
}
