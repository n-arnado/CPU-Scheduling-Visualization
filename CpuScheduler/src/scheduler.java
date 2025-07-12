/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.*;
/**
 *
 * @author Nj Arnado
 */
public class scheduler {
    public void fcfs(List<Process> processes) {
        // implementation
        
        //Sorting by arrival time
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        
        int currentTime = 0;
        List<String> ganttChart = new ArrayList<>();
        
        for (Process p : processes){
            if (currentTime < p.arrivalTime){
                currentTime = p.arrivalTime; //The Cpu is idle until it processes
            }
            p.startTime - currentTime;
            currentTime += p.burstTime;
            p,completionTime = currentTime;
            
            ganttChart.add("| " + p.processId + " ");
    
            
            
        }
        ganttChart.add("| ");
        displayGanntChart(ganttChart);
    }

    public void sjf(List<Process> processes) {
        // implementation
    }

    public void srtf(List<Process> processes) {
        // implementation
    }

    public void roundRobin(List<Process> processes, int timeQuantum) {
        // implementation
    }
}
