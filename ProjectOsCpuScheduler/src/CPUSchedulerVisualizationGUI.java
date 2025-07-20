package srcs;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Nj Arnado
 */
import java.util.*;
import javax.swing.*;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors; 
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.border.TitledBorder;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


 public class CPUSchedulerVisualizationGUI extends JFrame implements SchedulerListener {

    // --- UI Components ---
    private JComboBox<String> algorithmComboBox;
    private JSpinner numProcessesSpinner;
    private JButton generateRandomButton;
    private JTable processInputTable;
    private DefaultTableModel processInputTableModel;

    private GanttChartPanel ganttChartPanel;
    private JTable metricsTable;
    private DefaultTableModel metricsTableModel;

    private JTextField avgTurnaroundTimeField;
    private JTextField avgResponseTimeField;
    private JTextField avgWaitingTimeField;
    private JTextField totalSimulationTimeField; 

    // Algorithm-specific input fields
    private JTextField rrTimeQuantumField;
    private JTextField mlfqQ0TQField, mlfqQ1TQField, mlfqQ2TQField, mlfqQ3TQField;
    private JTextField mlfqQ0AllotField, mlfqQ1AllotField, mlfqQ2AllotField, mlfqQ3AllotField;

    // Simulation control elements
    private JSlider simSpeedSlider; 
    private JSpinner contextSwitchSpinner;
    private JButton startPauseResumeButton;
    private JButton nextStepButton;
    private JButton resetButton;
    private JButton exportButton;

    // Live status display areas
    private JTextArea currentTimeArea;
    private JTextArea cpuActivityArea;
    private JTextArea queueVizArea;

    // --- Simulation Logic Fields ---
    private Scheduler scheduler;
    private Timer simulationTimer;
    private int simulationSpeedDelay; 
    private boolean isSimulationRunning = false;
    private boolean isStepByStepMode = false;
    private List<Process> currentInputProcesses = new ArrayList<>();

   
    public CPUSchedulerVisualizationGUI() {
        setTitle("CPU Scheduling Simulator");
        setSize(1200, 850); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        getContentPane().setBackground(Color.BLACK); 
        setLayout(new BorderLayout(10, 10)); 

        scheduler = new Scheduler();
        scheduler.setListener(this); 

       
        simulationSpeedDelay = 500; 

        
        simulationTimer = new Timer(simulationSpeedDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
             
                String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
                if (!scheduler.step(selectedAlgorithm)) {
                    simulationTimer.stop();
                    isSimulationRunning = false;
                    startPauseResumeButton.setText("Start Simulation");
                    exportButton.setEnabled(true);
                    enableInputControls(true); 
                }
            }
        });
        simulationTimer.setCoalesce(true); // Ensure only one event is processed at a time

        // --- Main Title (Big CPU Scheduling Visualization) ---
        JLabel mainTitleLabel = new JLabel("CPU Scheduling Visualization", SwingConstants.CENTER);
        mainTitleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        mainTitleLabel.setForeground(Color.CYAN);
       

        // --- Combined Top Panel (Title + Controls) ---
        JPanel combinedTopPanel = new JPanel();
        combinedTopPanel.setBackground(Color.BLACK);
        combinedTopPanel.setLayout(new BoxLayout(combinedTopPanel, BoxLayout.Y_AXIS)); 

        combinedTopPanel.add(mainTitleLabel); 

        // --- Top Control Panel (Algorithm, Processes, Speed, CS Delay, Buttons) ---
        JPanel topControlPanel = new JPanel();
        topControlPanel.setBackground(Color.BLACK);
        topControlPanel.setLayout(new BoxLayout(topControlPanel, BoxLayout.Y_AXIS)); 

        // Row 1: Algorithm, Num Processes, Generate Random
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row1.setBackground(Color.BLACK);
        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setForeground(Color.CYAN);
        algoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        algorithmComboBox = new JComboBox<>(new String[]{"FCFS", "SJF", "SRTF", "Round Robin", "MLFQ"});
        styleComboBox(algorithmComboBox);
        algorithmComboBox.addActionListener(e -> toggleAlgorithmSpecificInputs());

        JLabel numProcessLabel = new JLabel("Num Processes:");
        numProcessLabel.setForeground(Color.CYAN);
        numProcessLabel.setFont(new Font("Arial", Font.BOLD, 16));
        SpinnerModel numProcessModel = new SpinnerNumberModel(5, 1, 20, 1); // Default 5 processes, min 1, max 20
        numProcessesSpinner = new JSpinner(numProcessModel);
        numProcessesSpinner.setPreferredSize(new Dimension(60, 25));
        numProcessesSpinner.addChangeListener(e -> {
            int currentNum = (int) numProcessesSpinner.getValue();
            processInputTableModel.setRowCount(0);
            currentInputProcesses.clear();
            for (int i = 0; i < currentNum; i++) {
                String processId = "P" + (i + 1);
                processInputTableModel.addRow(new Object[]{processId, 0, 0});
                currentInputProcesses.add(new Process(processId, 0, 0));
            }
            resetSimulation();
        });

        generateRandomButton = new JButton("Generate Random");
        styleButton(generateRandomButton);
        generateRandomButton.addActionListener(e -> generateRandomProcesses((int) numProcessesSpinner.getValue()));
        
        row1.add(algoLabel);
        row1.add(algorithmComboBox);
        row1.add(numProcessLabel);
        row1.add(numProcessesSpinner);
        row1.add(generateRandomButton);
        topControlPanel.add(row1);

        // Row 2: Sim Speed, Context Switch Delay
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row2.setBackground(Color.BLACK);
        JLabel simSpeedLabel = new JLabel("Sim Speed:");
        simSpeedLabel.setForeground(Color.CYAN);
        simSpeedLabel.setFont(new Font("Arial", Font.BOLD, 16));
        simSpeedSlider = new JSlider(JSlider.HORIZONTAL, 0, 4, 2); // 0:Instant, 1:Fast, 2:Normal, 3:Slow, 4:Step-by-Step
        simSpeedSlider.setBackground(Color.BLACK);
        simSpeedSlider.setForeground(Color.CYAN);
        simSpeedSlider.setMajorTickSpacing(1);
        simSpeedSlider.setPaintTicks(true);
        simSpeedSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("Instant"));
        labelTable.put(1, new JLabel("Fast"));
        labelTable.put(2, new JLabel("Normal"));
        labelTable.put(3, new JLabel("Slow"));
        labelTable.put(4, new JLabel("Step-by-Step"));
        simSpeedSlider.setLabelTable(labelTable);
        simSpeedSlider.setPreferredSize(new Dimension(300, 50));
        styleSliderLabels(simSpeedSlider); 
        simSpeedSlider.addChangeListener(e -> updateSimulationSpeed());
        simSpeedSlider.setValue(2); 

        JLabel csDelayLabel = new JLabel("Context Switch Delay:");
        csDelayLabel.setForeground(Color.CYAN);
        csDelayLabel.setFont(new Font("Arial", Font.BOLD, 16));
        SpinnerModel csDelayModel = new SpinnerNumberModel(1, 0, 5, 1); // Default 1 unit, min 0, max 5
        contextSwitchSpinner = new JSpinner(csDelayModel);
        contextSwitchSpinner.setPreferredSize(new Dimension(60, 25));

        row2.add(simSpeedLabel);
        row2.add(simSpeedSlider);
        row2.add(csDelayLabel);
        row2.add(contextSwitchSpinner);
        topControlPanel.add(row2);

        // Row 3: Simulation Control Buttons
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row3.setBackground(Color.BLACK);
        startPauseResumeButton = new JButton("Start Simulation");
        styleButton(startPauseResumeButton);
        startPauseResumeButton.addActionListener(e -> handleStartPauseResume());

        nextStepButton = new JButton("Next Step");
        styleButton(nextStepButton);
        nextStepButton.addActionListener(e -> handleNextStep());
        nextStepButton.setEnabled(false); // Only enabled in Step-by-Step mode

        resetButton = new JButton("Reset");
        styleButton(resetButton);
        resetButton.addActionListener(e -> resetSimulation());

        exportButton = new JButton("Export Results");
        styleButton(exportButton);
        exportButton.addActionListener(e -> exportResults());
        exportButton.setEnabled(false); // Enabled after simulation completes

        row3.add(startPauseResumeButton);
        row3.add(nextStepButton);
        row3.add(resetButton);
        row3.add(exportButton);
        topControlPanel.add(row3);

        combinedTopPanel.add(topControlPanel); 
        add(combinedTopPanel, BorderLayout.NORTH); 


        // --- Main Content Panel (Left: Input/MLFQ, Right: Live Status/Queues) ---
        JPanel mainContentPanel = new JPanel(new GridBagLayout()); 
        mainContentPanel.setBackground(Color.BLACK);

        // Process Input Table and MLFQ/RR Configuration
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(Color.BLACK);
        GridBagConstraints leftGBC = new GridBagConstraints();
        leftGBC.insets = new Insets(5, 5, 5, 5);
        leftGBC.anchor = GridBagConstraints.NORTHWEST;
        leftGBC.fill = GridBagConstraints.BOTH;

        
        JLabel processInputLabel = new JLabel("Process Definition (Manual Input):", SwingConstants.LEFT);
        processInputLabel.setForeground(Color.BLACK);
        processInputLabel.setFont(new Font("Arial", Font.BOLD, 16));
        leftGBC.gridx = 0; leftGBC.gridy = 0; leftGBC.gridwidth = 2; leftGBC.weightx = 1.0; leftGBC.weighty = 0;
        leftPanel.add(processInputLabel, leftGBC);

        String[] processColumnNames = {"Process ID", "Arrival Time", "Burst Time"};
        processInputTableModel = new DefaultTableModel(processColumnNames, 0);
        processInputTable = new JTable(processInputTableModel);
        styleTable(processInputTable);
        processInputTable.getTableHeader().setForeground(Color.BLACK); 
        JScrollPane processScrollPane = new JScrollPane(processInputTable);
        processScrollPane.setPreferredSize(new Dimension(350, 200));
        processScrollPane.getViewport().setBackground(Color.BLACK);
        leftGBC.gridx = 0; leftGBC.gridy = 1; leftGBC.gridwidth = 2; leftGBC.weighty = 0.5;
        leftPanel.add(processScrollPane, leftGBC);

        // MLFQ / Round Robin Configuration Panel
        JPanel mlfqConfigPanel = new JPanel(new GridBagLayout()); 
        mlfqConfigPanel.setBackground(Color.BLACK);
        mlfqConfigPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.CYAN), "Algorithm Specific Settings", TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.CYAN));
        mlfqConfigPanel.setPreferredSize(new Dimension(400, 200));

        GridBagConstraints mlfqGBC = new GridBagConstraints();
        mlfqGBC.insets = new Insets(2, 5, 2, 5);
        mlfqGBC.anchor = GridBagConstraints.WEST;
        mlfqGBC.fill = GridBagConstraints.HORIZONTAL;
        mlfqGBC.weightx = 0.5; 

        // Row 0: RR Time Quantum
        mlfqGBC.gridx = 0; mlfqGBC.gridy = 0; mlfqGBC.gridwidth = 1; 
        JLabel rrTQLabel = new JLabel("RR Time Quantum:");
        rrTQLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(rrTQLabel, mlfqGBC);
        mlfqGBC.gridx = 1; mlfqGBC.gridy = 0; mlfqGBC.gridwidth = 1; 
        rrTimeQuantumField = new JTextField("");
        styleTextField(rrTimeQuantumField);
        mlfqConfigPanel.add(rrTimeQuantumField, mlfqGBC);

        // Rows for Qx TQ (2 columns per pair, 4 columns total)
        // Q0 TQ
        mlfqGBC.gridx = 0; mlfqGBC.gridy = 1; mlfqGBC.gridwidth = 1;
        JLabel q0TQLabel = new JLabel("Q0 TQ:");
        q0TQLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q0TQLabel, mlfqGBC);
        mlfqGBC.gridx = 1; mlfqGBC.gridy = 1; mlfqGBC.gridwidth = 1;
        mlfqQ0TQField = new JTextField("");
        styleTextField(mlfqQ0TQField);
        mlfqConfigPanel.add(mlfqQ0TQField, mlfqGBC);

        // Q1 TQ
        mlfqGBC.gridx = 2; mlfqGBC.gridy = 1; mlfqGBC.gridwidth = 1;
        JLabel q1TQLabel = new JLabel("Q1 TQ:");
        q1TQLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q1TQLabel, mlfqGBC);
        mlfqGBC.gridx = 3; mlfqGBC.gridy = 1; mlfqGBC.gridwidth = 1;
        mlfqQ1TQField = new JTextField("");
        styleTextField(mlfqQ1TQField);
        mlfqConfigPanel.add(mlfqQ1TQField, mlfqGBC);

        // Q2 TQ
        mlfqGBC.gridx = 0; mlfqGBC.gridy = 2; mlfqGBC.gridwidth = 1;
        JLabel q2TQLabel = new JLabel("Q2 TQ:");
        q2TQLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q2TQLabel, mlfqGBC);
        mlfqGBC.gridx = 1; mlfqGBC.gridy = 2; mlfqGBC.gridwidth = 1;
        mlfqQ2TQField = new JTextField("");
        styleTextField(mlfqQ2TQField);
        mlfqConfigPanel.add(mlfqQ2TQField, mlfqGBC);

        // Q3 TQ
        mlfqGBC.gridx = 2; mlfqGBC.gridy = 2; mlfqGBC.gridwidth = 1;
        JLabel q3TQLabel = new JLabel("Q3 TQ:");
        q3TQLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q3TQLabel, mlfqGBC);
        mlfqGBC.gridx = 3; mlfqGBC.gridy = 2; mlfqGBC.gridwidth = 1;
        mlfqQ3TQField = new JTextField("");
        styleTextField(mlfqQ3TQField);
        mlfqConfigPanel.add(mlfqQ3TQField, mlfqGBC);
        
        // Rows for Qx Allot (2 columns per pair, 4 columns total)
        // Q0 Allot
        mlfqGBC.gridx = 0; mlfqGBC.gridy = 3; mlfqGBC.gridwidth = 1;
        JLabel q0AllotLabel = new JLabel("Q0 Allot:");
        q0AllotLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q0AllotLabel, mlfqGBC);
        mlfqGBC.gridx = 1; mlfqGBC.gridy = 3; mlfqGBC.gridwidth = 1;
        mlfqQ0AllotField = new JTextField("");
        styleTextField(mlfqQ0AllotField);
        mlfqConfigPanel.add(mlfqQ0AllotField, mlfqGBC);

        // Q1 Allot
        mlfqGBC.gridx = 2; mlfqGBC.gridy = 3; mlfqGBC.gridwidth = 1;
        JLabel q1AllotLabel = new JLabel("Q1 Allot:");
        q1AllotLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q1AllotLabel, mlfqGBC);
        mlfqGBC.gridx = 3; mlfqGBC.gridy = 3; mlfqGBC.gridwidth = 1;
        mlfqQ1AllotField = new JTextField("");
        styleTextField(mlfqQ1AllotField);
        mlfqConfigPanel.add(mlfqQ1AllotField, mlfqGBC);

        // Q2 Allot
        mlfqGBC.gridx = 0; mlfqGBC.gridy = 4; mlfqGBC.gridwidth = 1;
        JLabel q2AllotLabel = new JLabel("Q2 Allot:");
        q2AllotLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q2AllotLabel, mlfqGBC);
        mlfqGBC.gridx = 1; mlfqGBC.gridy = 4; mlfqGBC.gridwidth = 1;
        mlfqQ2AllotField = new JTextField("");
        styleTextField(mlfqQ2AllotField);
        mlfqConfigPanel.add(mlfqQ2AllotField, mlfqGBC);

        // Q3 Allot
        mlfqGBC.gridx = 2; mlfqGBC.gridy = 4; mlfqGBC.gridwidth = 1;
        JLabel q3AllotLabel = new JLabel("Q3 Allot:");
        q3AllotLabel.setForeground(Color.CYAN);
        mlfqConfigPanel.add(q3AllotLabel, mlfqGBC);
        mlfqGBC.gridx = 3; mlfqGBC.gridy = 4; mlfqGBC.gridwidth = 1;
        mlfqQ3AllotField = new JTextField("N/A");
        mlfqQ3AllotField.setEditable(false); // Q3 allotment is typically infinite or not applicable
        styleTextField(mlfqQ3AllotField);
        mlfqConfigPanel.add(mlfqQ3AllotField, mlfqGBC);

        leftGBC.gridx = 0; leftGBC.gridy = 2; leftGBC.gridwidth = 2; leftGBC.weighty = 0.5;
        leftPanel.add(mlfqConfigPanel, leftGBC);

        // Live Simulation Status Panel
        JPanel rightPanel = new JPanel(new GridBagLayout()); 
        rightPanel.setBackground(Color.BLACK);
        GridBagConstraints rightGBC = new GridBagConstraints();
        rightGBC.insets = new Insets(5, 5, 5, 5);
        rightGBC.anchor = GridBagConstraints.NORTHWEST;
        rightGBC.fill = GridBagConstraints.BOTH;
        rightGBC.weightx = 1.0;
        rightGBC.weighty = 1.0;

        JPanel simStatusPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        simStatusPanel.setBackground(Color.BLACK);
        simStatusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.CYAN), "Live Simulation Status", TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.CYAN));
        simStatusPanel.setPreferredSize(new Dimension(350, 200));

        currentTimeArea = new JTextArea("Current Time: 0");
        styleTextArea(currentTimeArea);
        currentTimeArea.setEditable(false);
        simStatusPanel.add(new JScrollPane(currentTimeArea));

        cpuActivityArea = new JTextArea("CPU: IDLE");
        styleTextArea(cpuActivityArea);
        cpuActivityArea.setEditable(false);
        simStatusPanel.add(new JScrollPane(cpuActivityArea));

        queueVizArea = new JTextArea("Queues:\n");
        styleTextArea(queueVizArea);
        queueVizArea.setEditable(false);
        JScrollPane queueScrollPane = new JScrollPane(queueVizArea);
        queueScrollPane.setPreferredSize(new Dimension(300, 150));
        simStatusPanel.add(queueScrollPane);
        
        rightGBC.gridx = 0; rightGBC.gridy = 0; rightGBC.gridwidth = 1; rightGBC.gridheight = 1;
        rightPanel.add(simStatusPanel, rightGBC);

 
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        topSplitPane.setResizeWeight(0.5); 
        topSplitPane.setBackground(Color.BLACK);
        topSplitPane.setBorder(null); 
        
    
        GridBagConstraints mainContentGBC = new GridBagConstraints();
        mainContentGBC.insets = new Insets(5,5,5,5);
        mainContentGBC.gridx = 0; mainContentGBC.gridy = 0; mainContentGBC.gridwidth = 1; mainContentGBC.gridheight = 1;
        mainContentGBC.weightx = 1.0; mainContentGBC.weighty = 1.0; mainContentGBC.fill = GridBagConstraints.BOTH;
        mainContentPanel.add(topSplitPane, mainContentGBC);


        // Gantt Chart, Metrics Table, Average Metrics
        JPanel bottomContentPanel = new JPanel(new GridBagLayout());
        bottomContentPanel.setBackground(Color.BLACK);
        GridBagConstraints bottomGBC = new GridBagConstraints();
        bottomGBC.insets = new Insets(5, 5, 5, 5);
        bottomGBC.anchor = GridBagConstraints.NORTHWEST;
        bottomGBC.fill = GridBagConstraints.BOTH;

        // Gantt Chart Area 
        JLabel ganttLabel = new JLabel("Gantt Chart:", SwingConstants.LEFT); 
        ganttLabel.setForeground(Color.WHITE);
        ganttLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottomGBC.gridx = 0; bottomGBC.gridy = 0; bottomGBC.gridwidth = 2; bottomGBC.weightx = 1.0; bottomGBC.weighty = 0;
        bottomContentPanel.add(ganttLabel, bottomGBC);

        ganttChartPanel = new GanttChartPanel();
        ganttChartPanel.setBorder(BorderFactory.createLineBorder(Color.CYAN));
        JScrollPane ganttScrollPane = new JScrollPane(ganttChartPanel);
        ganttScrollPane.setPreferredSize(new Dimension(800, 150));
        ganttScrollPane.getViewport().setBackground(Color.BLACK);
        bottomGBC.gridx = 0; bottomGBC.gridy = 1; bottomGBC.gridwidth = 2; bottomGBC.weighty = 0.5;
        bottomContentPanel.add(ganttScrollPane, bottomGBC);

        // Metrics Table 
        JLabel metricsLabel = new JLabel("Process Metrics:", SwingConstants.LEFT); 
        metricsLabel.setForeground(Color.WHITE);
        metricsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottomGBC.gridx = 0; bottomGBC.gridy = 2; bottomGBC.gridwidth = 2; bottomGBC.weighty = 0;
        bottomContentPanel.add(metricsLabel, bottomGBC);

        String[] metricsColumnNames = {"Process ID", "Arrival Time", "Burst Time", "Completion Time", "Turnaround Time", "Response Time", "Waiting Time"};
        metricsTableModel = new DefaultTableModel(metricsColumnNames, 0);
        metricsTable = new JTable(metricsTableModel);
        styleTable(metricsTable);
        metricsTable.getTableHeader().setForeground(Color.BLACK); 
        JScrollPane metricsScrollPane = new JScrollPane(metricsTable);
        metricsScrollPane.setPreferredSize(new Dimension(800, 200));
        metricsScrollPane.getViewport().setBackground(Color.BLACK);
        bottomGBC.gridx = 0; bottomGBC.gridy = 3; bottomGBC.gridwidth = 2; bottomGBC.weighty = 1.0;
        bottomContentPanel.add(metricsScrollPane, bottomGBC);

        // Average Metrics 
        JPanel avgMetricsPanel = new JPanel(new GridLayout(4, 2, 10, 5)); 
        avgMetricsPanel.setBackground(Color.BLACK);
        avgMetricsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.CYAN), "Average Metrics", TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.CYAN));
        avgMetricsPanel.setPreferredSize(new Dimension(300, 120)); 

        JLabel avgTATLabel = new JLabel("Avg Turnaround Time:");
        avgTATLabel.setForeground(Color.WHITE);
        avgTurnaroundTimeField = new JTextField();
        avgTurnaroundTimeField.setEditable(false);
        styleTextField(avgTurnaroundTimeField);

        JLabel avgRTLabel = new JLabel("Avg Response Time:");
        avgRTLabel.setForeground(Color.WHITE);
        avgResponseTimeField = new JTextField();
        avgResponseTimeField.setEditable(false);
        styleTextField(avgResponseTimeField);

        JLabel avgWTLabel = new JLabel("Avg Waiting Time:");
        avgWTLabel.setForeground(Color.WHITE);
        avgWaitingTimeField = new JTextField();
        avgWaitingTimeField.setEditable(false);
        styleTextField(avgWaitingTimeField);

        JLabel totalSimTimeLabel = new JLabel("Total Simulation Time:"); 
        totalSimTimeLabel.setForeground(Color.WHITE);
        totalSimulationTimeField = new JTextField(); 
        totalSimulationTimeField.setEditable(false);
        styleTextField(totalSimulationTimeField);


        avgMetricsPanel.add(avgTATLabel);
        avgMetricsPanel.add(avgTurnaroundTimeField);
        avgMetricsPanel.add(avgRTLabel);
        avgMetricsPanel.add(avgResponseTimeField);
        avgMetricsPanel.add(avgWTLabel);
        avgMetricsPanel.add(avgWaitingTimeField);
        avgMetricsPanel.add(totalSimTimeLabel); 
        avgMetricsPanel.add(totalSimulationTimeField);


        bottomGBC.gridx = 2; bottomGBC.gridy = 0; bottomGBC.gridwidth = 1; bottomGBC.gridheight = 4; 
        bottomGBC.anchor = GridBagConstraints.NORTHEAST; bottomGBC.fill = GridBagConstraints.NONE; bottomGBC.weightx = 0;
        bottomContentPanel.add(avgMetricsPanel, bottomGBC);

        // Add the main content panel to the frame's center
        mainContentGBC.gridx = 0; mainContentGBC.gridy = 1; mainContentGBC.gridwidth = 1; mainContentGBC.gridheight = 1;
        mainContentGBC.weightx = 1.0; mainContentGBC.weighty = 1.0; mainContentGBC.fill = GridBagConstraints.BOTH;
        mainContentPanel.add(bottomContentPanel, mainContentGBC); 


        add(mainContentPanel, BorderLayout.CENTER); 

        
        initializeProcessTable((int) numProcessesSpinner.getValue()); 
        toggleAlgorithmSpecificInputs(); 
        
        setVisible(true);
    }

    // --- Styling Helper Methods ---
    private void styleButton(JButton button) {
        button.setBackground(Color.BLACK); 
        button.setForeground(Color.GREEN); 
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
    }

    private void styleComboBox(JComboBox<String> comboBox) {
        comboBox.setBackground(Color.DARK_GRAY);
        comboBox.setForeground(Color.CYAN);
        comboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                comp.setBackground(isSelected ? Color.BLUE.darker() : Color.DARK_GRAY);
                comp.setForeground(Color.CYAN);
                return comp;
            }
        });
    }

    private void styleTable(JTable table) {
        table.setBackground(Color.BLACK);
        table.setForeground(Color.WHITE);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setGridColor(Color.DARK_GRAY);
        table.getTableHeader().setBackground(Color.GRAY.darker());
        table.getTableHeader().setForeground(Color.WHITE);
    }

    private void styleTextField(JTextField textField) {
        textField.setBackground(Color.DARK_GRAY);
        textField.setForeground(Color.CYAN);
    }
    
    private void styleTextArea(JTextArea textArea) {
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setForeground(Color.GREEN); 
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setBorder(BorderFactory.createLineBorder(Color.CYAN));
    }

    
    private void styleSliderLabels(JSlider slider) {
        slider.setLabelTable(slider.createStandardLabels(1)); 
        Enumeration<?> labels = slider.getLabelTable().elements();
        while (labels.hasMoreElements()) {
            JLabel label = (JLabel) labels.nextElement();
            label.setForeground(Color.CYAN);
            label.setFont(new Font("Arial", Font.PLAIN, 12));
        }
    }


    // --- Core Logic Methods ---
   
    private void initializeProcessTable(int numProcesses) {
        processInputTableModel.setRowCount(0); 
        currentInputProcesses.clear(); 
        for (int i = 0; i < numProcesses; i++) {
            String processId = "P" + (i + 1);
            processInputTableModel.addRow(new Object[]{processId, 0, 0});
            currentInputProcesses.add(new Process(processId, 0, 0)); // Dummy processes for initial state
        }
     
    }

   //generate random arrival and burst
    private void generateRandomProcesses(int numProcesses) {
        Random rand = new Random();
        processInputTableModel.setRowCount(0); 
        currentInputProcesses.clear(); 

        for (int i = 0; i < numProcesses; i++) {
            String processId = "P" + (i + 1);
            int arrivalTime = rand.nextInt(20); 
            int burstTime = rand.nextInt(15) + 1; 
            processInputTableModel.addRow(new Object[]{processId, arrivalTime, burstTime});
            currentInputProcesses.add(new Process(processId, arrivalTime, burstTime));
        }
        resetSimulation(); 
    }

    
    private void toggleAlgorithmSpecificInputs() {
        String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
        boolean isMLFQ = "MLFQ".equals(selectedAlgo);
        boolean isRR = "Round Robin".equals(selectedAlgo);

        
        rrTimeQuantumField.setVisible(isRR);
        if (rrTimeQuantumField.getParent() != null) {
            Component[] siblings = rrTimeQuantumField.getParent().getComponents();
            for (Component comp : siblings) {
                if (comp instanceof JLabel && "RR Time Quantum:".equals(((JLabel)comp).getText())) {
                    comp.setVisible(isRR);
                    break;
                }
            }
        }

        // MLFQ TQ fields
        mlfqQ0TQField.setVisible(isMLFQ);
        mlfqQ1TQField.setVisible(isMLFQ);
        mlfqQ2TQField.setVisible(isMLFQ);
        mlfqQ3TQField.setVisible(isMLFQ);
        
        // MLFQ Allotment fields
        mlfqQ0AllotField.setVisible(isMLFQ);
        mlfqQ1AllotField.setVisible(isMLFQ);
        mlfqQ2AllotField.setVisible(isMLFQ);
        mlfqQ3AllotField.setVisible(isMLFQ); // Q3 N/A field

        
        Component[] mlfqPanelComponents = mlfqQ0TQField.getParent().getComponents(); 
        for (Component comp : mlfqPanelComponents) {
            if (comp instanceof JLabel) {
                String text = ((JLabel)comp).getText();
                if (text.startsWith("Q") || text.endsWith("Allot:")) { 
                     comp.setVisible(isMLFQ);
                }
            }
        }
        
    }
    
 
    private void updateSimulationSpeed() {
        int sliderValue = simSpeedSlider.getValue();
        switch (sliderValue) {
            case 0: // Instant
                simulationSpeedDelay = 0;
                isStepByStepMode = false;
                nextStepButton.setEnabled(false);
                break;
            case 1: // Fast
                simulationSpeedDelay = 200;
                isStepByStepMode = false;
                nextStepButton.setEnabled(false);
                break;
            case 2: // Normal
                simulationSpeedDelay = 500;
                isStepByStepMode = false;
                nextStepButton.setEnabled(false);
                break;
            case 3: // Slow
                simulationSpeedDelay = 1000;
                isStepByStepMode = false;
                nextStepButton.setEnabled(false);
                break;
            case 4: // Step-by-Step
                simulationSpeedDelay = 0; // Timer not used for delay, but for triggering steps
                isStepByStepMode = true;
                nextStepButton.setEnabled(true);
                break;
        }
      
        if (simulationTimer.isRunning()) {
            simulationTimer.stop();
            if (!isStepByStepMode && simulationSpeedDelay > 0) {
                simulationTimer.setDelay(simulationSpeedDelay);
                simulationTimer.start();
            }
        }
        // If in step-by-step mode and already paused, ensure button text is correct
        if (isStepByStepMode && isSimulationRunning) {
            startPauseResumeButton.setText("Resume Simulation");
        }
    }

    
    private void enableInputControls(boolean enable) {
        algorithmComboBox.setEnabled(enable);
        numProcessesSpinner.setEnabled(enable);
        generateRandomButton.setEnabled(enable);
        processInputTable.setEnabled(enable);
        rrTimeQuantumField.setEditable(enable);
        mlfqQ0TQField.setEditable(enable);
        mlfqQ1TQField.setEditable(enable);
        mlfqQ2TQField.setEditable(enable);
        mlfqQ3TQField.setEditable(enable);
        mlfqQ0AllotField.setEditable(enable);
        mlfqQ1AllotField.setEditable(enable);
        mlfqQ2AllotField.setEditable(enable);
        contextSwitchSpinner.setEnabled(enable);
        simSpeedSlider.setEnabled(enable); 
    }


    private void handleStartPauseResume() {
        if (!isSimulationRunning) {
            startSimulation();
        } else {
            pauseSimulation();
        }
    }

    
    private void startSimulation() { 
        if (scheduler.getCurrentTime() == 0) {
            if (!readProcessInputs()) {
                return; 
            }
            String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
            int csDelay = (int) contextSwitchSpinner.getValue();
            
            // Pass a deep copy of processes to the scheduler to avoid external modification
            List<Process> processesForScheduler = new ArrayList<>();
            for(Process p : currentInputProcesses) {
                processesForScheduler.add(new Process(p.getProcessId(), p.getArrivalTime(), p.getBurstTime()));
            }
            scheduler.setProcesses(processesForScheduler, csDelay);
            
            // Set algorithm-specific parameters in the scheduler
            if ("Round Robin".equals(selectedAlgorithm)) {
                try {
                    int rrQuantum = Integer.parseInt(rrTimeQuantumField.getText());
                    if (rrQuantum <= 0) {
                        JOptionPane.showMessageDialog(this, "Round Robin Time Quantum must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    scheduler.setRrTimeQuantum(rrQuantum);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format for RR Time Quantum.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if ("MLFQ".equals(selectedAlgorithm)) {
                try {
                    int[] tqs = new int[4];
                    int[] allotments = new int[4];
                    tqs[0] = Integer.parseInt(mlfqQ0TQField.getText());
                    tqs[1] = Integer.parseInt(mlfqQ1TQField.getText());
                    tqs[2] = Integer.parseInt(mlfqQ2TQField.getText());
                    tqs[3] = Integer.parseInt(mlfqQ3TQField.getText());

                    allotments[0] = Integer.parseInt(mlfqQ0AllotField.getText());
                    allotments[1] = Integer.parseInt(mlfqQ1AllotField.getText());
                    allotments[2] = Integer.parseInt(mlfqQ2AllotField.getText());
                    allotments[3] = Integer.MAX_VALUE; // Q3 usually has infinite allotment or FCFS

                    for (int tq : tqs) { if (tq <= 0) throw new IllegalArgumentException("MLFQ Time Quantum must be > 0."); }
                    for (int allot : new int[]{allotments[0], allotments[1], allotments[2]}) { if (allot <= 0) throw new IllegalArgumentException("MLFQ Allotment must be > 0."); }

                    scheduler.setMlfqParameters(tqs, allotments);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format for MLFQ settings.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid value for MLFQ settings: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // Start actual simulation based on speed setting
        int sliderValue = simSpeedSlider.getValue();
        if (sliderValue == 0) { // Instant mode
            runInstantSimulation();
        } else if (isStepByStepMode) {
            startPauseResumeButton.setText("Pause Simulation");
            nextStepButton.setEnabled(true);
            enableInputControls(false); 
            handleNextStep(); 
        } else {
            simulationTimer.setDelay(simulationSpeedDelay);
            simulationTimer.start();
            startPauseResumeButton.setText("Pause Simulation");
            enableInputControls(false); 
        }
        isSimulationRunning = true;
        exportButton.setEnabled(false); 
    }

    /**
     * Pauses the ongoing simulation.
     */
    private void pauseSimulation() {
        if (simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        isSimulationRunning = false;
        startPauseResumeButton.setText("Resume Simulation");
        nextStepButton.setEnabled(isStepByStepMode); 
        enableInputControls(false); 
    }

   
    private void handleNextStep() {
        if (isStepByStepMode) {
            if (scheduler.getCalculatedProcesses().size() == currentInputProcesses.size() && scheduler.getCurrentTime() > 0) {
                isSimulationRunning = false;
                startPauseResumeButton.setText("Start Simulation");
                nextStepButton.setEnabled(false);
                exportButton.setEnabled(true);
                enableInputControls(true);
                JOptionPane.showMessageDialog(this, "Simulation completed!", "Simulation Finished", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
            boolean simulationContinues = scheduler.step(selectedAlgorithm);

            if (!simulationContinues) {
                isSimulationRunning = false;
                startPauseResumeButton.setText("Start Simulation");
                nextStepButton.setEnabled(false);
                exportButton.setEnabled(true);
                enableInputControls(true);
                JOptionPane.showMessageDialog(this, "Simulation completed!", "Simulation Finished", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

  
    private void runInstantSimulation() {
        String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
        while (scheduler.step(selectedAlgorithm)); 
    }

  
    private boolean readProcessInputs() {
        currentInputProcesses.clear();
        for (int i = 0; i < processInputTableModel.getRowCount(); i++) {
            try {
                String id = (String) processInputTableModel.getValueAt(i, 0);
                int arrival = Integer.parseInt(processInputTableModel.getValueAt(i, 1).toString());
                int burst = Integer.parseInt(processInputTableModel.getValueAt(i, 2).toString());

                if (burst <= 0) {
                    JOptionPane.showMessageDialog(this, "Burst Time for " + id + " must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                if (arrival < 0) {
                    JOptionPane.showMessageDialog(this, "Arrival Time for " + id + " cannot be negative.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                currentInputProcesses.add(new Process(id, arrival, burst));
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format in process input table. Please enter integers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (currentInputProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No processes defined. Please add processes or generate random ones.", "No Processes", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    
    private void resetSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        isSimulationRunning = false;
        isStepByStepMode = false;
        startPauseResumeButton.setText("Start Simulation");
        simSpeedSlider.setValue(2); 
        nextStepButton.setEnabled(false);
        exportButton.setEnabled(false); 
        enableInputControls(true); 

        // Clear all display areas and tables
        metricsTableModel.setRowCount(0);
        avgTurnaroundTimeField.setText("");
        avgResponseTimeField.setText("");
        avgWaitingTimeField.setText("");
        totalSimulationTimeField.setText(""); 
        currentTimeArea.setText("Current Time: 0");
        cpuActivityArea.setText("CPU: IDLE");
        queueVizArea.setText("Queues:\n");
        ganttChartPanel.setGanttData(new ArrayList<>(), new ArrayList<>()); 

        scheduler.reset(); 
        updateSimulationSpeed(); // Re-apply speed settings
    }

    
    private void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Simulation Results");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".txt");
            }

            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write("CPU Scheduling Simulation Results\n");
                writer.write("-------------------------------------\n\n");
                writer.write("Algorithm: " + algorithmComboBox.getSelectedItem() + "\n");
                writer.write("Context Switch Delay: " + contextSwitchSpinner.getValue() + "\n");
                if ("Round Robin".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("RR Time Quantum: " + rrTimeQuantumField.getText() + "\n");
                } else if ("MLFQ".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("MLFQ Q0 TQ: " + mlfqQ0TQField.getText() + ", Allot: " + mlfqQ0AllotField.getText() + "\n");
                    writer.write("MLFQ Q1 TQ: " + mlfqQ1TQField.getText() + ", Allot: " + mlfqQ1AllotField.getText() + "\n");
                    writer.write("MLFQ Q2 TQ: " + mlfqQ2TQField.getText() + ", Allot: " + mlfqQ2AllotField.getText() + "\n");
                    writer.write("MLFQ Q3 TQ: " + mlfqQ3TQField.getText() + ", Allot: " + mlfqQ3AllotField.getText() + "\n");
                }
                writer.write("\n");

                writer.write("--- Process Details ---\n");
                writer.write(String.format("%-12s %-15s %-12s\n", "Process ID", "Arrival Time", "Burst Time"));
                for (int i = 0; i < processInputTableModel.getRowCount(); i++) {
                    writer.write(String.format("%-12s %-15s %-12s\n",
                            processInputTableModel.getValueAt(i, 0),
                            processInputTableModel.getValueAt(i, 1),
                            processInputTableModel.getValueAt(i, 2)));
                }
                writer.write("\n");

                writer.write("      Gantt Chart     \n");
               
                StringBuilder ganttString = new StringBuilder();
                int currentGanttTime = 0;
                for (String entry : scheduler.getGanttChartOutput()) {
                    String cleanEntry = entry.trim().replace("|", "").replace("(Q0)", "").replace("(Q1)", "").replace("(Q2)", "").replace("(Q3)", "").trim();
                    if (!cleanEntry.isEmpty()) {
                        ganttString.append(String.format(" %2d| %-5s", currentGanttTime, cleanEntry));
                        currentGanttTime++;
                    }
                }
                ganttString.append(" ").append(currentGanttTime).append(" |\n");
                writer.write(ganttString.toString());
                writer.write("\n");


                    writer.write("        Process Metrics         \n");
                writer.write(String.format("%-12s %-15s %-12s %-18s %-18s %-15s %-15s\n",
                        "Process ID", "Arrival Time", "Burst Time", "Completion Time", "Turnaround Time", "Response Time", "Waiting Time"));
                for (Process p : scheduler.getCalculatedProcesses()) {
                    writer.write(String.format("%-12s %-15d %-12d %-18d %-18d %-15d %-15d\n",
                            p.getProcessId(), p.getArrivalTime(), p.getBurstTime(), p.getCompletionTime(),
                            p.getTurnaroundTime(), p.getResponseTime(), p.getWaitingTime()));
                }
                writer.write("\n");

                writer.write("      Average Metrics         \n");
                writer.write(String.format("Average Turnaround Time: %.2f\n", scheduler.getAvgTurnaroundTime()));
                writer.write(String.format("Average Response Time: %.2f\n", scheduler.getAvgResponseTime()));
                writer.write(String.format("Average Waiting Time: %.2f\n", scheduler.getAvgWaitingTime()));
                writer.write(String.format("Total Simulation Time: %d\n", scheduler.getCurrentTime())); 

                JOptionPane.showMessageDialog(this, "Results exported successfully to:\n" + fileToSave.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting results: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // --- SchedulerListener Implementation ---
    @Override
    public void onTick(int currentTime, String cpuActivity, List<String> ganttChartOutput, List<Queue<Process>> currentQueues, List<Process> currentProcessStates) {
        // Update live status displays
        currentTimeArea.setText("Current Time: " + currentTime);
        cpuActivityArea.setText("CPU: " + cpuActivity);

        // Update Queue Visualization
        StringBuilder queueText = new StringBuilder("Queues:\n");
        if (currentQueues != null && !currentQueues.isEmpty()) {
            String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();
            if ("MLFQ".equals(selectedAlgorithm)) {
                for (int i = 0; i < currentQueues.size(); i++) {
                    queueText.append("Q").append(i).append(": ");
                    if (currentQueues.get(i).isEmpty()) {
                        queueText.append("Empty\n");
                    } else {
                        queueText.append(currentQueues.get(i).stream().map(Process::getProcessId).collect(Collectors.joining(", "))).append("\n");
                    }
                }
            } else if ("FCFS".equals(selectedAlgorithm) || "Round Robin".equals(selectedAlgorithm)) {
                queueText.append("Ready Queue: ");
                if (currentQueues.get(0).isEmpty()) {
                    queueText.append("Empty\n");
                } else {
                    queueText.append(currentQueues.get(0).stream().map(Process::getProcessId).collect(Collectors.joining(", "))).append("\n");
                }
            } else if ("SJF".equals(selectedAlgorithm) || "SRTF".equals(selectedAlgorithm)) {
                 queueText.append("Ready Processes: ");
                 if (currentQueues.get(0).isEmpty()) {
                     queueText.append("Empty\n");
                 } else {
                     queueText.append(currentQueues.get(0).stream().map(Process::getProcessId).collect(Collectors.joining(", "))).append("\n");
                 }
            }
        } else {
            queueText.append("No active queues.\n");
        }
        queueVizArea.setText(queueText.toString());

      
        ganttChartPanel.setGanttData(currentProcessStates, ganttChartOutput);
        
       
        JScrollBar horizontalScrollBar = ((JScrollPane) ganttChartPanel.getParent().getParent()).getHorizontalScrollBar();
        horizontalScrollBar.setValue(horizontalScrollBar.getMaximum());

       
        metricsTableModel.setRowCount(0); // Clear and re-populate
        currentProcessStates.sort(Comparator.comparing(Process::getProcessId)); 
        for (Process p : currentProcessStates) {
            metricsTableModel.addRow(new Object[]{
                    p.getProcessId(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getCompletionTime() == 0 ? "-" : p.getCompletionTime(), // Show '-' if not completed
                    p.getTurnaroundTime() == 0 ? "-" : p.getTurnaroundTime(),
                    p.getResponseTime() == -1 ? "-" : p.getResponseTime(), // Show '-' if not started
                    p.getWaitingTime() == 0 ? "-" : p.getWaitingTime()
            });
        }
    }

    @Override
    public void onSimulationComplete(List<Process> calculatedProcesses, List<String> ganttChartOutput, double avgTurnaroundTime, double avgResponseTime, double avgWaitingTime) {
        metricsTableModel.setRowCount(0); 
        calculatedProcesses.sort(Comparator.comparing(Process::getProcessId)); 
        for (Process p : calculatedProcesses) {
            metricsTableModel.addRow(new Object[]{
                    p.getProcessId(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getCompletionTime(),
                    p.getTurnaroundTime(),
                    p.getResponseTime(),
                    p.getWaitingTime()
            });
        }

        // Display average metrics
        avgTurnaroundTimeField.setText(String.format("%.2f", avgTurnaroundTime));
        avgResponseTimeField.setText(String.format("%.2f", avgResponseTime));
        avgWaitingTimeField.setText(String.format("%.2f", avgWaitingTime));
        totalSimulationTimeField.setText(String.valueOf(scheduler.getCurrentTime())); 
        
      
        ganttChartPanel.setGanttData(calculatedProcesses, ganttChartOutput);
        
        
        exportButton.setEnabled(true);
        enableInputControls(true); 
    }

    @Override
    public void onReset() {
       
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            // Fallback to default if Nimbus is not available
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new CPUSchedulerVisualizationGUI());
    }
}

