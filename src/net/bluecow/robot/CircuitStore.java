/*
 * Copyright (c) 2007, Jonathan Fuerth
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Jonathan Fuerth nor the names of other
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Created on Mar 13, 2006
 *
 * This code belongs to Jonathan Fuerth.
 */
package net.bluecow.robot;

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluecow.robot.gate.Gate;

/**
 * The CircuitStore class is responsible for saving and loading descriptions
 * of circuits.
 * 
 * @author fuerth
 * @version $Id$
 */
public class CircuitStore {

    public static final String MAGIC = "CIRCUIT 2.0";
    
    /**
     * Saves a description of all the gates in the given circuit editor to
     * the given output stream.
     */
    public static void save(OutputStream out, Collection<Robot> robots) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
        int nextId = 0;
        Map<Gate,String> idmap = new HashMap<Gate,String>();
        pw.printf(MAGIC+"\n");
        for (Robot robot : robots) {
            pw.printf("*Robot %s\n", robot.getId());
            Circuit c = robot.getCircuit();
            for (Gate g : c.getGates()) {
                Rectangle r = g.getBounds();
                String id;
                if (g instanceof Robot.RobotSensorOutput) {
                    id = g.getLabel();
                } else if (g instanceof Robot.RobotInputsGate) {
                    id = "Inputs";
                } else {
                    id = String.valueOf(nextId++);
                }
                
                idmap.put(g, id);
                pw.printf("%s [%d,%d,%d,%d] %s\n",
                        id, r.x, r.y, r.width, r.height, g.getClass().getName());
            }
            
            pw.printf("*Connections\n");
            
            for (Gate g : c.getGates()) {
                String id = idmap.get(g);
                Gate.Input[] inputs = g.getInputs();
                for (int i = 0; i < inputs.length; i++) {
                    if (inputs[i].getConnectedGate() != null) {
                        String sourceid = idmap.get(inputs[i].getConnectedGate());
                        pw.printf("%s:%d <- %s\n", id, i, sourceid);
                    }
                }
            }
        }        
        pw.flush();
    }
    
    /**
     * Reads the descriptions of circuits for the one or more robots represented
     * in the input stream, and sets the newly-loaded circuit on each robot. It
     * is an error for the input stream to describe a circuit for a robot who isn't
     * in the given list, or for the input stream to describe more than one circuit
     * for the same robot. However, there is no check for the latter condition yet,
     * so don't count on that error checking (but also don't assume it will always
     * be safe to violate that condition.. future versions might throw exceptions for
     * that error).
     * 
     * @throws IOException If the input stream can't be read
     * @throws IllegalArgumentException if the input stream describes a circuit for a
     * robot who is not in the given list (robots are matched by their names, case
     * sensitively).
     */
    public static void load(InputStream in, Collection<Robot> robots) throws IOException {
        Pattern robotHeader = Pattern.compile("\\*Robot (.*)");
        Pattern gateLine = Pattern.compile("(\\w+) \\[([0-9]+),([0-9]+),([0-9]+),([0-9]+)\\] (.*)");
        Pattern connLine = Pattern.compile("(\\w+):([0-9]+) <- (\\w+)");
        LineNumberReader br = new LineNumberReader(new InputStreamReader(in));

        Map<String, Robot> roboMap = new HashMap<String, Robot>();
        for (Robot r : robots) {
            roboMap.put(r.getId(), r);
        }
        
        String line = br.readLine();
        if (!MAGIC.equals(line)) {
            throw new FileFormatException(
                    "The first line must be exactly '"+MAGIC+"'" +
                    " (with no leading or trailing space)", 1, line, -1);
        }

        
        // read the initial robot header line (subsequent headers are read at the bottom of the loop)
        line = br.readLine();
        
        while (line != null) {
            // find the current robot
            Robot robot;
            {
                Matcher m = robotHeader.matcher(line);
                if (m.matches()) {
                    robot = roboMap.get(m.group(1));
                } else {
                    throw new FileFormatException("Expected robot header line", br.getLineNumber(), line, -1);
                }
                if (robot == null) {
                    throw new IllegalArgumentException("Robot '"+m.group(1)+"' is referenced in the circuit file, but is not in the given collection");
                }
            }
            
            // read gate positions and fill idmap
            Map<String,Gate> idmap = new HashMap<String,Gate>();
            while ( (line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("*Connections")) break;
                Matcher m = gateLine.matcher(line);
                if (!m.matches()) {
                    throw new FileFormatException(
                            "Invalid line format for gate position",
                            br.getLineNumber(), line, -1);
                }
                String id = m.group(1);
                int x = 0;
                int y = 0;
                int width = 0;
                int height = 0;
                try {
                    x = Integer.parseInt(m.group(2));
                } catch (NumberFormatException ex) {
                    throw new FileFormatException("Could not parse X coordinate", br.getLineNumber(), line, m.start(2));
                }
                try {
                    y = Integer.parseInt(m.group(3));
                } catch (NumberFormatException ex) {
                    throw new FileFormatException("Could not parse Y coordinate", br.getLineNumber(), line, m.start(3));
                }
                try {
                    width = Integer.parseInt(m.group(4));
                } catch (NumberFormatException ex) {
                    throw new FileFormatException("Could not parse width", br.getLineNumber(), line, m.start(4));
                }
                try {
                    height = Integer.parseInt(m.group(5));
                } catch (NumberFormatException ex) {
                    throw new FileFormatException("Could not parse height", br.getLineNumber(), line, m.start(5));
                }
                String gateClassName = m.group(6);
                Rectangle bounds = new Rectangle(x, y, width, height);
                Gate g;
                if ("net.bluecow.robot.Robot$RobotSensorOutput".equals(gateClassName)) {
                    Gate[] robotGates = robot.getOutputs();
                    g = null;
                    for (Gate roboGate : robotGates) {
                        if (roboGate.getLabel().equals(id)) {
                            g = roboGate; 
                        }
                    }
                    if (g == null) throw new IOException("Unknwon Robot Sensor '"+id+"'");
                } else if ("net.bluecow.robot.Robot$RobotInputsGate".equals(gateClassName)) {
                    g = robot.getInputsGate();
                } else {
                    try {
                        g = (Gate) Class.forName(gateClassName).newInstance();
                        robot.getCircuit().addGate(g, bounds);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                        throw new FileFormatException(
                                "Couldn't create gate class: "+e.getMessage(),
                                br.getLineNumber(), line, -1);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new FileFormatException(
                                "Couldn't access gate constructor: "+e.getMessage(),
                                br.getLineNumber(), line, -1);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        throw new FileFormatException(
                                "Couldn't find gate class: "+e.getMessage(),
                                br.getLineNumber(), line, -1);
                    }
                }
                idmap.put(id, g);
            }
            
            // read gate connections
            while ( (line = br.readLine()) != null) {
                if (line.startsWith("*Robot ")) break;
                Matcher m = connLine.matcher(line);
                if (!m.matches()) {
                    throw new FileFormatException(
                            "Invalid connection line. Required form is " +
                            "TARGETID:INPUT# <- CONNECTTO", br.getLineNumber(), line, -1);
                }
                String id = m.group(1);
                int inpNum = Integer.parseInt(m.group(2));
                String connTo = m.group(3);
                
                Gate target = idmap.get(id);
                Gate.Input targetInput = target.getInputs()[inpNum];
                Gate source = idmap.get(connTo);
                
                targetInput.connect(source);
            }
        }
    }
}
