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
 * Created on Apr 12, 2006
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.KeyStroke;

import net.bluecow.robot.gate.Gate;
import net.bluecow.robot.resource.ResourceLoader;
import net.bluecow.robot.sound.SoundManager;
import net.bluecow.robot.sprite.Sprite;
import net.bluecow.robot.sprite.SpriteLoadException;
import net.bluecow.robot.sprite.SpriteManager;

/**
 * The GameConfig class holds configuration information for an instance of
 * the GrodBots game.  Although a GameConfig can be created from scratch
 * programmatically, every aspect of the configuration defaults to being
 * empty or unspecified.  The easiest way to create a configured GameConfig
 * object is by using the LevelStore class. It can read a configuration file
 * and return a fully-set-up GameConfig.
 * 
 * @see LevelStore
 * @author fuerth
 * @version $Id:$
 */
public class GameConfig {

    /**
     * GateConfig represents a type, or class, of gate that can be used in
     * circuits in this game.  Instances of this class are immutable, so they
     * are safe to share between objects without first making a copy.
     */
    public static class GateConfig {
        private String name;
        private KeyStroke accelerator;
        private Class<Gate> gateClass;
        
        /**
         * Creates a new gate configuration with the given properties.
         * 
         * @param name The gate's identifier in this config
         * @param accelerator The accelerator key for creating a gate of this type
         * @param clazz The class whose instances represent a gate of this type
         */
        public GateConfig(String name, KeyStroke accelerator, Class<Gate> clazz) {
            this.name = name;
            this.accelerator = accelerator;
            this.gateClass = clazz;
        }

        public KeyStroke getAccelerator() {
            return accelerator;
        }
        public Class<Gate> getGateClass() {
            return gateClass;
        }
        public String getName() {
            return name;
        }
    }
    
    public static class SquareConfig implements Square {
        private String name;
        private char mapChar;
        private boolean occupiable = true;
        private Sprite sprite;
        private Collection<SensorConfig> sensorTypes;
        
        public SquareConfig() {
            sensorTypes = new ArrayList<SensorConfig>();
        }
        
        public SquareConfig(String name, char mapChar, boolean occupiable,
                Sprite sprite, Collection<SensorConfig> sensorTypes) {
            this.name = name;
            this.mapChar = mapChar;
            this.occupiable = occupiable;
            this.sprite = sprite;
            this.sensorTypes = new ArrayList<SensorConfig>(sensorTypes);
        }

        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public char getMapChar() {
            return mapChar;
        }
        
        public void setMapChar(char mapChar) {
            this.mapChar = mapChar;
        }

        public boolean isOccupiable() {
            return occupiable;
        }
        
        public void setOccupiable(boolean occupiable) {
            this.occupiable = occupiable;
        }

        public Collection<SensorConfig> getSensorTypes() {
            return Collections.unmodifiableCollection(sensorTypes);
        }
        
        public void setSensorTypes(Collection<SensorConfig> sensorTypes) {
            this.sensorTypes = sensorTypes;
        }

        public boolean removeSensorType(SensorConfig sc) {
            return sensorTypes.remove(sc);
        }
        
        public Sprite getSprite() {
            return sprite;
        }
        
        public void setSprite(Sprite sprite) {
            this.sprite = sprite;
        }
    }
    
    public static class SensorConfig {
        private String id;
        
        public SensorConfig(String id) {
            if (id == null) throw new NullPointerException("Null sensor type not allowed");
            this.id = id;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
        
        @Override
        public boolean equals(Object other) {
            return id.equals(((SensorConfig) other).id);
        }
        
        @Override
        public String toString() {
            return id;
        }

    }

    /**
     * Provides the necessary support for maintaining a list of property
     * change listeners and firing property change events to them.
     */
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    /**
     * The sound manager for this game config. It uses the same resource
     * loader as this game config.
     */
    private final SoundManager sm;
    
    private Map<String, GateConfig> gateTypes = new HashMap<String, GateConfig>();
    private Map<Character, SquareConfig> squareTypes = new HashMap<Character, SquareConfig>();
    private Map<String, SensorConfig> sensorTypes = new LinkedHashMap<String, SensorConfig>();
    private List<LevelConfig> levels = new ArrayList<LevelConfig>();

    /**
     * The resource loader that is responsible for loading all the auxiliary resources
     * for this game config (images, sounds, the config file, and so on).
     */
    private final ResourceLoader resourceLoader;
    
    
    public GameConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        sm = new SoundManager(resourceLoader);
    }
    
    
    /**
     * Calculates the total score for this game by adding up the current scores
     * from each level.
     * 
     * <p>This could be improved to cache the results (or partial cumulative
     * results) if performance becomes a problem (due to having too many levels
     * in the same game).
     * 
     * @return The current total score.
     */
    public int getScore() {
        int score = 0;
        for (LevelConfig lc : levels) {
            score += lc.getEffectiveScore();
        }
        return score;
    }
    
