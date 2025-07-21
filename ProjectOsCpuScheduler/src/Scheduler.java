package srcs;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.*;
import java.util.stream.Collectors;package srcs;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.*;
import java.util.stream.Collectors;
/**
 *
 * @author Nj Arnado
 */

interface SchedulerListener {
    
    void onTick(int currentTime, String cpuActivity, List<String> ganttChartOutput, List<Queue<Process>> currentQueues, List<Process> currentProcessStates);

    
    void onSimulationComplete(List<Process> calculatedProcesses, List<String> ganttChartOutput, double avgTurnaroundTime, double avgResponseTime, double avgWaitingTime);

   
    void onReset();
}

public class Scheduler {

    private List<String> ganttChartOutput;
    private double avgTurnaroundTime;
    private double avgResponseTime;
    private double avgWaitingTime;
    private List<Process> calculatedProcesses; 

    private SchedulerListener listener;

    // --- Simulation State Variables ---
    private int currentTime;
    private String currentCpuActivity;
    private List<Process> simulationProcesses; 
    private Queue<Process> fcfsQueue; 
    private Queue<Process> rrQueue; 
    private List<Queue<Process>> mlfqQueues; 
    private Map<Process, Integer> mlfqLevel; 
    private Map<Process, Integer> mlfqSpent; 
    private int rrTimeQuantum;
    private int[] mlfqTimeQuantums;
    private int[] mlfqAllotments;
    private int nextProcessToArriveIndex; 
    private Process currentExecutingProcess;
    private int contextSwitchRemaining; 

    private int contextSwitchDelay; 

    /**
     * Constructor for the Scheduler. 
     */
    public Scheduler() {
        reset(); // Initialize state
    }

    
    public void setListener(SchedulerListener listener) {
        this.listener = listener;
    }

    //Resets all
    public void reset() {
        ganttChartOutput = new ArrayList<>();
        avgTurnaroundTime = 0;
        avgResponseTime = 0;
        avgWaitingTime = 0;
        calculatedProcesses = new ArrayList<>();

        currentTime = 0;
        currentCpuActivity = "IDLE";
        simulationProcesses = new ArrayList<>();
        fcfsQueue = new LinkedList<>();
        rrQueue = new LinkedList<>();
        mlfqQueues = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            mlfqQueues.add(new LinkedList<>());
        }
        mlfqLevel = new HashMap<>();
        mlfqSpent = new HashMap<>();
        rrTimeQuantum = 0;
        mlfqTimeQuantums = new int[4];
        mlfqAllotments = new int[4];
        nextProcessToArriveIndex = 0;
        currentExecutingProcess = null;
        contextSwitchRemaining = 0; // No context switch initially

