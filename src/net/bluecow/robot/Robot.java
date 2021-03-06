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
package net.bluecow.robot;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.bluecow.robot.GameConfig.GateConfig;
import net.bluecow.robot.GameConfig.SensorConfig;
import net.bluecow.robot.gate.AbstractGate;
import net.bluecow.robot.gate.Gate;
import net.bluecow.robot.sprite.Sprite;

/**
 * The robot.
 */
public class Robot implements Labelable {

    /**
     * Controls debugging features of this class.
     */
    private static boolean debugOn = false;
    
    /**
     * Prints the given format string to stdout if debugOn is true.
     */
    private static void debugf(String fmt, Object ... args) {
        if (debugOn) System.out.format(fmt, args);
    }
    
    private static final int MOVING_UP = 1 << 0;
    private static final int MOVING_DOWN = 1 << 1;
    private static final int MOVING_LEFT = 1 << 2;
    private static final int MOVING_RIGHT = 1 << 3;
	
    /**
     * A bitmask of the directions this robot is currently moving.
     * Possible values to OR together are <tt>MOVING_UP</tt>,
     * <tt>MOVING_RIGHT</tt>, <tt>MOVING_LEFT</tt>, and <tt>MOVING_DOWN</tt>.
     */
    private int movingDirection = 0;
    
    /**
     * A count of how many frames in a row this robot has been moving
     * in the same direction.
     */
    private int movingFrame = 0;
    
	private LevelConfig level;
	private Point2D.Float position;
	private Sprite sprite;
	private float stepSize;
    private int evalsPerStep; 
    
	// Inputs that make the robot do stuff
	private RobotInput upInput = new RobotInput("Up");
	private RobotInput downInput = new RobotInput("Down");
	private RobotInput leftInput = new RobotInput("Left");
	private RobotInput rightInput = new RobotInput("Right");
    private RobotInputsGate robotInputsGate 
    				= new RobotInputsGate(new RobotInput[] {upInput, downInput, leftInput, rightInput});
	
	/** A collection of outputs that report the robot's current state and surroundings. */
	private Map<SensorConfig,RobotSensorOutput> outputs;

    /** Indicates whether or not this robot has reached its goal. */
    private boolean goalReached;
    
    /** The position that this robot starts out on. */
    private Point2D.Float startPosition;
    
    /** This robot's scripting identifier */
    private String id;
    
    /** The circuit that controls this robot's behaviour. */
    private Circuit circuit;

    /** This robot's user-visible name */
    private String labelText;

    /**
     * True if this robot's label should be visible.
     */
    private boolean labelEnabled;
    
    /**
     * The position of this robot's label, relative to the robot.
     */
    private Direction labelDirection = Direction.EAST;

    /**
     * The previous direction, in radians, that the robot was heading in. This
     * is the most recent value returned by {@link #getIconHeading()}.
     */
    private double prevHeading;
    
    /**
     * The original heading that the robot should start off facing (and revert
     * to after a reset).
     */
    private double initialHeading;
    
    private static final Dimension DEFAULT_GATE_SIZE = new Dimension(22,20);
	
    /**
     * Creates a new robot, initialising its properties to those given
     * in the argument list.
     * <p>
     * Warning: This constructor does not cover all robot properties!
     * In fact, it should probably not exist because it's a bit of
     * a red herring in that regard.
     * 
     * @param id The robot's scripting ID
     * @param name The robot's name (this is its player-visible label)
     * @param level The level the robot lives in
     * @param sensorList The types of sensors this robot has
     * @param gateConfigs The types of gates this robot can use
     * @param sprite This robot's graphical representation
     * @param startPosition The initial location of this robot in the
     *      level's playfield. (0,0) is top left.
     * @param stepSize The distance this robot travels in one step
     *      (expressed as a number of playfield suqares)
     * @param circuit The circuit that governs this robot's behaviour (if null,
     * an appropriate one will be created for you).
     * @param evalsPerStep The number of circuit evaluations this robot
     *      will perform before taking one step
     */
    public Robot(String id, String name, LevelConfig level, List<SensorConfig> sensorList,
            Collection<GateConfig> gateConfigs,
            Sprite sprite, Point2D.Float startPosition, float stepSize,
            Circuit circuit, int evalsPerStep) {
        this.id = id;
        this.labelText = name;
        this.level = level;
        this.sprite = sprite;
        this.startPosition = startPosition;
        setPosition(startPosition);
        this.stepSize = stepSize;
        this.evalsPerStep = evalsPerStep;
        
        outputs = new LinkedHashMap<SensorConfig, RobotSensorOutput>();
        for (SensorConfig sensor : sensorList) {
            outputs.put(sensor, new RobotSensorOutput(sensor));
        }
        
        this.circuit = new Circuit(
                name, robotInputsGate, outputs.values(),
                gateConfigs, DEFAULT_GATE_SIZE);
        if (circuit != null) {
            for (Map.Entry<Class<? extends Gate>, Integer> allowance :
                   circuit.getGateAllowances().entrySet()) {
                this.circuit.addGateAllowance(allowance.getKey(), allowance.getValue());
            }
        }
	}
    
