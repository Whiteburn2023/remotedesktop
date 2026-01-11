package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class OpenCVLoader {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVLoader.class);
    private static boolean loaded = false;

    public static void load() {
        if (!loaded) {
            synchronized (OpenCVLoader.class) {
                if (!loaded) {
                    try {
                        // Путь к native библиотекам в проекте (указываем относительно проекта)
                        String projectPath = System.getProperty("user.dir");
                        String nativePath = projectPath + File.separator + "lib" + File.separator + "native";

                        logger.info("Trying to load OpenCV from: {}", nativePath);

                        // Проверяем наличие папки
                        File nativeDir = new File(nativePath);
                        if (!nativeDir.exists()) {
                            logger.warn("Native directory not found: {}", nativePath);
                            // Пробуем альтернативный путь
                            nativePath = projectPath + File.separator + "target" + File.separator + "lib" + File.separator + "native";
                            nativeDir = new File(nativePath);
                        }

                        if (nativeDir.exists()) {
                            // Для Windows загружаем основные библиотеки
                            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                // Минимальный набор для работы с изображениями
                                String[] libs = {
                                        "opencv_core481.dll",
                                        "opencv_imgproc481.dll",
                                        "opencv_imgcodecs481.dll",
                                        "opencv_java481.dll"
                                };

                                for (String lib : libs) {
                                    File libFile = new File(nativeDir, lib);
                                    if (libFile.exists()) {
                                        System.load(libFile.getAbsolutePath());
                                        logger.debug("Loaded: {}", lib);
                                    } else {
                                        logger.warn("Library not found: {}", libFile.getAbsolutePath());
                                    }
                                }
                            } else {
                                // Для других ОС пробуем загрузить основную библиотеку
                                String libName;
                                if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                                    libName = "libopencv_java481.so";
                                } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                                    libName = "libopencv_java481.dylib";
                                } else {
                                    libName = "opencv_java481.dll";
                                }

                                File libFile = new File(nativeDir, libName);
                                if (libFile.exists()) {
                                    System.load(libFile.getAbsolutePath());
                                }
                            }

                            // Проверяем, что OpenCV загружен
                            String version = org.opencv.core.Core.VERSION;
                            logger.info("OpenCV successfully loaded. Version: {}", version);
                            loaded = true;

                        } else {
                            // Пробуем загрузить из системного пути
                            logger.info("Native directory not found, trying system library path...");
                            System.loadLibrary("opencv_java481");
                            logger.info("OpenCV loaded from system library path");
                            loaded = true;
                        }

                    } catch (UnsatisfiedLinkError e) {
                        logger.error("Failed to load OpenCV: {}", e.getMessage());
                        logger.warn("OpenCV not available, will use ImageIO fallback");
                        loaded = false;
                    }
                }
            }
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String getVersion() {
        if (loaded) {
            return org.opencv.core.Core.VERSION;
        }
        return "OpenCV not loaded";
    }
}