        if (listener != null) {
            listener.onReset(); 
        }
    }

    // --- Getters for Simulation Results ---
    public List<String> getGanttChartOutput() {
        return ganttChartOutput;
    }

    public double getAvgTurnaroundTime() {
        return avgTurnaroundTime;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public double getAvgWaitingTime() {
        return avgWaitingTime;
    }

    public List<Process> getCalculatedProcesses() {
        return calculatedProcesses;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public String getCurrentCpuActivity() {
        return currentCpuActivity;
    }


    public List<Queue<Process>> getCurrentQueues() {
        List<Queue<Process>> queuesCopy = new ArrayList<>();
        // For MLFQ, return all 4 queues
        for (Queue<Process> q : mlfqQueues) {
            queuesCopy.add(new LinkedList<>(q));
        }
        // For other algorithms, return the relevant queue as the first element
        // The GUI will interpret based on selected algorithm
        if (!fcfsQueue.isEmpty()) {
            queuesCopy.add(0, new LinkedList<>(fcfsQueue));
        } else if (!rrQueue.isEmpty()) {
            queuesCopy.add(0, new LinkedList<>(rrQueue));
        } else { // For SJF/SRTF, return a conceptual "ready list"
            List<Process> readyList = simulationProcesses.stream()
                .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0 && !calculatedProcesses.contains(p) && p != currentExecutingProcess)
                .collect(Collectors.toList());
            if (!readyList.isEmpty()) {
                // Sort for consistent display (e.g., by remaining time for SRTF, or burst for SJF)
                readyList.sort(Comparator.comparingInt(Process::getRemainingTime).thenComparingInt(Process::getArrivalTime));
                queuesCopy.add(0, new LinkedList<>(readyList)); // Add as a queue for consistent return type
            } else {
                 queuesCopy.add(0, new LinkedList<>()); // Add an empty queue if no ready processes
            }
        }
        return queuesCopy;
    }
    
  
    public void setProcesses(List<Process> processes, int contextSwitchDelay) {
        reset(); // ensuring clean slate 
        this.contextSwitchDelay = contextSwitchDelay;

        // deep copy processes to avoid modifying the original list for multiple runs
        for (Process p : processes) {
            this.simulationProcesses.add(new Process(p.getProcessId(), p.getArrivalTime(), p.getBurstTime()));
        }
        // sort processes by arrival time for all algorithms (important for initial queueing)
        this.simulationProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));
    }

    
    public void setRrTimeQuantum(int quantum) {
        this.rrTimeQuantum = quantum;
    }

    
    public void setMlfqParameters(int[] tqs, int[] allotments) {
        this.mlfqTimeQuantums = tqs;
        this.mlfqAllotments = allotments;
    }


    
    public boolean step(String algorithm) {
        
        boolean allProcessesCompleted = simulationProcesses.stream().allMatch(p -> p.getRemainingTime() <= 0);
        if (allProcessesCompleted && currentExecutingProcess == null && isAllQueuesEmpty() && nextProcessToArriveIndex >= simulationProcesses.size()) {
            ganttChartOutput.add("|"); 
            calculateMetrics(simulationProcesses); 
            if (listener != null) {
                listener.onSimulationComplete(calculatedProcesses, ganttChartOutput, avgTurnaroundTime, avgResponseTime, avgWaitingTime);
            }
            return false; 
        }
        
        // --- Handle Context Switching Delay ---
        if (contextSwitchRemaining > 0) {
            currentCpuActivity = "CS"; // Mark as Context Switch
            ganttChartOutput.add("| CS ");
            contextSwitchRemaining--;
            currentTime++;
            if (listener != null) {
                
                listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
            }
            return true; 
        }

     
        addArrivingProcessesToQueue(algorithm);

        // Determine next process to execute
        Process prevExecutingProcess = currentExecutingProcess; // Store previous to detect switch
        Process nextProcessToRun = selectNextProcess(algorithm);

       
        if (prevExecutingProcess != null && nextProcessToRun != null && prevExecutingProcess != nextProcessToRun && contextSwitchDelay > 0) {
            contextSwitchRemaining = contextSwitchDelay; 
            currentCpuActivity = "CS"; // Mark as Context Switch
            ganttChartOutput.add("| CS ");
            currentTime++; // Consume one time unit for the start of CS
            if (listener != null) {
                listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
            }
            return true; 
        }

        currentExecutingProcess = nextProcessToRun; 

        if (currentExecutingProcess != null) {
            if (currentExecutingProcess.getStartTime() == -1) {
                currentExecutingProcess.setStartTime(currentTime); // Set start time if first time running
            }
            currentExecutingProcess.setRemainingTime(currentExecutingProcess.getRemainingTime() - 1);
            currentCpuActivity = currentExecutingProcess.getProcessId();
            ganttChartOutput.add("| " + currentCpuActivity + getQueueIdentifier(algorithm, currentExecutingProcess) + " ");

            // For MLFQ, update spent time in current level
            if ("MLFQ".equals(algorithm)) {
                mlfqSpent.put(currentExecutingProcess, mlfqSpent.getOrDefault(currentExecutingProcess, 0) + 1);
            }

            // Check if process finished
            if (currentExecutingProcess.getRemainingTime() <= 0) {
                currentExecutingProcess.setCompletionTime(currentTime + 1); 
                removeFromAllQueues(currentExecutingProcess);
                currentExecutingProcess = null; 
            } else {
                // If not finished, put it back to queue if necessary (for pre-emptive)
                requeueProcess(algorithm, currentExecutingProcess);
                // If it was preempted by a higher priority process, it's no longer on CPU
                if (currentExecutingProcess != null && currentExecutingProcess != selectNextProcess(algorithm)) { 
                    currentExecutingProcess = null; // Preempted
                }
            }
        } else {
            // CPU is idle
            currentCpuActivity = "IDLE";
            ganttChartOutput.add("| IDLE ");
        }

        currentTime++; // Advance time

      
        if (listener != null) {
            listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
        }

       
        allProcessesCompleted = simulationProcesses.stream().allMatch(p -> p.getRemainingTime() <= 0);
        if (allProcessesCompleted && currentExecutingProcess == null && isAllQueuesEmpty() && nextProcessToArriveIndex >= simulationProcesses.size()) {
            ganttChartOutput.add("|"); 
            calculateMetrics(simulationProcesses); 
            if (listener != null) {
                listener.onSimulationComplete(calculatedProcesses, ganttChartOutput, avgTurnaroundTime, avgResponseTime, avgWaitingTime);
            }
            return false; 
        }
        return true; 
    }

    
    private void addArrivingProcessesToQueue(String algorithm) {
        while (nextProcessToArriveIndex < simulationProcesses.size() && 
               simulationProcesses.get(nextProcessToArriveIndex).getArrivalTime() <= currentTime) {
            Process p = simulationProcesses.get(nextProcessToArriveIndex);
            
            switch (algorithm) {
                case "FCFS":
                    fcfsQueue.add(p);
                    break;
                case "SJF": 
                case "SRTF":                
                    break;
                case "Round Robin":
                    rrQueue.add(p);
                    break;
                case "MLFQ":
                    mlfqQueues.get(0).add(p); // New arrivals go to highest priority queue (Q0)
                    mlfqLevel.put(p, 0);
                    mlfqSpent.put(p, 0);
                    break;
            }
            nextProcessToArriveIndex++;
        }
    }

   
    private Process selectNextProcess(String algorithm) {
        Process selected = null;
        switch (algorithm) {
            case "FCFS":
                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    return currentExecutingProcess; 
                }
                if (!fcfsQueue.isEmpty()) {
                    selected = fcfsQueue.peek(); 
                }
                break;
            case "SJF":
                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    return currentExecutingProcess;
                }
                List<Process> availableSJF = simulationProcesses.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .collect(Collectors.toList());
                if (!availableSJF.isEmpty()) {
                    availableSJF.sort(Comparator.comparingInt(Process::getBurstTime).thenComparingInt(Process::getArrivalTime));
                    selected = availableSJF.get(0);
                }
                break;
            case "SRTF":
                // Preemptive SRTF: always select the one with shortest remaining time
                List<Process> availableSRTF = simulationProcesses.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .collect(Collectors.toList());
                if (!availableSRTF.isEmpty()) {
                    availableSRTF.sort(Comparator.comparingInt(Process::getRemainingTime).thenComparingInt(Process::getArrivalTime));
                    selected = availableSRTF.get(0);
                }
                break;
            case "Round Robin":

                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    int quantumUsedInCurrentBurst = currentExecutingProcess.getBurstTime() - currentExecutingProcess.getRemainingTime();
                    if (quantumUsedInCurrentBurst % rrTimeQuantum != 0) { 
                        return currentExecutingProcess;
                    }
                }
                // If CPU is free or quantum exhausted, get next from queue
                if (!rrQueue.isEmpty()) {
                    selected = rrQueue.peek(); // Peek, will be polled when it actually runs
                }
                break;
            case "MLFQ":
                // Find highest priority non-empty queue
                for (int i = 0; i < 4; i++) {
                    Queue<Process> q = mlfqQueues.get(i);
                    if (!q.isEmpty()) {
                        selected = q.peek(); // Peek, will be polled when it actually runs
                        break;
                    }
                }
                break;
        }
        return selected;
    }

   
    private void requeueProcess(String algorithm, Process p) {
        if (p.getRemainingTime() > 0) { // Only re-queue if not finished
            switch (algorithm) {
                case "FCFS":
                case "SJF":
                case "SRTF":
                    break;
                case "Round Robin":
                    int quantumUsedInCurrentBurst = p.getBurstTime() - p.getRemainingTime();
                    if (quantumUsedInCurrentBurst % rrTimeQuantum == 0) { 
                        if (rrQueue.contains(p)) { 
                            rrQueue.remove(p); // Remove from its current position (might be front)
                        }
                        rrQueue.add(p); // Add to back of the queue
                    }
 break;
                case "MLFQ":
                    int currentLevel = mlfqLevel.getOrDefault(p, 0);
                    int currentSpent = mlfqSpent.getOrDefault(p, 0);

                    // Check if quantum for current level is exhausted OR allotment for current level is exhausted
                    boolean quantumExhausted = (p.getBurstTime() - p.getRemainingTime()) % mlfqTimeQuantums[currentLevel] == 0;
                    boolean allotmentExhausted = (currentLevel < 3 && currentSpent >= mlfqAllotments[currentLevel]);

                    if (quantumExhausted || allotmentExhausted) {
                        // Remove from current queue before re-adding or promoting
                        if (mlfqQueues.get(currentLevel).contains(p)) {
                            mlfqQueues.get(currentLevel).remove(p);
                        }

                        if (currentLevel < 3) { // Demote to next queue if not the last queue
                            int nextLevel = currentLevel + 1;
                            mlfqQueues.get(nextLevel).add(p);
                            mlfqLevel.put(p, nextLevel);
                            mlfqSpent.put(p, 0); // Reset spent time in new queue
                        } else { // Already in the lowest queue (Q3), behaves like FCFS
                            mlfqQueues.get(currentLevel).add(p); // Add back to Q3
                        }
                    } else {
                        // If neither quantum nor allotment exhausted, put it back to its current queue
                        if (mlfqQueues.get(currentLevel).contains(p)) {
                            mlfqQueues.get(currentLevel).remove(p); // Remove from its current position
                        }
                        mlfqQueues.get(currentLevel).add(p); // Add back to the end of its current queue
                    }
                    break;
            }
        }
    }

   
    private void removeFromAllQueues(Process p) {
        fcfsQueue.remove(p);
        rrQueue.remove(p);
        for (Queue<Process> q : mlfqQueues) {
            q.remove(p);
        }
        mlfqLevel.remove(p);
        mlfqSpent.remove(p);
    }

    
    private boolean isAllQueuesEmpty() {
        if (!fcfsQueue.isEmpty() || !rrQueue.isEmpty()) {
            return false;
        }
        for (Queue<Process> q : mlfqQueues) {
            if (!q.isEmpty()) {
                return false;
            }
        }
       
        return simulationProcesses.stream()
                                  .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                                  .collect(Collectors.toList()).isEmpty();
    }

   
    private String getQueueIdentifier(String algorithm, Process p) {
        if ("MLFQ".equals(algorithm)) {
            return "(Q" + mlfqLevel.getOrDefault(p, 0) + ")";
        }
        return "";
    }


   
    public void calculateMetrics(List<Process> processList) {
        double totalTat = 0;
        double totalRT = 0;
        double totalWT = 0;

        
        List<Process> completedProcessesOnly = processList.stream()
                                                        .filter(p -> p.getCompletionTime() > 0)
                                                        .collect(Collectors.toList());

        for (Process p : completedProcessesOnly) {
            int tat = p.getCompletionTime() - p.getArrivalTime();
            int rt = p.getStartTime() - p.getArrivalTime(); 
            int wt = tat - p.getBurstTime(); 

            p.setTurnaroundTime(tat);
            p.setResponseTime(rt);
            p.setWaitingTime(wt);

            totalTat += tat;
            totalRT += rt;
            totalWT += wt;
        }

        this.calculatedProcesses = new ArrayList<>(completedProcessesOnly); 
        if (!completedProcessesOnly.isEmpty()) {
            this.avgTurnaroundTime = totalTat / completedProcessesOnly.size();
            this.avgResponseTime = totalRT / completedProcessesOnly.size();
            this.avgWaitingTime = totalWT / completedProcessesOnly.size();
        } else {
            this.avgTurnaroundTime = 0;
            this.avgResponseTime = 0;
            this.avgWaitingTime = 0;
        }
    }

    // --- Wrapper Methods for Instant Simulation Mode ---

    public void fcfs(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("FCFS");
    }

    public void sjf(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("SJF");
    }

    public void srtf(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("SRTF");
    }

    public void roundRobin(List<Process> processes, int timeQuantum, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        setRrTimeQuantum(timeQuantum);
        runSimulationToCompletion("Round Robin");
    }

    public void mlfq(List<Process> processes, int[] timeQuantums, int[] allotments, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        setMlfqParameters(timeQuantums, allotments);
        runSimulationToCompletion("MLFQ");
    }

   
    private void runSimulationToCompletion(String algorithm) {
        while (step(algorithm)); // Keep stepping until `step` returns false (simulation complete)
    }
}
/**
 *
 * @author Nj Arnado
 */

