# CPU-Scheduling-Visualization
POJECT 01 - CPU Scheduling Visualization

**Project Overview**
This project is a Java-based CPU Scheduling Visualization that supports multiple scheduling algorithms with both console-based and (planned) GUI-based implementations. It is ideal for understanding how different CPU scheduling algorithms work by visualizing process execution and comparing turnaround time, response time, and completion time.

Support for multiple scheduling algorithms:
  - FCFS (First-Come First-Serve)
  - SJF (Shortest Job First – Non-Preemptive)
  - SRTF (Shortest Remaining Time First – Preemptive)
  - Round Robin (Preemptive with Time Quantum)
  - MLFQ (Multi-Level Feedback Queue – with Time Quantum & Slice)

Two input modes:
  - Manual Job Entry 
  - Random Job Generation but still ask if arrival and burst time is manual or randomly generated

Displays:
  - Gantt Chart (for process execution order)
  - Turnaround Time, Waiting Time, and Response Time (per process and average)
 
**How to Run**
  Console Version
  1.Make sure you have Java JDK installed (version 8 or higher).
  2. Compile all .java files:
  javac Main.java Scheduler.java Process.java
  3.Run the program:
  java Main

  **Algorithm Descriptions**
First-Come First-Serve (FIFO/FCFS)-	Processes are scheduled in the order they arrive.
Shortest Job First – Non-Preemptive (SJF)-	Selects the shortest available job to execute next. Non-preemptive.
Shortest Remaining Time First – Preemptive (SRTF)-	A preemptive version of SJF where the currently running process may be interrupted by a shorter job.
Round Robin (RR)-	Each process gets a fixed time quantum and is rotated in a queue.
(MLFQ)-	Uses multiple levels of queues with different time quanta, promoting/demoting processes based on behavior.

**Known Issues**
GUI Version is improved but there are things not yet cosidered.
Input validation is minimal. Ensure correct values are entered.
No file I/O: Results are shown only in the terminal and are not saved.

Note: This repository was set to private during development and made public for submission, as per submission guidelines.

  