    /**
     * Copy constructor.  Makes an independant copy of the given robot, also
     * duplicating any objects owned by the source robot which are mutable.
     * <p>
     * Note that the level config is more like a parent pointer, so it's not
     * duplicated (that would be a bit awkward).
     * 
     * @param src The robot to copy.
     */
    public Robot(Robot src, LevelConfig targetLevel) {
        copyFrom(src, targetLevel);
    }
    
    /**
     * Makes this robot an independant copy of the given robot, duplicating any
     * objects owned by the source robot which are mutable.
     * <p>
     * The level that this robot belongs to will not be changed: Copying
     * a robot updates all its properties to the source robot, but does not
     * move it into the same level as the source robot.
     * 
     * @param src The robot to copy.
     */
    public final void copyFrom(Robot src, LevelConfig targetLevel) {
        this.level = targetLevel;
        this.id = src.id;
        this.labelText = src.labelText;
        this.sprite = (src.sprite == null ? null : src.sprite.clone());
        this.startPosition = new Point2D.Float(
                (float) src.startPosition.getX(),
                (float) src.startPosition.getY());
        this.position = new Point2D.Float(
                (float) src.position.getX(),
                (float) src.position.getY());
        this.stepSize = src.stepSize;
        this.evalsPerStep = src.evalsPerStep;
        this.labelDirection = src.labelDirection;
        this.labelEnabled = src.labelEnabled;
        this.movingDirection = src.movingDirection;
        this.movingFrame = src.movingFrame;
        this.prevHeading = src.prevHeading;
        this.initialHeading = src.initialHeading;
        
        outputs = new LinkedHashMap<SensorConfig, RobotSensorOutput>();
        for (Map.Entry<SensorConfig, RobotSensorOutput> entry : src.outputs.entrySet()) {
            SensorConfig sensor = entry.getKey();
            outputs.put(sensor, new RobotSensorOutput(sensor));
        }
        
        this.circuit = new Circuit(src.circuit, robotInputsGate, outputs.values());
    }
    
    public void move() {
        int direction = 0;
	    if (upInput.getState() == true) {
	        moveUp();
            direction |= MOVING_UP;
	    }
	    if (downInput.getState() == true) {
	        moveDown();
            direction |= MOVING_DOWN;
	    }
	    if (leftInput.getState() == true) {
	        moveLeft();
            direction |= MOVING_LEFT;
	    }
	    if (rightInput.getState() == true) {
	        moveRight();
            direction |= MOVING_RIGHT;
	    }
        
        if (direction == movingDirection) {
            movingFrame++;
        } else {
            movingDirection = direction;
            movingFrame = 0;
        }
	}
	
    /**
     * Updates all the sensor outputs to correspond with the square this robot
     * is currently occupying.
     */
	public void updateSensors() {
	    Square s = level.getSquare(position.x, position.y);
        Collection squareSensors = s.getSensorTypes();
        for (Map.Entry<SensorConfig, RobotSensorOutput> entry : outputs.entrySet()) {
            entry.getValue().setState(squareSensors.contains(entry.getKey()));
        }
	}
	
	private void moveLeft() {
		if (position.x > 0
				&& level.getSquare(position.x-stepSize, position.y).isOccupiable()) {
			position.x -= stepSize;
		}
	}
	
	private void moveRight() {
		if (position.x < level.getWidth()
				&& level.getSquare(position.x+stepSize, position.y).isOccupiable()) {
			position.x += stepSize;
		}
	}
	
	private void moveDown() {
        boolean atBottom = position.y >= level.getHeight();
        boolean obstacleInTheWay = !level.getSquare(position.x, position.y+stepSize).isOccupiable();
        if ( (!atBottom)	&& (!obstacleInTheWay)) {
			position.y += stepSize;
		}
	}
	
