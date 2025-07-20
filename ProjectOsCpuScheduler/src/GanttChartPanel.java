/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package srcs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;

/**
 *
 * @author Nj Arnado
 */

public class GanttChartPanel extends JPanel {
    private List<Process> processes; 
    private List<String> ganttChartOutput;
    private Map<String, Color> processColors; // Map IDs to colors
    private int timeScale = 20; // px per time u
    private int barHeight = 30; // 
    private int yOffset = 20; // 

  //initializes the backgorund and the colors
    public GanttChartPanel() {
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(800, 150)); // preferred size
        
        processColors = new HashMap<>();
        // pre-define some colors for processes.
        processColors.put("P1", new Color(255, 100, 100)); // Light Red
        processColors.put("P2", new Color(100, 255, 100)); // Light Green
        processColors.put("P3", new Color(100, 100, 255)); // Light Blue
        processColors.put("P4", new Color(255, 200, 100)); // Light Orange
        processColors.put("P5", new Color(200, 100, 255)); // Light Purple
        processColors.put("P6", new Color(100, 255, 255)); // Light Cyan
        processColors.put("P7", new Color(255, 100, 200)); // Pink
        processColors.put("P8", new Color(200, 255, 100)); // Lime Green
        processColors.put("P9", new Color(100, 200, 255)); // Sky Blue
        processColors.put("P10", new Color(255, 150, 50)); // Dark Orange
        processColors.put("IDLE", Color.DARK_GRAY.darker()); // Specific color for IDLE
        processColors.put("CS", new Color(150, 50, 200)); // Color for Context Switch (Purple-ish)
    }

    
    public void setGanttData(List<Process> processes, List<String> ganttChartOutput) {
        this.processes = processes; 
        this.ganttChartOutput = ganttChartOutput;
        repaint(); 
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        
        if (ganttChartOutput == null || ganttChartOutput.isEmpty()) {
            // displaying a message there is no data
            g.setColor(Color.CYAN);
            g.drawString("No Gantt Chart data available. Run a simulation.", 10, getHeight() / 2);
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // For smoother edges
        g2d.setFont(new Font("Monospaced", Font.BOLD, 10)); //font

        int currentX = 10; 

        // drawing the Gantt chart bars (colorful boxes)
        for (String entry : ganttChartOutput) {
            //clean up the process
            String processId = entry.trim().replace("|", "").replace("(Q0)", "").replace("(Q1)", "").replace("(Q2)", "").replace("(Q3)", "").trim();
            int duration = 1; // each entry represents 1 time u

            // get color for id or if it is not found this can generate a new one 
            Color color = processColors.computeIfAbsent(processId, k -> generateRandomColor());

            g2d.setColor(color);
            g2d.fillRect(currentX, yOffset, duration * timeScale, barHeight); //rectangle
            g2d.setColor(Color.BLACK); // border color
            g2d.drawRect(currentX, yOffset, duration * timeScale, barHeight); //border 

           
            g2d.setColor(Color.WHITE); // label
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(processId);
            int textX = currentX + (duration * timeScale - textWidth) / 2; //center horizontally
            int textY = yOffset + ((barHeight - fm.getHeight()) / 2) + fm.getAscent(); // center vertically
            g2d.drawString(processId, textX, textY);

            currentX += duration * timeScale; 
        }

        // Time scale below the bar
        g2d.setColor(Color.CYAN); //color for labels and markers
        int timeLabelY = yOffset + barHeight + 15; //position below the bar

       
        g2d.drawLine(10, yOffset + barHeight + 5, currentX, yOffset + barHeight + 5);

        // draw time labels and tick marks 
        for (int i = 0; i <= currentX / timeScale; i++) {
            int xPos = 10 + i * timeScale;
            g2d.drawLine(xPos, yOffset + barHeight + 5, xPos, yOffset + barHeight + 10); // Tick mark
            g2d.drawString(String.valueOf(i), xPos - (i == 0 ? 0 : 5), timeLabelY); // Time label, adjust for centering
        }
        
        int requiredWidth = currentX + 20; // Add some padding on the right
        int requiredHeight = yOffset + barHeight + 30; // Height for bars + labels + padding

        Dimension currentPreferredSize = getPreferredSize();
        if (currentPreferredSize.width < requiredWidth || currentPreferredSize.height < requiredHeight) {
            setPreferredSize(new Dimension(Math.max(currentPreferredSize.width, requiredWidth), Math.max(currentPreferredSize.height, requiredHeight)));
            revalidate(); 
        }
    }

    //Generates a random, light color for new processes to ensure visibility.
     
    private Color generateRandomColor() {
        Random rand = new Random();
        float r = 0.5f + rand.nextFloat() * 0.5f;
        float g = 0.5f + rand.nextFloat() * 0.5f; 
        float b = 0.5f + rand.nextFloat() * 0.5f; 
        return new Color(r, g, b);
    }
}