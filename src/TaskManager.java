import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {
    private static final Queue<TaskLogEntry> taskLog = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger Idcounter = new AtomicInteger(1);

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        Set<String> validInputs = Set.of("create", "save", "download", "print");

        ExecutorService executor = Executors.newFixedThreadPool(4);

        while (true) {
            System.out.println();
            System.out.println("""
                    ====== Task Queue Manager ======
                    1. Add Task
                    2. View Task History
                    3. Exit
                    ================================
                    Enter your choice:""");
            String choice = s.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.println("Enter a task (create, download, save, print):");
                    String input = s.nextLine().trim().toLowerCase();

                    if (!validInputs.contains(input)) {
                        System.out.println("Invalid task type! Try again.");
                        continue;
                    }

                    String content = "";
                    String filename;

                    System.out.println("Enter file name:");
                    filename = s.nextLine().trim();

                    if (input.equals("create") || input.equals("save")) {
                        System.out.println("Enter content:");
                        content = s.nextLine();
                    }

                    int taskID = Idcounter.getAndIncrement();
                    taskLog.add(new TaskLogEntry(taskID, input, filename, "SUBMITTED"));
                    Future<?> future = executor.submit(new Task(input, content, filename, taskID));
                    try{
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Task failed: "+e.getMessage());
                    }
                }

                case "2" -> {
                    System.out.println("======== Task History =========");
                    if (taskLog.isEmpty()) {
                        System.out.println("No tasks submitted yet.");
                    } else {
                        System.out.println("""
    ===============================================
    | Task ID | Task Type |      File Name       |   Status   |
    ===============================================""");

                        for (TaskLogEntry entry : taskLog) {
                            System.out.printf("| %-7d | %-9s | %-20s | %-10s |%n",
                                    entry.taskID,
                                    entry.taskType.toUpperCase(),
                                    entry.filename,
                                    entry.status);
                        }System.out.println("===============================================");

                    }
                }

                case "3" -> {
                    System.out.println("Shutting down Task Manager...");
                    executor.shutdown();
                    try {
                        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                    s.close();
                    return; // Exit the method after shutdown
                }

                default -> System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    public static class Task implements Runnable {
        private final int taskID;
        private final String input;
        private final String content;
        private final String filename;

        public Task(String input, String content, String filename, int taskID) {
            this.input = input;
            this.content = content;
            this.filename = filename;
            this.taskID = taskID;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            System.out.printf("""
                    --------------------------------
                    Task #%d started on %s
                    Task: %s
                    File: %s
                    --------------------------------
                    %n""", taskID, threadName, input, filename);

            System.out.println("Started: " + input);

            switch (input) {
                case "download" -> simulateDownload();
                case "print" -> simulatePrinting();
                case "save" -> simulateSaving();
                case "create" -> writeToFile();
            }

            System.out.println("Finished: " + input);
            updateTaskStatus(taskID, "COMPLETED");
        }

        private void simulateDownload() {
            System.out.println("Downloading...");
            sleep(1000);
        }

        private void simulatePrinting() {
            System.out.println("Printing...");
            readFromFile();
            sleep(1000);
        }

        private void simulateSaving() {
            writeToFile();
            System.out.println("Saving...");
            sleep(1000);
        }

        private synchronized void writeToFile() {
            System.out.println("Writing...");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
                writer.write(content);
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Write failed: " + e.getMessage());
            }
        }

        private synchronized void readFromFile() {
            System.out.println("Reading...");
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                System.out.println("====== Contents of " + filename + " ======");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("Read failed: " + e.getMessage());
            } finally {
                System.out.println("====== End of file ======");
            }
        }

        private void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void updateTaskStatus(int taskID, String newStatus) {
            for (TaskLogEntry entry : taskLog) {
                if (entry.taskID == taskID) {
                    entry.status = newStatus;
                    break;
                }
            }
        }
    }

    public static class TaskLogEntry {
        final int taskID;
        final String taskType;
        final String filename;
        volatile String status;

        public TaskLogEntry(int taskID, String taskType, String filename, String status) {
            this.taskID = taskID;
            this.taskType = taskType;
            this.filename = filename;
            this.status = status;
        }
    }
}
