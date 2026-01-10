package ru.otus.remotedesktop;

import ru.otus.remotedesktop.server.OpenCVLoader;

public class SimpleTestOpenCV {
    public static void main(String[] args) {
        System.out.println("=== Тест локального OpenCV ===");

        try {
            // Загружаем OpenCV
            OpenCVLoader.load();

            System.out.println("OpenCV version: " + OpenCVLoader.getVersion());

            // Простой тест с матрицей
            org.opencv.core.Mat mat = org.opencv.core.Mat.eye(3, 3, org.opencv.core.CvType.CV_8UC1);
            System.out.println("Test matrix created successfully");
            System.out.println("Matrix:\n" + mat.dump());

            // Тест сжатия изображения
            org.opencv.core.Mat testImage = new org.opencv.core.Mat(
                    100, 100, org.opencv.core.CvType.CV_8UC3,
                    new org.opencv.core.Scalar(255, 0, 0) // Синий цвет (BGR)
            );

            org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
            org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", testImage, mob);

            System.out.println("Image compressed: " + mob.toArray().length + " bytes");

            testImage.release();
            mob.release();

            System.out.println("\n=== OpenCV работает корректно! ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
