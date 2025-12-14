package com.netflixplus.processing;

import java.util.Map;
import java.util.concurrent.*;

public class VideoProcessor {

    private static final ExecutorService executor =
            Executors.newFixedThreadPool(8);

    private static final Map<String, Process> runningProcesses =
            new ConcurrentHashMap<>();

    public static void submit(String movieId, Runnable task) {
        executor.submit(task);
    }

    public static void registerProcess(String movieId, Process process) {
        runningProcesses.put(movieId, process);
    }

    public static void cancel(String movieId) {
        Process p = runningProcesses.get(movieId);
        if (p != null) {
            p.destroyForcibly();
            runningProcesses.remove(movieId);
        }
    }

    public static void done(String movieId) {
        runningProcesses.remove(movieId);
    }
}