package srcs;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Nj Arnado
 */
public class Process {
    private final String processId;
    private int arrivalTime;
    private int burstTime;
    private int completionTime;
    private int turnaroundTime;
    private int responseTime;
    private int remainingTime;
    private int startTime;
    private int waitingTime;

    /**
     * Constructor for a Process.
     * @param id The unique identifier for the process.
     * @param arrivalTime The time at which the process arrives in the ready queue.
     * @param burstTime The total CPU time required for the process to complete.
     */
    public Process(String id, int arrivalTime, int burstTime){
        this.processId = id;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime; // Initial remaining time is full burst time
        this.startTime = -1; // -1 indicates the process has not started execution yet
        this.waitingTime = 0; // Initialize waiting time
        this.completionTime = 0; // Initialize completion time
    }

    // --- Setters ---
    public void setArrivalTime(int arrivalTime){
        this.arrivalTime = arrivalTime;
    }
    public void setBurstTime(int burstTime){
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
    }
    public void setRemainingTime(int remainingTime){
        this.remainingTime = remainingTime;
    }
    public void setCompletionTime(int completionTime){
        this.completionTime = completionTime;
    }
    public void setTurnaroundTime(int turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }
    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
    public void setWaitingTime(int waitingTime){
        this.waitingTime = waitingTime;
    }

    // --- Getters ---
    public String getProcessId(){
        return processId;
    }
    public int getArrivalTime(){
        return arrivalTime;
    }
    public int getBurstTime(){
        return burstTime;
    }
    public int getRemainingTime(){
        return remainingTime;
    }
    public int getCompletionTime(){
        return completionTime;
    }
    public int getTurnaroundTime() {
        return turnaroundTime;
    }
    public int getResponseTime() {
        return responseTime;
    }
    public int getStartTime() {
        return startTime;
    }
    public int getWaitingTime(){
        return waitingTime;
    }
    
    /**
     * Displays process information to the console (for debugging/console output).
     * Not directly used by the GUI.
     */
    public void display(){
        System.out.printf("%-9s %-10s %-10s %-14s %-13s %-13s %-12s\n",
                          processId, arrivalTime, burstTime, completionTime,
                          turnaroundTime, responseTime, waitingTime);
    }
}
