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
 * Created on Aug 20, 2005
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot;

import java.awt.AlphaComposite;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import net.bluecow.robot.LevelConfig.Switch;
import bsh.EvalError;

/**
 * The GameLoop represents the main loop of the game while it is in operation.
 *
 * @author fuerth
 * @version $Id$
 */
public class GameLoop implements Runnable {

    private List<Robot> robots = new ArrayList<Robot>();
    
    private final Playfield playfield;
    
    /**
     * The current level that the user is playing.
     */
    private LevelConfig level;
    
    /**
     * LevelConfigs that contain ghost robots and switches.  They need to be
     * reset when the real level is reset.
     */
    private List<LevelConfig> ghostLevels = new ArrayList<LevelConfig>();
    
    /**
     * Set this to true to abort the current game.
     */
    private boolean stopRequested;

    /**
     * This value starts off false, then becomes true when the
     * run() method is invoked.
     */
    private boolean running;

    /**
     * Counts how many loops the game has gone through since it was started
     * (by calling the run() method).
     */
    private int loopCount;
    
    /**
     * Gets set to true if and when the robot reaches the goal.
     */
    private boolean goalReached;
    
    /**
     * Time to sleep between loops (in milliseconds).
     */
    private int frameDelay = 50;
    
    /**
     * @param robot
     * @param playfield
     */
    public GameLoop(Collection<Robot> robots, LevelConfig level, Playfield playfield) {
        this.level = level;
        this.playfield = playfield;
        for (Robot r : robots) {
            addRobot(r);
        }
    }
    
    public final void addRobot(Robot robot) {
        robots.add(robot);
    }
    
    /**
     * Removes the given robot from this game loop.
     */
    public final void removeRobot(Robot robot) {
        robots.remove(robot);
    }

    /**
     * Returns the playfield that this game loop is attached to.
     */
    public Playfield getPlayfield() {
        return playfield;
    }
    
    /**
     * Adds a ghost level to this game loop which will behave properly when
     * the loop has been reset.
     * 
     * <p>XXX there should be a way to remove a ghost level
     */
    public void addGhostLevel(LevelConfig l) {
        ghostLevels.add(l);
        for (Robot r : l.getRobots()) {
            addRobot(r); // long-term, it might be better to keep these separate from the real ones
        }
        addGhostsToPlayfield(l);
    }
    
