package ru.otus.remotedesktop.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCVLoader {
    private static final Logger logger = LoggerFactory.getLogger(OpenCVLoader.class);
    private static boolean loaded = false;

    public static void load() {
        if (!loaded) {
            synchronized (OpenCVLoader.class) {
                if (!loaded) {
                    try {
                        // Основной путь к OpenCV в проекте
                        String projectPath = System.getProperty("user.dir");
                        String[] dllPaths = {
                                projectPath + "\\opencv\\build\\java\\x64\\opencv_java481.dll",
                                projectPath + "\\opencv\\build\\x64\\vc16\\bin\\opencv_java481.dll",
                                "C:\\opencv\\build\\java\\x64\\opencv_java481.dll",
                                "C:\\opencv\\build\\x64\\vc16\\bin\\opencv_java481.dll"
                        };

                        boolean success = false;
                        for (String path : dllPaths) {
                            try {
                                System.load(path);
                                logger.info("Successfully loaded OpenCV from: {}", path);
                                success = true;
                                break;
                            } catch (UnsatisfiedLinkError e) {
                                logger.debug("Failed to load from {}: {}", path, e.getMessage());
                            }
                        }

                        if (!success) {
                            // Последняя попытка - из системного пути
                            System.loadLibrary("opencv_java481");
                            logger.info("OpenCV loaded from system library path");
                        }

                        // Проверка версии
                        String version = org.opencv.core.Core.VERSION;
                        logger.info("OpenCV version: {}", version);

                        loaded = true;

                    } catch (UnsatisfiedLinkError e) {
                        logger.error("Failed to load OpenCV: {}", e.getMessage());
                        throw new RuntimeException(
                                "OpenCV 4.8.1 not found. Please ensure:\n" +
                                        "1. OpenCV 4.8.1 is installed\n" +
                                        "2. opencv_java481.dll is in PATH or project directory\n" +
                                        "3. Visual C++ Redistributable is installed", e);
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