	private void moveUp() {
		if (position.y > 0
				&& level.getSquare(position.x, position.y-stepSize).isOccupiable()) {
			position.y -= stepSize;
		}
	}
		
	/**
	 * The RobotSensorOutput class represents an environmental sensor
	 * attached to the robot.  It can change state as the robot's
	 * immediate surroundings change.  For example, when the robot
	 * moves from a red square to a green square, the red output will
	 * change to false and the green output will change to true.
	 */
	class RobotSensorOutput extends AbstractGate {

		private static final int SENSOR_OUTPUT_GATE_HEIGHT = 15;

        public RobotSensorOutput(SensorConfig config) {
            super(config.getId());
            
            // init width to same size as output stick. position and height will
            // be taken care of by the circuit's layout.
            setBounds(new Rectangle(0, 0, getOutputStickLength(), SENSOR_OUTPUT_GATE_HEIGHT));
		}

		/** Sets the state of this output.  Only methods in Robot should call this. */
		public void setState(boolean v) {
            nextOutputState = v;
            latchOutput();
		}
		
        @Override
		public Gate.Input[] getInputs() {
			return new Gate.Input[0];
		}
		
        public void evaluateInput() {
            // doesn't do anything because this gate's state gets set elsewhere
            // XXX: this could check which colour of square the robot is over, and update the state accordingly
        }

        @Override
        protected boolean isInputInverted() {
            return false;
        }

        @Override
        protected boolean isOutputInverted() {
            return false;
        }
        
        @Override
        public void drawBody(Graphics2D g2) {
            // empty body
        }

        /**
         * This method is not implemented, since RobotSensorOutputs are
         * necessarily tied to a particular robot in a 1:1 relationship.
         */
        public Gate createDisconnectedCopy() {
            throw new UnsupportedOperationException("Can't copy robot sensor outputs");
        }

        /**
         * Returns "ROBOT_SENSOR_OUTPUT".
         */
        public String getType() {
            return "ROBOT_SENSOR_OUTPUT";
        }
        
	}
	
	/**
	 * The RobotInput class represents an input to the robot which
	 * will cause it to perform some action.  For example, the upInput
	 * and downInput instances cause the robot to move up or down.
	 */
	private class RobotInput implements Gate.Input {
	    private Gate inputGate;
	    private String label;
		
	    public RobotInput(String label) {
	        this.label = label;
	    }
	    
		public void connect(Gate g) {
			inputGate = g;
            robotInputsGate.fireInputConnected(this);
		}
		
		public boolean getState() {
		    if (inputGate != null) {
		        return inputGate.getOutputState();
		    } else {
		        return false;
		    }
		}

        public Gate getConnectedGate() {
            return inputGate;
        }
        
        /**
         * Returns the robotInputsGate instance.
         */
        public Gate getGate() {
            return robotInputsGate;
        }
        
        public String getLabel() {
            return label;
        }

        public Point getPosition() {
            return AbstractGate.calcInputPosition(this);
        }

	}
	
    /**
     * Returns the number of circuit evaluations that should be made
     * on this robot before the move() routine is called.
     */
    public int getEvalsPerStep() {
        return evalsPerStep;
    }
    
    public void setEvalsPerStep(int evalsPerStep) {
        this.evalsPerStep = evalsPerStep;
    }
    
	/**
	 * Returns the input that will cause the robot to move UP when the
	 * input state is TRUE.
	 */
	public Gate.Input getUpInput() {
		return upInput;
	}
	
	/**
	 * Returns the input that will cause the robot to move DOWN when the
	 * input state is TRUE.
	 */
	public Gate.Input getDownInput() {
		return downInput;
	}
	
	/**
	 * Returns the input that will cause the robot to move LEFT when the
	 * input state is TRUE.
	 */
	public Gate.Input getLeftInput() {
		return leftInput;
	}
	
	/**
	 * Returns the input that will cause the robot to move RIGHT when the
	 * input state is TRUE.
	 */
	public Gate.Input getRightInput() {
		return rightInput;
	}
	
	/**
	 * The RobotInputsGate is basically a place to hold the collection
	 * of this robot's inputs.  Its output state is meaningless.
	 */
	public class RobotInputsGate extends AbstractGate {

	    private RobotInput[] inputs;

