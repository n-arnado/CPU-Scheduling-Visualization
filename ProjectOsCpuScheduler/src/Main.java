/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.*;
/**
 *
 * @author Nj Arnado
 */
public class Main {
    public static void main(String[]args ){
        Scanner scanner = new Scanner(System.in);
        Scheduler scheduler = new Scheduler();
        List<Process> processes = new ArrayList<>();
        
        System.out.print("Enter numberof processes: ");
        int n = scanner.nextInt();
        
        for (int i = 0; i < n; i++){
            System.out.println("\n Process" + (i + 1));
            System.out.print("Process ID: ");
            String pid = scanner.next();
            System.out.print("Arrival Time: ");
            int at = scanner.nextInt();
            System.out.print("Burst Time: ");
            int bt = scanner.nextInt();
            processes.add(new Process(pid, at, bt));
        }
        
        System.out.println("\n Scheduling Algorithm: ");
        System.out.println("1. First-In First-Out (FIFO/FCFS)");
        System.out.println("2. Shortest Job First - Non-Preemptive");
        System.out.println("3. Shortest Remaining Time (STRF) - Preemptive");
        System.out.println("4. Round Robin");
        System.out.println ("5. Multilevel Feedback Queue (MLFQ");
        System.out.print("Choice: ");
        int choice = scanner.nextInt();
        
        switch (choice){
            case 1: 
                scheduler.fcfs(cloneProcessList(processes));
                break;
            case 2:
                scheduler.sjf(cloneProcessList(processes));
                break;
            case 3: 
                scheduler.srtf(cloneProcessList(processes));
                break;
            case 4:
                System.out.print("Enter Time Quantum: ");
                int tq = scanner.nextInt();
                scheduler.roundRobin(cloneProcessList(processes), tq);
                break;
            case 5:
                System.out.print("Enter time slices for Q0-Q3 (comma separated: ");
                scanner.nextInt();
                String[] times = scanner.next().split(",");
                int[] quanta = Arrays.stream(times).mapToInt(Integer::parseInt).toArray();
                if (quanta.length != 4) {
                    System.out.println("Error. You must enter exactly 4 time quantums for Q0–Q3.");
                    return;
                }

                int[] allotments = {8, 12, 16, 1000}; // default allotments per level
                scheduler.mlfq(cloneProcessList(processes), quanta, allotments);
                break;
            default:
                System.out.print("Invalid choice!");
        }
        
        scanner.close();
        
        
    }
    //helper to deep clean list it actually prevents to modifying the original input list
    private static List<Process> cloneProcessList(List<Process> original){
        List<Process>  cloned = new ArrayList<>();
        for (Process p : original){
        cloned.add(new Process(p.getProcessId(),p.getArrivalTime(),p.getBurstTime()));
    }
        return cloned;
    }
    
}
