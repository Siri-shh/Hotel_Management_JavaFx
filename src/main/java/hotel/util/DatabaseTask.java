package hotel.util;

import javafx.concurrent.Task;
import java.util.function.Supplier;

/**
 * Generic JavaFX Task that executes a database operation on a BACKGROUND THREAD,
 * keeping the JavaFX Application Thread (UI thread) responsive.
 *
 * How JavaFX threading works:
 *   - All UI updates MUST happen on the "JavaFX Application Thread".
 *   - Long-running work (DB queries, file I/O) should run on a separate thread
 *     so the UI does not freeze or become unresponsive.
 *   - This class extends Task<T>, which handles the thread boundary:
 *       call()           → runs on background thread
 *       setOnSucceeded() → runs back on FX Application Thread automatically
 *       setOnFailed()    → runs on FX Application Thread on error
 *
 * Usage example:
 *   DatabaseTask<List<Room>> task = new DatabaseTask<>(() -> roomDAO.getAllRooms());
 *   task.setOnSucceeded(e -> roomList.setAll(task.getValue()));  // FX thread safe
 *   task.setOnFailed(e -> System.err.println("Load failed: " + task.getException()));
 *
 *   Thread t = new Thread(task);
 *   t.setDaemon(true);   // daemon thread: won't block JVM shutdown
 *   t.setName("DB-RoomLoader");
 *   t.start();
 */
public class DatabaseTask<T> extends Task<T> {

    private final Supplier<T> dbOperation;

    public DatabaseTask(Supplier<T> dbOperation) {
        this.dbOperation = dbOperation;
    }

    @Override
    protected T call() {
        // This method runs on the background thread — NOT the FX thread
        System.out.println("[Thread: " + Thread.currentThread().getName()
                + " | ID: " + Thread.currentThread().threadId()
                + "] Executing DB operation...");
        return dbOperation.get();
    }

    /**
     * Convenience factory: creates task, wires handlers, starts a named daemon thread.
     *
     * @param op        the database operation (runs on background thread)
     * @param onSuccess handler called with result on the FX thread
     * @param threadName meaningful name for debugging (visible in thread dumps)
     */
    public static <T> DatabaseTask<T> startTask(
            Supplier<T> op,
            java.util.function.Consumer<T> onSuccess,
            String threadName) {

        DatabaseTask<T> task = new DatabaseTask<>(op);

        task.setOnSucceeded(e -> {
            System.out.println("[FX Thread] Task '" + threadName + "' finished. Updating UI.");
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e ->
            System.err.println("[FX Thread] Task '" + threadName + "' failed: "
                    + task.getException().getMessage())
        );

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.setName(threadName);
        t.start();

        return task;
    }
}
