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
    
    public Process(String id, int arrivalTime, int burstTime){
    this.processId = id;
    this.arrivalTime = arrivalTime;
    this.burstTime = burstTime;
    this.remainingTime = burstTime; //this is for the SRTF
    this.startTime = -1; //it will be updated
    }
    
    //setters
    
    
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
        this.completionTime =completionTime;
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
   
    //getters
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
    
        //display method (for table)
    public void display(){
        System.out.printf("%-9s %-15d %-10d %-18d %-16d %-15 %-15d\n",processId, arrivalTime,burstTime, completionTime, turnaroundTime, responseTime, waitingTime );
        
}
}