	    /**
	     * Creates the instance of this robot's input gate.
	     * 
	     * @param inputs All the inputs that this robot has.
	     */
	    public RobotInputsGate(RobotInput[] inputs) {
	        super("Robot inputs");
	        this.inputs = inputs;
	    }

        /**
         * Returns false because this gate doesn't appear to have an output,
         * (and conceptually, it shouldn't have one at all), but it would be
         * possible to accidentally connect something to this gate's output.
         * Making such a connection is confusing.  If you haven't guessed yet,
         * this has happened to a few people, and they got confused. :)
         * 
         * @return false.
         */
        @Override
        public boolean isOutputConnectable() {
            return false;
        }
        
	    public Input[] getInputs() {
	        return inputs;
	    }

        public void evaluateInput() {
            // this gate always outputs false
        }

        /**
         * Override is here to make this method visible to the RobotInput class.
         * This method behaves identically to the AbstractGate implementation.
         */
        @Override
        protected void fireInputConnected(Input input) {
            super.fireInputConnected(input);
        }
        
        @Override
        protected boolean isInputInverted() {
            return false;
        }

        @Override
        protected boolean isOutputInverted() {
            return false;
        }
        
        @Override
        public void drawBody(Graphics2D g2) {
            // empty body
        }

        /**
         * This method is not implemented, since Robot Inputs are
         * tied to a particular robot in a 1:1 relationship.
         */
        public Gate createDisconnectedCopy() {
            throw new UnsupportedOperationException("Can't copy robot sensor outputs");
        }

        /**
         * Returns "ROBOT_INPUTS_GATE".
         */
        public String getType() {
            return "ROBOT_INPUTS_GATE";
        }
	}

	// ACCESSORS and MUTATORS

    /**
     * Tells the icon's appropriate heading (in radians) based on its current direction
     * of travel.
     */
    public double getIconHeading() {
        double theta;
        if (movingDirection == 0) {
            theta = prevHeading;
        } else if (movingDirection == MOVING_UP) {
            theta = 0.0;
        } else if (movingDirection == (MOVING_UP | MOVING_RIGHT)) {
            theta = 0.25 * Math.PI;
        } else if (movingDirection == MOVING_RIGHT) {
            theta = 0.5 * Math.PI;
        } else if (movingDirection == (MOVING_DOWN | MOVING_RIGHT)) {
            theta = 0.75 * Math.PI;
        } else if (movingDirection == MOVING_DOWN) {
            theta = 1.0 * Math.PI;
        } else if (movingDirection == (MOVING_DOWN | MOVING_LEFT)) {
            theta = 1.25 * Math.PI;
        } else if (movingDirection == MOVING_LEFT) {
            theta = 1.5 * Math.PI;
        } else if (movingDirection == (MOVING_UP | MOVING_LEFT)) {
            theta = 1.75 * Math.PI;
        } else {
            // Robot is working against itself! Just spin around wildly
            theta = Math.random() * Math.PI * 2.0;
        }
        prevHeading = theta;
        return theta;
    }

    /**
     * Returns true if this robot is moving (has changed position in the past frame).
     */
    public boolean isMoving() {
        return movingDirection != 0;
    }

    public LevelConfig getLevel() {
        return level;
    }
    
    public void setLevel(LevelConfig config) {
        this.level = config;
    }

    public String getLabel() {
        return labelText;
    }
    
    public void setLabel(String l) {
        circuit.setName(l);
        labelText = l;
    }

    public Direction getLabelDirection() {
        return labelDirection;
    }

    public void setLabelDirection(Direction labelDirection) {
        this.labelDirection = labelDirection;
    }

    public boolean isLabelEnabled() {
        return labelEnabled;
    }