interface SchedulerListener {
    
    void onTick(int currentTime, String cpuActivity, List<String> ganttChartOutput, List<Queue<Process>> currentQueues, List<Process> currentProcessStates);

    
    void onSimulationComplete(List<Process> calculatedProcesses, List<String> ganttChartOutput, double avgTurnaroundTime, double avgResponseTime, double avgWaitingTime);

   
    void onReset();
}

public class Scheduler {

    private List<String> ganttChartOutput;
    private double avgTurnaroundTime;
    private double avgResponseTime;
    private double avgWaitingTime;
    private List<Process> calculatedProcesses; 

    private SchedulerListener listener;

    // --- Simulation State Variables ---
    private int currentTime;
    private String currentCpuActivity;
    private List<Process> simulationProcesses; 
    private Queue<Process> fcfsQueue; 
    private Queue<Process> rrQueue; 
    private List<Queue<Process>> mlfqQueues; 
    private Map<Process, Integer> mlfqLevel; 
    private Map<Process, Integer> mlfqSpent; 
    private int rrTimeQuantum;
    private int[] mlfqTimeQuantums;
    private int[] mlfqAllotments;
    private int nextProcessToArriveIndex; 
    private Process currentExecutingProcess;
    private int contextSwitchRemaining; 

    private int contextSwitchDelay; 

