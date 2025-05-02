import java.io.*;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class TaskManager {
    private static final AtomicInteger Idcounter = new AtomicInteger(1);
    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        Set<String> validInputs = Set.of("create", "save", "download", "print","exit");
        System.out.println("====== Task Queue Manager ======");
        ExecutorService executor = Executors.newFixedThreadPool(4);
        while(true) {
            System.out.println("Enter a task (create,download,save,print) or exit to close:");
            String input = s.nextLine().trim().toLowerCase();
            String content = "";
            String filename="";
            if (input.equalsIgnoreCase("exit")) {
                break;
            }
            if (input.isEmpty() || !validInputs.contains(input.toLowerCase())) {
                System.out.println("Invalid! Try again.");
                continue;
            }else if (input.equals("create")
                    ||input.equals("save")) {
                System.out.println("Enter file name:");
                filename= s.nextLine();
                System.out.println("Enter a content:");
                content = s.nextLine();
            }
            else if(input.equals("download")||
                    input.equals("print")){
                System.out.println("Enter file name:");
                filename= s.nextLine().trim();            }


            int taskID =Idcounter.getAndIncrement();
            executor.execute(new Task(input,content,filename,taskID));
        }
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        s.close();
    }

    public static class Task implements Runnable {
        private final int taskID;
        private final String input;
        private final String content;
        private final String filename;
       public  Task (String input,String content,String filename,int taskID){
            this.input = input;
            this.content= content;
            this.taskID = taskID;
            this.filename = filename;
       }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            System.out.println("""
            --------------------------------
            Task #%d started on %s
            Task: %s
            File: %s
            Active Tasks: %d
            --------------------------------
            """.formatted(taskID, threadName, input, filename,taskID));
            System.out.println("Started: "+input);
            switch(input.toLowerCase()){
                case "download" -> stimulateDownload();
                case "print"->stimulatePrinting();
                case "save"->stimulateSaving(content);
                case "create"->WriteToFile(content);
            }
            System.out.println("Finished: "+input);

        }

        public void stimulateDownload(){
            System.out.println("File downloading....");
            sleep(1000);
        }
        public void stimulatePrinting(){
            System.out.println("Printing....");
            ReadFromFile();
            sleep(1000);
        }
        public void stimulateSaving(String content){
            WriteToFile(content);
           System.out.println("Saving....");
            sleep(1000);
        }
        public synchronized void WriteToFile(String content){
            System.out.println("Writing....");
            try(BufferedWriter writer =new BufferedWriter(new FileWriter(filename,true))){
                writer.write(content);
                writer.newLine();
            } catch (IOException e) {
                System.out.println("Logging failed: "+e.getMessage());
            }
        }
        public synchronized void ReadFromFile(){
            System.out.println("Reading....");
            try(BufferedReader reader =new BufferedReader(new FileReader(filename))){
                String line;
                System.out.println("====== Contents of "+filename+" ======");
                while((line = reader.readLine())!=null){
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("File reading failed: "+e.getMessage());
            }
            finally {
                System.out.println("====== End of file ======");
            }
        }
        public void sleep(int ms){
            try{
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