    private void addGhostsToPlayfield(LevelConfig l) {
        for (Robot r : l.getRobots()) {
            playfield.addRobot(r, AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
    }
    
    public void run() {
        synchronized (this) {
            if (running) {
                throw new IllegalStateException("Already running!");
            } else {
                running = true;
            }
        }
        
        playfield.setAsyncRepaint(false);
        
        pcs.firePropertyChange("running", false, true);
        
        try {
            while (running) {
                
                if (running) {
                    singleStep();
                }
                
                try {
                    Thread.sleep(frameDelay);
                } catch (InterruptedException ex) {
                    System.out.println("GameLoop was Interrupted while sleeping.");
                }
            }
        } finally {
            halt();
        }
    }

    public void singleStep() {
        synchronized (this) {
            if (stopRequested || goalReached) {
                halt();
                return;
            }
            loopCount++;
        }

        boolean allGoalsReached = true;
        for (Robot robot : robots) {
            boolean thisGoalReached = robot.isGoalReached();
            if (!thisGoalReached) {
                robot.updateSensors();
                for (int i = 0; i < robot.getEvalsPerStep(); i++) {
                    // should this loop be in the robot's circuit instead?
                    robot.getCircuit().evaluateOnce();
                }
                
                Point2D oldPos = robot.getPosition();
                robot.move();
                
                if (!isSameSquare(oldPos, robot.getPosition())) {
                    
                    /*
                     * This has to be the robot's level (as opposed to the 
                     * level in this game loop) for ghosts to work properly
                     */
                    LevelConfig l = robot.getLevel();
                    
                    Switch exitingSwitch = l.getSwitch(oldPos);
                    Switch enteringSwitch = l.getSwitch(robot.getPosition());
                    try {
                        // TODO it might be better (simpler, more reliable) to evaluate the scripts here instead of in the switches
                        if (exitingSwitch != null) exitingSwitch.onExit(robot, playfield);
                        if (enteringSwitch != null) enteringSwitch.onEnter(robot, playfield);
                    } catch (EvalError e) {
                        JOptionPane.showMessageDialog(null, "Error evaluating switch:\n"+e.getMessage());
                    }
                }
                // XXX: should we re-check if the goal is reached, or wait for the next loop?
            }
            allGoalsReached &= thisGoalReached; 
        }
        
        playfield.setFrameCount(loopCount);
        playfield.repaint();
        
        if (allGoalsReached) {
            setGoalReached(true);
        }
    }

    private void halt() {
        boolean wasRunning;
        synchronized (this) {
            wasRunning = running;
            running = false;
            stopRequested = false;
        }
        playfield.setAsyncRepaint(true);
        if (wasRunning) {
            pcs.firePropertyChange("running", true, false);
        }
    }
    
    private static boolean isSameSquare(Point2D p1, Point2D p2) {
        return (Math.floor(p1.getX()) == Math.floor(p2.getX())) &&
               (Math.floor(p1.getY()) == Math.floor(p2.getY()));
    }
    
    /**
     * Tells whether or not the game loop is currently running.  This is a bound
     * property; to recieve change notifications, register a property change listener
     * for the "running" property.
     */
    public synchronized boolean isRunning() {
        return running;
    }
    
    /**
     * Calling this method with the parameter <tt>true</tt> will cause the
     * game loop to halt during the next loop iteration. If you need to be
     * notified when the loop has halted, you can sign up for the "running"
     * property change event.
     */
    public synchronized void setStopRequested(boolean v) {
        stopRequested = v;
    }

    /**
     * Returns the number of loops this game loop has executed so far.
     */
    public synchronized int getLoopCount() {
        return loopCount;
    }
    
    /**
     * This becomes true when the robot has reached its goal. When the goal has
     * been reached, the game loop stops itself. This flag can be reset by
     * calling resetState(), which you will have to do before the game loop can
     * be restarted.
     */
    public synchronized boolean isGoalReached() {
        return goalReached;
    }
    
    private synchronized void setGoalReached(boolean v) {
        if (goalReached != v) {
            goalReached = v;
            pcs.firePropertyChange("goalReached", !goalReached, goalReached);
        }
    }

    /**
     * Sets the amount of time that the loop will sleep between frames.
     *
     * @param delayInMS The amount of time to sleep, in milliseconds.
     */
    public void setFrameDelay(int delayInMS) {
        frameDelay = delayInMS;
    }
    
    public int getFrameDelay() {
        return frameDelay;
    }
    
    /**
     * Resets this game loop, its levelconfig and robots, the ghost levels
     * and their robots, and the playfield to their initial states.
     * 
     * @throws IllegalStateException if you call this method when the game loop
     * is running
     */
    public void resetState() {
        if (isRunning()) {
            throw new IllegalStateException("You can't reset the loop when it's running.");
        }
        setGoalReached(false);
        loopCount = 0;
        level.resetState();
        
        for (LevelConfig ghostLevel : ghostLevels) {
            ghostLevel.resetState();
        }

        // this list includes the ghost robots (subject to change in the future)
        for (Robot robot : robots) {
            robot.resetState();
        }
        
        playfield.setLevel(level);
        playfield.setFrameCount(null);
        playfield.setAsyncRepaint(true);
        
        for (LevelConfig ghostLevel : ghostLevels) {
            addGhostsToPlayfield(ghostLevel);
        }
    }

    public LevelConfig getLevelConfig() {
        return level;
    }
    
    // PROPERTY CHANGE STUFF (for notifying of game wins)
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }
}
