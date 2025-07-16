/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.*;
/**
 *
 * @author Nj Arnado
 */
public class Scheduler {
    public void fcfs(List<Process> processes) {
     //Sorting by arrival time
        processes.sort(Comparator.comparingInt(p -> p.getArrivalTime()));
        
        int currentTime = 0;
        List<String> ganttChart = new ArrayList<>();
        
        for (Process p : processes){
            if (currentTime < p.getArrivalTime()){
                currentTime = p.getArrivalTime(); //The Cpu is idle until it processes
            }
            p.setStartTime(currentTime);
            currentTime += p.getBurstTime();
            p.setCompletionTime(currentTime);
            
            ganttChart.add("| " + p.getProcessId() + " ");
    
            
            
        }
        ganttChart.add("| ");
        displayGanttChart(ganttChart);
        calculateMetrics(processes);
    }

    public void sjf(List<Process> processes) {
      
    }

    public void srtf(List<Process> processes) {
        // implementation
        
    }

    public void roundRobin(List<Process> processes, int timeQuantum) {
        // implementation
    }

    public void calculateMetrics(List<Process>process) {
    double totalTat = 0, totalRT = 0;

    for (Process p : process) {
        int tat = p.getCompletionTime() - p.getArrivalTime();
        int rt = p.getStartTime() - p.getArrivalTime();
        
        p.setTurnaroundTime(tat);
        p.setResponseTime(rt);
        
        totalTat +=  tat;
        totalRT += rt;
    }

        System.out.printf("%-7s %-10s %-8s %15s %15s %13s\n", "PID", "Arrival", "Burst", "Completion", "Turnarround", "Response");

        for (Process p : process){
            p.display();
        }
        System.out.printf("\n Average Turnaround Time: %.2f\n", totalTat/process.size());
        System.out.printf("\n Average Response Time: %.2f\n", totalRT/process.size());
    }
    
    private void displayGanttChart(List<String> chart){
        System.out.println("\n Gantt Chart:");
        for (String entry : chart){
            System.out.print(entry);
        }
        System.out.println("\n");
    }
}