    @SuppressWarnings("unchecked")
    public void addGate(String gateName, char accelKey, String gateClass) throws ClassNotFoundException {
        gateTypes.put(gateName,
                new GateConfig(gateName,
                        KeyStroke.getKeyStroke(accelKey),
                        (Class<Gate>) Class.forName(gateClass)));
    }

    public Set<String> getGateTypeNames() {
        return Collections.unmodifiableSet(gateTypes.keySet());
    }
    
    public Collection<GateConfig> getGateTypes() {
        return Collections.unmodifiableCollection(gateTypes.values());
    }
    
    public GateConfig getGate(String gateTypeName) {
        return gateTypes.get(gateTypeName);
    }

    /**
     * Adds a new square config to the game and fires a property change event
     * for the property "squareTypes".
     */
    public void addSquareType(String squareName, char squareChar,
            boolean occupiable, String graphicsFileName,
            Collection<String> sensorTypeIdList) throws SpriteLoadException {
        
        Set<SensorConfig> squareSensorTypes = new HashSet<SensorConfig>();
        for (String st : sensorTypeIdList) {
            squareSensorTypes.add(sensorTypes.get(st));
        }
        SquareConfig squareConfig = 
            new SquareConfig(squareName, squareChar,
                occupiable, SpriteManager.load(resourceLoader, graphicsFileName),
                squareSensorTypes);
        addSquareType(squareConfig);
    }

    /**
     * Adds a new square config to the game and fires a property change event
     * for the property "squareTypes".
     */
    public void addSquareType(SquareConfig squareConfig) {
        squareTypes.put(squareConfig.getMapChar(), squareConfig);
        pcs.firePropertyChange("squareTypes", null, null);
    }

    /**
     * Removes the given square type from this game config. Changes all squares
     * in maps that were using it to null.
     * 
     * @param squareConfig The square config to remove.  If such a square config
     * is not part of this game config, nothing will be affected.
     */
    public void removeSquareType(SquareConfig squareConfig) {
        if (squareTypes.remove(squareConfig.getMapChar()) != null) {
            for (LevelConfig level : levels) {
                // XXX this is too tightly coupled to level config.. need listener api for square type add/removes
                Square[][] map = level.getMap();
                for (int row = 0; row < map.length; row++) {
                    for (int col = 0; col < map[row].length; col++) {
                        if (map[row][col] != null &&
                                map[row][col].getMapChar() == squareConfig.getMapChar()) {
                            map[row][col] = null;
                        }
                    }
                }
            }
            pcs.firePropertyChange("squareTypes", null, null);
        }
    }

    public SquareConfig getSquare(char squareChar) {
        return squareTypes.get(squareChar);
    }
    
    public Collection<SquareConfig> getSquareTypes() {
        return squareTypes.values();
    }

    public void addLevel(LevelConfig level) {
        addLevel(levels.size(), level);
    }

    public void addLevel(int index, LevelConfig level) {
        levels.add(index, level);
        pcs.firePropertyChange("levels", null, level);
    }

    public void removeLevel(LevelConfig level) {
        levels.remove(level);
        pcs.firePropertyChange("levels", null, level);
    }

    public List<LevelConfig> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public List<SensorConfig> getSensorTypes() {
        List<SensorConfig> retval = new ArrayList<SensorConfig>(sensorTypes.size());
        for (Map.Entry<String, SensorConfig> entry : sensorTypes.entrySet()) {
            retval.add(entry.getValue());
        }
        return retval;
    }
    
    public void addSensorType(SensorConfig sc) {
        sensorTypes.put(sc.getId(), sc);
        pcs.firePropertyChange("sensorTypes", null, sensorTypes);
    }

    /**
     * Removes the given sensor type from this game.  If there were any square
     * types that activated this sensor type, they will be altered so they don't
     * refer to it any more.
     */
    public void removeSensorType(SensorConfig sensorConfig) {
        if (sensorTypes.remove(sensorConfig.getId()) != null) {
            for (SquareConfig square : squareTypes.values()) {
                square.removeSensorType(sensorConfig);
            }
            pcs.firePropertyChange("sensorTypes", null, null);
        }
    }

    public Object getSensor(String typeName) {
        return sensorTypes.get(typeName);
    }

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

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }
    
    public SoundManager getSoundManager() {
        return sm;
    }

    /**
     * Releases all resources held by this game config which need to be closed
     * explicitly. This may include files, network connections, and audio channels
     * among other things.
     */
    public void close() {
        sm.close();
    }
}
