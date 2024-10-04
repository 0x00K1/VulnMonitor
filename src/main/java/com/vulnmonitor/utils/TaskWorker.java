package com.vulnmonitor.utils;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * A reusable utility class for performing background operations using SwingWorker.
 * This class allows you to pass in tasks to be executed in the background and callbacks for handling results and errors.
 *
 * @param <T> The result type of the background computation.
 * @param <V> The intermediate result type, if needed for processing chunks.
 */
public class TaskWorker<T, V> extends SwingWorker<T, V> {

    private final Callable<T> backgroundTask;
    private final Consumer<T> onSuccess;
    private final Consumer<Exception> onFailure;

    /**
     * Constructs a new TaskWorker.
     *
     * @param backgroundTask The task to be executed in the background (non-UI thread).
     * @param onSuccess      The callback to be executed upon successful completion of the background task.
     * @param onFailure      The callback to be executed if an exception occurs during the background task.
     */
    public TaskWorker(Callable<T> backgroundTask, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        this.backgroundTask = backgroundTask;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    protected T doInBackground() throws Exception {
        return backgroundTask.call();
    }

    @Override
    protected void done() {
        try {
            T result = get(); // Retrieve the result of the background task
            onSuccess.accept(result);
        } catch (Exception e) {
            onFailure.accept(e); // Handle any exceptions that occurred during the background task
        }
    }
}