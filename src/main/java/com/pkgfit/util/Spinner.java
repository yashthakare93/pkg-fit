package com.pkgfit.util;

import java.util.function.Supplier;

public class Spinner {

    private static final String[] FRAMES = { "\u280B", "\u2819", "\u2839", "\u2838", "\u283C", "\u2834", "\u2826", "\u2827", "\u2807", "\u280F" };
    private static volatile Thread currentThread;

    public static void start(String message) {
        stop();
        currentThread = new Thread(() -> {
            int i = 0;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.err.print("\r" + FRAMES[i % FRAMES.length] + " " + message);
                    i++;
                    Thread.sleep(80);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        currentThread.setDaemon(true);
        currentThread.start();
    }

    public static void stop() {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
            try {
                currentThread.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.err.print("\r\u001B[K");
            System.err.flush();
            currentThread = null;
        }
    }

    public static <T> T run(String message, Supplier<T> task) {
        start(message);
        try {
            return task.get();
        } finally {
            stop();
        }
    }
}
