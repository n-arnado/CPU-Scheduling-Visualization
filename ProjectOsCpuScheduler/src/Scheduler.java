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
        List<Process> completed = new ArrayList<>();
        List<String> ganttChart = new ArrayList<>();
        int currentTime = 0;
        int completedCount = 0;
        int n = processes.size();

        // To avoid modifying the original list
        List<Process> remaining = new ArrayList<>(processes);

            while (completedCount < n) {
            // Get all arrived and not yet completed processes
            List<Process> available = new ArrayList<>();
             for (Process p : remaining) {
                if (p.getArrivalTime() <= currentTime) {
                available.add(p);
            }
        }

        // Sort available processes by shortest burst time
        available.sort(Comparator.comparingInt(Process::getBurstTime));

        if (available.isEmpty()) {
            ganttChart.add("| IDLE ");
            currentTime++; // No process ready, CPU idle
            continue;
        }

        Process current = available.get(0);
        remaining.remove(current); // Mark as being scheduled

        current.setStartTime(currentTime);
        currentTime += current.getBurstTime();
        current.setCompletionTime(currentTime);
        completed.add(current);
        ganttChart.add("| " + current.getProcessId() + " ");

        completedCount++;
    }

    ganttChart.add("|");
    displayGanttChart(ganttChart);
    calculateMetrics(completed);
    }

    public void srtf(List<Process> processes) {
        List<String> ganttChart = new ArrayList<>();
    int currentTime = 0;
    int completed = 0;
    int n = processes.size();
    Process current = null;

    // To keep track of executed processes
    List<Process> remaining = new ArrayList<>();
    for (Process p : processes) {
        remaining.add(new Process(p.getProcessId(), p.getArrivalTime(), p.getBurstTime()));
    }

    while (completed < n) {
        // Find available processes
        List<Process> available = new ArrayList<>();
        for (Process p : remaining) {
            if (p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0) {
                available.add(p);
            }
        }

        if (available.isEmpty()) {
            ganttChart.add("| IDLE ");
            currentTime++;
            continue;
        }

        // Choose process with shortest remaining time
        available.sort(Comparator.comparingInt(Process::getRemainingTime));
        Process exec = available.get(0);

        // Set start time only once
        if (exec.getStartTime() == -1) {
            exec.setStartTime(currentTime);
        }

        exec.setRemainingTime(exec.getRemainingTime() - 1);
        ganttChart.add("| " + exec.getProcessId() + " ");
        currentTime++;

        if (exec.getRemainingTime() == 0) {
            exec.setCompletionTime(currentTime);
            completed++;

            // Replace original process info
            for (Process p : processes) {
                if (p.getProcessId().equals(exec.getProcessId())) {
                    p.setStartTime(exec.getStartTime());
                    p.setCompletionTime(exec.getCompletionTime());
                    p.setRemainingTime(0);
                    break;
                }
            }
        }
    }

    ganttChart.add("|");
    displayGanttChart(ganttChart);
    calculateMetrics(processes);
        
    }

    public void roundRobin(List<Process> processes, int timeQuantum) {
         Queue<Process> queue = new LinkedList<>();
    List<Process> ready = new ArrayList<>(processes); // copy for sorting
    List<String> ganttChart = new ArrayList<>();

    // Sort by arrival time
    ready.sort(Comparator.comparingInt(Process::getArrivalTime));

    int currentTime = 0;
    int completed = 0;
    int index = 0;
    int n = ready.size();

    // Add first arriving process(es) to queue
    while (index < n && ready.get(index).getArrivalTime() <= currentTime) {
        queue.add(ready.get(index));
        index++;
    }

    while (!queue.isEmpty()) {
        Process current = queue.poll();

        if (current.getStartTime() == -1) {
            current.setStartTime(currentTime);
        }

        ganttChart.add("| " + current.getProcessId() + " ");

        int executeTime = Math.min(timeQuantum, current.getRemainingTime());
        currentTime += executeTime;
        current.setRemainingTime(current.getRemainingTime() - executeTime);

        // Add newly arrived processes to queue
        while (index < n && ready.get(index).getArrivalTime() <= currentTime) {
            queue.add(ready.get(index));
            index++;
        }

        if (current.getRemainingTime() > 0) {
            queue.add(current); // re-add if not finished
        } else {
            current.setCompletionTime(currentTime);
            completed++;
        }

        if (queue.isEmpty() && completed < n && index < n) {
            // CPU is idle until next process arrives
            currentTime = ready.get(index).getArrivalTime();
            queue.add(ready.get(index));
            index++;
        }
    }

    ganttChart.add("|");
    displayGanttChart(ganttChart);
    calculateMetrics(processes);
    }
    
    public void mlfq(List<Process> processes, int[] timeQuantum, int[] allotments){
         int currentTime = 0;
    int completed = 0;
    int n = processes.size();
    List<String> ganttChart = new ArrayList<>();

    // Priority Queues: Q0 (highest) to Q3 (lowest)
    List<Queue<Process>> queues = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
        queues.add(new LinkedList<>());
    }

    // Clone and sort by arrival time
    List<Process> pending = new ArrayList<>(processes);
    pending.sort(Comparator.comparingInt(Process::getArrivalTime));
    int nextIndex = 0;

    // Track each process’s level and time spent in current level
    Map<Process, Integer> level = new HashMap<>();
    Map<Process, Integer> spent = new HashMap<>();

    while (completed < n) {
        // Add arriving processes to Q0
        while (nextIndex < n && pending.get(nextIndex).getArrivalTime() <= currentTime) {
            Process p = pending.get(nextIndex++);
            queues.get(0).add(p);
            level.put(p, 0);
            spent.put(p, 0);
        }

        boolean executed = false;

        for (int i = 0; i < 4; i++) {
            Queue<Process> q = queues.get(i);

            if (!q.isEmpty()) {
                Process current = q.poll();
                int tq = timeQuanta[i];
                int allot = allotments[i];

                if (current.getStartTime() == -1) {
                    current.setStartTime(currentTime);
                }

                ganttChart.add("| " + current.getProcessId() + "(Q" + i + ") ");

                int runTime = Math.min(tq, current.getRemainingTime());
                currentTime += runTime;
                int newRemaining = current.getRemainingTime() - runTime;
                current.setRemainingTime(newRemaining);
                spent.put(current, spent.get(current) + runTime);

                // Add new arrivals during execution
                while (nextIndex < n && pending.get(nextIndex).getArrivalTime() <= currentTime) {
                    Process p = pending.get(nextIndex++);
                    queues.get(0).add(p);
                    level.put(p, 0);
                    spent.put(p, 0);
                }

                if (newRemaining <= 0) {
                    current.setCompletionTime(currentTime);
                    completed++;
                } else {
                    if (spent.get(current) >= allot && i < 3) {
                        int nextLevel = i + 1;
                        queues.get(nextLevel).add(current);
                        level.put(current, nextLevel);
                        spent.put(current, 0);
                    } else {
                        queues.get(i).add(current);
                    }
                }

                executed = true;
                break; // Execute only one process per tick
            }
        }

        if (!executed) {
            // CPU is idle
            ganttChart.add("| IDLE ");
            currentTime++;
        }
    }

    ganttChart.add("|");
    displayGanttChart(ganttChart);
    calculateMetrics(processes);
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