    public void setLabelEnabled(boolean labelEnabled) {
        this.labelEnabled = labelEnabled;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
	public Sprite getSprite() {
		return sprite;
	}

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    /**
     * Returns a copy of this robot's current position.
     */
    public Point2D getPosition() {
        return new Point2D.Float(position.x, position.y);
    }
    
    /**
     * Sets this robot's position by making a copy of the given point.
     * 
     * <p>This method is final because it is called from the constructor.
     */
    public final void setPosition(Point2D position) {
        this.position = new Point2D.Float((float) position.getX(), (float) position.getY());
    }

    /**
     * Returns a copy of this robot's starting position.
     */
    public Point2D.Float getStartPosition() {
        return new Point2D.Float(startPosition.x, startPosition.y);
    }
    
    /**
     * Sets this robot's starting position by making a copy of the given point.
     * The starting position is the point that this robot will return to
     * when it is reset.
     * 
     * <p>This method is final because it is called from the constructor.
     */
    public final void setStartPosition(Point2D position) {
        this.startPosition = new Point2D.Float((float) position.getX(), (float) position.getY());
        debugf("Set start position of %s: (%2.1f,%2.1f)\n",
                getId(), startPosition.getX(), startPosition.getY());
    }

    /**
     * Sets this robot's initial heading, as well as this robot's
     * internal idea of its previous heading.  This is the direction
     * this robot will be facing initially, as well as after
     * every reset.
     * 
     * @param initialHeadingRadians The heading measured in radians.
     * 0 is north (up), and positive values rotate in the clockwise
     * direction.
     */
    public void setInitialHeading(double initialHeadingRadians) {
        this.initialHeading = initialHeadingRadians;
        this.prevHeading = initialHeadingRadians;
    }

    /**
     * Returns this robot's initial heading, in radians.  0 is north (up),
     * and positive values rotate in the clockwise direction.  This is
     * the direction the robot will be facing initially, as well as after
     * every reset.
     */
    public double getInitialHeading() {
        return initialHeading;
    }
    
    /**
     * Reports this robot's x position.
     * 
     * <p>Note: this bean property is expressed as a double so that it will work
     * in the bean shell with an expression like "grod.x = 1.0" (the literal
     * 1.0 is a double, which requires a narrowing primitive conversion (such
     * type conversions are not done implicitly).
     */
    public double getX() {
        return position.x;
    }
    
    /**
     * Sets this robot's x position.  The y position will remain unchanged.
     * 
     * <p>Note: this bean property is expressed as a double so that it will work
     * in the bean shell with an expression like "grod.x = 1.0" (the literal
     * 1.0 is a double, which requires a narrowing primitive conversion (such
     * type conversions are not done implicitly).
     */
    public void setX(double x) {
        position.x = (float) x;
    }

    /**
     * Reports this robot's y position.
     * 
     * <p>Note: this bean property is expressed as a double so that it will work
     * in the bean shell with an expression like "grod.x = 1.0" (the literal
     * 1.0 is a double, which requires a narrowing primitive conversion (such
     * type conversions are not done implicitly).
     */
    public double getY() {
        return position.y;
    }
    
    /**
     * Sets this robot's y position.  The x position will remain unchanged.
     * 
     * <p>Note: this bean property is expressed as a double so that it will work
     * in the bean shell with an expression like "grod.x = 1.0" (the literal
     * 1.0 is a double, which requires a narrowing primitive conversion (such
     * type conversions are not done implicitly).
     */
    public void setY(double y) {
        position.y = (float) y;
    }


    /**
     *  Sets this robot's position.
     * 
     * <p>Note: the arguments are declared as doubles so that it will work
     * in the bean shell with an expression like "grod.setPosition(1.0, 1.0)" (the literal
     * 1.0 is a double, which requires a narrowing primitive conversion (such
     * type conversions are not done implicitly).
     */
    public void setPosition(double x, double y) {
        setPosition(new Point2D.Float((float) x, (float) y));
    }
    
	public RobotInput[] getInputs() {
	    return robotInputsGate.inputs;
	}
	
	public RobotSensorOutput[] getOutputs() {
		return outputs.values().toArray(new RobotSensorOutput[outputs.size()]);
	}

    public Gate getInputsGate() {
        return robotInputsGate;
    }

    public boolean isGoalReached() {
        return goalReached;
    }
    
    public void setGoalReached(boolean v) {
        goalReached = v;
    }

    /**
     * Resets this robot's position and heading to their initial values,
     * as well as resetting the circuit to its default state.
     */
    public void resetState() {
        debugf("Reset State for %s: start position=(%2.1f,%2.1f)\n",
                getId(), startPosition.getX(), startPosition.getY());
        setGoalReached(false);
        setPosition(startPosition);
        
        // This sets the robot's absolute heading.  It depends on the
        // internal implementation of getSpriteHeading().
        movingDirection = 0;
        prevHeading = initialHeading;
        
        circuit.resetState();
    }

    /**
     * Returns the circuit that controls this Robot's behaviour.
     */
    public Circuit getCircuit() {
        return circuit;
    }

    public float getStepSize() {
        return stepSize;
    }
    
    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
    }
}
