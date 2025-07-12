/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Nj Arnado
 */
public class process {
    String processId;
    int arrivalTime;
    int burstTime;
    int completionTime;
    int turnarroundTime;
    int reponseTeam;
    int remainingTime;
    int startTime;
    
    public Process(String id, int arrivalTime, int burstTime){
    this.processId = id;
    this.arrivalTime = arrivalTime;
    this.burstTime = burstTime;
    this.remainingTime = burstTime; //this is for the SRTF
    this.startTime = -1; //it will be updated
    }

    public void display(){
        System.out.printf(%-7s %-10d %-8d %-15d %-15d %-13d\n",processId, arrivalTime,bursTime, completionTime, turnaroundTime, responseTime );
        
}
}