    /**
     * Constructor for the Scheduler. 
     */
    public Scheduler() {
        reset(); // Initialize state
    }

    
    public void setListener(SchedulerListener listener) {
        this.listener = listener;
    }

    //Resets all
    public void reset() {
        ganttChartOutput = new ArrayList<>();
        avgTurnaroundTime = 0;
        avgResponseTime = 0;
        avgWaitingTime = 0;
        calculatedProcesses = new ArrayList<>();

        currentTime = 0;
        currentCpuActivity = "IDLE";
        simulationProcesses = new ArrayList<>();
        fcfsQueue = new LinkedList<>();
        rrQueue = new LinkedList<>();
        mlfqQueues = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            mlfqQueues.add(new LinkedList<>());
        }
        mlfqLevel = new HashMap<>();
        mlfqSpent = new HashMap<>();
        rrTimeQuantum = 0;
        mlfqTimeQuantums = new int[4];
        mlfqAllotments = new int[4];
        nextProcessToArriveIndex = 0;
        currentExecutingProcess = null;
        contextSwitchRemaining = 0; // No context switch initially

        if (listener != null) {
            listener.onReset(); 
        }
    }

    // --- Getters for Simulation Results ---
    public List<String> getGanttChartOutput() {
        return ganttChartOutput;
    }

    public double getAvgTurnaroundTime() {
        return avgTurnaroundTime;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public double getAvgWaitingTime() {
        return avgWaitingTime;
    }

    public List<Process> getCalculatedProcesses() {
        return calculatedProcesses;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public String getCurrentCpuActivity() {
        return currentCpuActivity;
    }


    public List<Queue<Process>> getCurrentQueues() {
        List<Queue<Process>> queuesCopy = new ArrayList<>();
        // For MLFQ, return all 4 queues
        for (Queue<Process> q : mlfqQueues) {
            queuesCopy.add(new LinkedList<>(q));
        }
        // For other algorithms, return the relevant queue as the first element
        // The GUI will interpret based on selected algorithm
        if (!fcfsQueue.isEmpty()) {
            queuesCopy.add(0, new LinkedList<>(fcfsQueue));
        } else if (!rrQueue.isEmpty()) {
            queuesCopy.add(0, new LinkedList<>(rrQueue));
        } else { // For SJF/SRTF, return a conceptual "ready list"
            List<Process> readyList = simulationProcesses.stream()
                .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0 && !calculatedProcesses.contains(p) && p != currentExecutingProcess)
                .collect(Collectors.toList());
            if (!readyList.isEmpty()) {
                // Sort for consistent display (e.g., by remaining time for SRTF, or burst for SJF)
                readyList.sort(Comparator.comparingInt(Process::getRemainingTime).thenComparingInt(Process::getArrivalTime));
                queuesCopy.add(0, new LinkedList<>(readyList)); // Add as a queue for consistent return type
            } else {
                 queuesCopy.add(0, new LinkedList<>()); // Add an empty queue if no ready processes
            }
        }
        return queuesCopy;
    }
    
  
    public void setProcesses(List<Process> processes, int contextSwitchDelay) {
        reset(); // ensuring clean slate 
        this.contextSwitchDelay = contextSwitchDelay;

        // deep copy processes to avoid modifying the original list for multiple runs
        for (Process p : processes) {
            this.simulationProcesses.add(new Process(p.getProcessId(), p.getArrivalTime(), p.getBurstTime()));
        }
        // sort processes by arrival time for all algorithms (important for initial queueing)
        this.simulationProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));
    }

    
    public void setRrTimeQuantum(int quantum) {
        this.rrTimeQuantum = quantum;
    }

    
    public void setMlfqParameters(int[] tqs, int[] allotments) {
        this.mlfqTimeQuantums = tqs;
        this.mlfqAllotments = allotments;
    }


    
    public boolean step(String algorithm) {
        
        boolean allProcessesCompleted = simulationProcesses.stream().allMatch(p -> p.getRemainingTime() <= 0);
        if (allProcessesCompleted && currentExecutingProcess == null && isAllQueuesEmpty() && nextProcessToArriveIndex >= simulationProcesses.size()) {
            ganttChartOutput.add("|"); 
            calculateMetrics(simulationProcesses); 
            if (listener != null) {
                listener.onSimulationComplete(calculatedProcesses, ganttChartOutput, avgTurnaroundTime, avgResponseTime, avgWaitingTime);
            }
            return false; 
        }
        
        // --- Handle Context Switching Delay ---
        if (contextSwitchRemaining > 0) {
            currentCpuActivity = "CS"; // Mark as Context Switch
            ganttChartOutput.add("| CS ");
            contextSwitchRemaining--;
            currentTime++;
            if (listener != null) {
                
                listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
            }
            return true; 
        }

     
        addArrivingProcessesToQueue(algorithm);

        // Determine next process to execute
        Process prevExecutingProcess = currentExecutingProcess; // Store previous to detect switch
        Process nextProcessToRun = selectNextProcess(algorithm);

       
        if (prevExecutingProcess != null && nextProcessToRun != null && prevExecutingProcess != nextProcessToRun && contextSwitchDelay > 0) {
            contextSwitchRemaining = contextSwitchDelay; 
            currentCpuActivity = "CS"; // Mark as Context Switch
            ganttChartOutput.add("| CS ");
            currentTime++; // Consume one time unit for the start of CS
            if (listener != null) {
                listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
            }
            return true; 
        }

        currentExecutingProcess = nextProcessToRun; 

        if (currentExecutingProcess != null) {
            if (currentExecutingProcess.getStartTime() == -1) {
                currentExecutingProcess.setStartTime(currentTime); // Set start time if first time running
            }
            currentExecutingProcess.setRemainingTime(currentExecutingProcess.getRemainingTime() - 1);
            currentCpuActivity = currentExecutingProcess.getProcessId();
            ganttChartOutput.add("| " + currentCpuActivity + getQueueIdentifier(algorithm, currentExecutingProcess) + " ");

            // For MLFQ, update spent time in current level
            if ("MLFQ".equals(algorithm)) {
                mlfqSpent.put(currentExecutingProcess, mlfqSpent.getOrDefault(currentExecutingProcess, 0) + 1);
            }

            // Check if process finished
            if (currentExecutingProcess.getRemainingTime() <= 0) {
                currentExecutingProcess.setCompletionTime(currentTime + 1); 
                removeFromAllQueues(currentExecutingProcess);
                currentExecutingProcess = null; 
            } else {
                // If not finished, put it back to queue if necessary (for pre-emptive)
                requeueProcess(algorithm, currentExecutingProcess);
                // If it was preempted by a higher priority process, it's no longer on CPU
                if (currentExecutingProcess != null && currentExecutingProcess != selectNextProcess(algorithm)) { 
                    currentExecutingProcess = null; // Preempted
                }
            }
        } else {
            // CPU is idle
            currentCpuActivity = "IDLE";
            ganttChartOutput.add("| IDLE ");
        }

        currentTime++; // Advance time

      
        if (listener != null) {
            listener.onTick(currentTime, currentCpuActivity, ganttChartOutput, getCurrentQueues(), simulationProcesses);
        }

       
        allProcessesCompleted = simulationProcesses.stream().allMatch(p -> p.getRemainingTime() <= 0);
        if (allProcessesCompleted && currentExecutingProcess == null && isAllQueuesEmpty() && nextProcessToArriveIndex >= simulationProcesses.size()) {
            ganttChartOutput.add("|"); 
            calculateMetrics(simulationProcesses); 
            if (listener != null) {
                listener.onSimulationComplete(calculatedProcesses, ganttChartOutput, avgTurnaroundTime, avgResponseTime, avgWaitingTime);
            }
            return false; 
        }
        return true; 
    }

    
    private void addArrivingProcessesToQueue(String algorithm) {
        while (nextProcessToArriveIndex < simulationProcesses.size() && 
               simulationProcesses.get(nextProcessToArriveIndex).getArrivalTime() <= currentTime) {
            Process p = simulationProcesses.get(nextProcessToArriveIndex);
            
            switch (algorithm) {
                case "FCFS":
                    fcfsQueue.add(p);
                    break;
                case "SJF": 
                case "SRTF":                
                    break;
                case "Round Robin":
                    rrQueue.add(p);
                    break;
                case "MLFQ":
                    mlfqQueues.get(0).add(p); // New arrivals go to highest priority queue (Q0)
                    mlfqLevel.put(p, 0);
                    mlfqSpent.put(p, 0);
                    break;
            }
            nextProcessToArriveIndex++;
        }
    }

   
    private Process selectNextProcess(String algorithm) {
        Process selected = null;
        switch (algorithm) {
            case "FCFS":
                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    return currentExecutingProcess; 
                }
                if (!fcfsQueue.isEmpty()) {
                    selected = fcfsQueue.peek(); 
                }
                break;
            case "SJF":
                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    return currentExecutingProcess;
                }
                List<Process> availableSJF = simulationProcesses.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .collect(Collectors.toList());
                if (!availableSJF.isEmpty()) {
                    availableSJF.sort(Comparator.comparingInt(Process::getBurstTime).thenComparingInt(Process::getArrivalTime));
                    selected = availableSJF.get(0);
                }
                break;
            case "SRTF":
                // Preemptive SRTF: always select the one with shortest remaining time
                List<Process> availableSRTF = simulationProcesses.stream()
                    .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                    .collect(Collectors.toList());
                if (!availableSRTF.isEmpty()) {
                    availableSRTF.sort(Comparator.comparingInt(Process::getRemainingTime).thenComparingInt(Process::getArrivalTime));
                    selected = availableSRTF.get(0);
                }
                break;
            case "Round Robin":

                if (currentExecutingProcess != null && currentExecutingProcess.getRemainingTime() > 0) {
                    int quantumUsedInCurrentBurst = currentExecutingProcess.getBurstTime() - currentExecutingProcess.getRemainingTime();
                    if (quantumUsedInCurrentBurst % rrTimeQuantum != 0) { 
                        return currentExecutingProcess;
                    }
                }
                // If CPU is free or quantum exhausted, get next from queue
                if (!rrQueue.isEmpty()) {
                    selected = rrQueue.peek(); // Peek, will be polled when it actually runs
                }
                break;
            case "MLFQ":
                // Find highest priority non-empty queue
                for (int i = 0; i < 4; i++) {
                    Queue<Process> q = mlfqQueues.get(i);
                    if (!q.isEmpty()) {
                        selected = q.peek(); // Peek, will be polled when it actually runs
                        break;
                    }
                }
                break;
        }
        return selected;
    }

   
    private void requeueProcess(String algorithm, Process p) {
        if (p.getRemainingTime() > 0) { // Only re-queue if not finished
            switch (algorithm) {
                case "FCFS":
                case "SJF":
                case "SRTF":
                    break;
                case "Round Robin":
                    int quantumUsedInCurrentBurst = p.getBurstTime() - p.getRemainingTime();
                    if (quantumUsedInCurrentBurst % rrTimeQuantum == 0) { 
                        if (rrQueue.contains(p)) { 
                            rrQueue.remove(p); // Remove from its current position (might be front)
                        }
                        rrQueue.add(p); // Add to back of the queue
                    }
 break;
                case "MLFQ":
                    int currentLevel = mlfqLevel.getOrDefault(p, 0);
                    int currentSpent = mlfqSpent.getOrDefault(p, 0);

                    // Check if quantum for current level is exhausted OR allotment for current level is exhausted
                    boolean quantumExhausted = (p.getBurstTime() - p.getRemainingTime()) % mlfqTimeQuantums[currentLevel] == 0;
                    boolean allotmentExhausted = (currentLevel < 3 && currentSpent >= mlfqAllotments[currentLevel]);

                    if (quantumExhausted || allotmentExhausted) {
                        // Remove from current queue before re-adding or promoting
                        if (mlfqQueues.get(currentLevel).contains(p)) {
                            mlfqQueues.get(currentLevel).remove(p);
                        }

                        if (currentLevel < 3) { // Demote to next queue if not the last queue
                            int nextLevel = currentLevel + 1;
                            mlfqQueues.get(nextLevel).add(p);
                            mlfqLevel.put(p, nextLevel);
                            mlfqSpent.put(p, 0); // Reset spent time in new queue
                        } else { // Already in the lowest queue (Q3), behaves like FCFS
                            mlfqQueues.get(currentLevel).add(p); // Add back to Q3
                        }
                    } else {
                        // If neither quantum nor allotment exhausted, put it back to its current queue
                        if (mlfqQueues.get(currentLevel).contains(p)) {
                            mlfqQueues.get(currentLevel).remove(p); // Remove from its current position
                        }
                        mlfqQueues.get(currentLevel).add(p); // Add back to the end of its current queue
                    }
                    break;
            }
        }
    }

   
    private void removeFromAllQueues(Process p) {
        fcfsQueue.remove(p);
        rrQueue.remove(p);
        for (Queue<Process> q : mlfqQueues) {
            q.remove(p);
        }
        mlfqLevel.remove(p);
        mlfqSpent.remove(p);
    }

    
    private boolean isAllQueuesEmpty() {
        if (!fcfsQueue.isEmpty() || !rrQueue.isEmpty()) {
            return false;
        }
        for (Queue<Process> q : mlfqQueues) {
            if (!q.isEmpty()) {
                return false;
            }
        }
       
        return simulationProcesses.stream()
                                  .filter(p -> p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0)
                                  .collect(Collectors.toList()).isEmpty();
    }

   
    private String getQueueIdentifier(String algorithm, Process p) {
        if ("MLFQ".equals(algorithm)) {
            return "(Q" + mlfqLevel.getOrDefault(p, 0) + ")";
        }
        return "";
    }


   
    public void calculateMetrics(List<Process> processList) {
        double totalTat = 0;
        double totalRT = 0;
        double totalWT = 0;

        
        List<Process> completedProcessesOnly = processList.stream()
                                                        .filter(p -> p.getCompletionTime() > 0)
                                                        .collect(Collectors.toList());

        for (Process p : completedProcessesOnly) {
            int tat = p.getCompletionTime() - p.getArrivalTime();
            int rt = p.getStartTime() - p.getArrivalTime(); 
            int wt = tat - p.getBurstTime(); 

            p.setTurnaroundTime(tat);
            p.setResponseTime(rt);
            p.setWaitingTime(wt);

            totalTat += tat;
            totalRT += rt;
            totalWT += wt;
        }

        this.calculatedProcesses = new ArrayList<>(completedProcessesOnly); 
        if (!completedProcessesOnly.isEmpty()) {
            this.avgTurnaroundTime = totalTat / completedProcessesOnly.size();
            this.avgResponseTime = totalRT / completedProcessesOnly.size();
            this.avgWaitingTime = totalWT / completedProcessesOnly.size();
        } else {
            this.avgTurnaroundTime = 0;
            this.avgResponseTime = 0;
            this.avgWaitingTime = 0;
        }
    }

    // --- Wrapper Methods for Instant Simulation Mode ---

    public void fcfs(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("FCFS");
    }

    public void sjf(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("SJF");
    }

    public void srtf(List<Process> processes, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        runSimulationToCompletion("SRTF");
    }

    public void roundRobin(List<Process> processes, int timeQuantum, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        setRrTimeQuantum(timeQuantum);
        runSimulationToCompletion("Round Robin");
    }

    public void mlfq(List<Process> processes, int[] timeQuantums, int[] allotments, int contextSwitchDelay) {
        setProcesses(processes, contextSwitchDelay);
        setMlfqParameters(timeQuantums, allotments);
        runSimulationToCompletion("MLFQ");
    }

   
    private void runSimulationToCompletion(String algorithm) {
        while (step(algorithm)); // Keep stepping until `step` returns false (simulation complete)
    }
}
