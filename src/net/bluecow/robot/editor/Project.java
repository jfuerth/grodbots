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
 * Created on Sep 26, 2006
 *
 * This code belongs to SQL Power Group Inc.
 */
package net.bluecow.robot.editor;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import net.bluecow.robot.GameConfig;
import net.bluecow.robot.LevelConfig;
import net.bluecow.robot.LevelStore;
import net.bluecow.robot.Robot;
import net.bluecow.robot.LevelConfig.Switch;
import net.bluecow.robot.editor.event.LifecycleEvent;
import net.bluecow.robot.editor.event.LifecycleListener;
import net.bluecow.robot.resource.CompoundResourceManager;
import net.bluecow.robot.resource.DirectoryResourceManager;
import net.bluecow.robot.resource.JarResourceManager;
import net.bluecow.robot.resource.ListableResourceLoader;
import net.bluecow.robot.resource.PreListedResourceLoader;
import net.bluecow.robot.resource.PrefixResourceLoader;
import net.bluecow.robot.resource.ResourceManager;
import net.bluecow.robot.resource.ResourceUtils;
import net.bluecow.robot.resource.SystemResourceLoader;

/**
 * The Project class represents a project in the robot's level editor.
 * It includes the project's resource manager, which the editor uses
 * for loading, saving, and exploring the resource files available;
 * the project's game configuration, which assembles those resources
 * together into levels, squares, switches, robots, and so on; the
 * project file's location on the local filesystem; and finally a
 * set of prototypical objects that the editor uses as starting points
 * for new robots and swiches.
 * <p>
 * The Project also manages the lifecycle of an editing session, and
 * as such it is essential that the project instance is notified when
 * it is no longer needed (by calling the {@link #close()} method).
 * The initial use for project lifecycles was to let the resource
 * manager know when to clean up its temporary files, but the
 * lifecycle events are not inherently limited to just resource
 * manager cleanup.
 */
public class Project {

    /**
     * The prefix within the system resource loader under which all the
     * resources built into the game can be found. In order to make a listable
     * resource loader from this information, you must use the {@link PreListedResourceLoader}
     * together with a listing resource of {@link #RESOURCE_LIST_PATH}.
     */
    private static final String BUILTIN_RESOURCES_PREFIX = "builtin/";

    /**
     * The standard filename within a resource loader that lists its contents.
     * If a resource loader has this file inside its root directory, that
     * resource loader can be upgraded to a {@link ListableResourceLoader}.
     */
    private static final String RESOURCE_LIST_PATH = "resources.list";

    /**
     * The prefix within the system classpath that contains the resource
     * collection that should be copied into new projects. As of this writing,
     * the only resource is the ROBO-INF/default.map file which itself depends
     * on the built-in resources.
     */
    private static final String NEW_PROJECT_RESOURCES_PREFIX = "new_project/";

    private static final boolean debugOn = false;

    private static void debug(String msg) {
        if (debugOn) System.out.println(msg);
    }

    /**
     * The file or directory that this project was most recently loaded from or saved to.
     */
    private File fileLocation;
    
    /**
     * The game config associated with this project.
     */
    private GameConfig gameConfig;
    
    /**
     * The prototype robot for creating new ones.  One day, this will
     * be modifiable by the user and get saved with the project.
     */
    private Robot defaultRobot = new Robot(
            "new_robot", "Grod", null, new ArrayList<GameConfig.SensorConfig>(),
            new ArrayList<GameConfig.GateConfig>(), null,
            new Point2D.Float(0.5f, 0.5f), 0.1f, null, 1);
    
    private Switch defaultSwitch = new LevelConfig.Switch(
            new Point(1, 1), "new_switch", "", null, null);
    
    /**
     * All of the listeners interested in this project's lifecycle.
     */
    private final List<LifecycleListener> lifecycleListeners = new ArrayList<LifecycleListener>();
    
    /**
     * Private do-nothing constructor to prevent heathens from creating
     * projects on their own.  See {@link #createNewProject(File)} and
     * {@link #load(File)}.
     */
    private Project() {
        super();
    }

    /**
     * Creates a new project with a default empty level.  This operation creates
     * the project's initial JAR file containing the ROBO-INF subdirectory.
     * <p>
     * Important Note: It is essential that you call {@link #close()} on the
     * returned project instance when you are done with it.. even if the next
     * step will be to terminate the JVM.
     * 
     * @param file The file that will hold this project.  It must not exist yet.
     * @return The new project.
     * @throws IOException If the file already exists, or it can't be created.
     */
    public static Project createNewProject(File file) throws IOException {
        debug("Trying to create new project at \""+file.getAbsolutePath()+"\"");
        if (file.exists()) {
            throw new IOException(
                    "File "+file.getAbsolutePath()+" already exists.");
        }

        // now copy the default project to the user's selected location
        // then load it like a regular project
        // This code assumes the resource is a JAR file.
        ListableResourceLoader newProjectResources =
            new PreListedResourceLoader(
                    new PrefixResourceLoader(new SystemResourceLoader(), NEW_PROJECT_RESOURCES_PREFIX),
                    RESOURCE_LIST_PATH);
        ResourceUtils.createResourceJar(newProjectResources, file);
        
        Project proj = load(file);
        
        return proj;
    }
    
    /**
     * Creates a new Project instance by loading it from a directory structure
     * which contains files laid out in a special way. The directory structure
     * can be an actual directory with files and subdirectories, or it can be
     * a JAR file which contains the same structure.
     * <p>
     * Important Note: It is essential that you call {@link #close()} on the
     * returned project instance when you are done with it.. even if the next
     * step will be to terminate the JVM.
     * 
     * @param jarOrDir
     *            The directory or JAR file to read the project description and
     *            resources from.
     * @return A new project configured from the given location
     * @throws IOException
     */
    public static Project load(File jarOrDir) throws IOException {
        
        ResourceManager projResources;
        if (jarOrDir.isDirectory()) {
            projResources = new DirectoryResourceManager(jarOrDir);
        } else {
            projResources = new JarResourceManager(jarOrDir);
        }
        PreListedResourceLoader builtinResources = new PreListedResourceLoader(
                new PrefixResourceLoader(new SystemResourceLoader(), BUILTIN_RESOURCES_PREFIX),
                RESOURCE_LIST_PATH);
        
        final ResourceManager compoundResources =
            new CompoundResourceManager(projResources, builtinResources);
        
        Project proj = new Project();
        proj.gameConfig = LevelStore.loadLevels(compoundResources);
        proj.fileLocation = jarOrDir;
        ResourceUtils.initResourceURLHandler(compoundResources);
        
        proj.addLifecycleListener(new LifecycleListener() {
            public void lifecycleEnding(LifecycleEvent evt) {
                try {
                    compoundResources.close();
                } catch (IOException ex) {
                    System.err.println("Couldn't close project resource manager!");
                    ex.printStackTrace();
                }
            }
        });
        return proj;
    }

    /**
     * Returns this project's game configuration.
     */
    public GameConfig getGameConfig() {
        return gameConfig;
    }
    
    /**
     * Returns the resource manager associated with this project.
     */
    public ResourceManager getResourceManager() {
        // note, we created the game config with a resource manager (not just a loader),
        // so this cast will work as long as no one changes its resource loader. 
        return (ResourceManager) gameConfig.getResourceLoader();
    }
    

    /**
     * Saves this project by bundling all its resources into a single JAR file.
     * Also updates the {@link #fileLocation} property if applicable.
     * 
     * FIXME there should be a variant that preens the resource set a little bit: remove
     * old map backups, remove example solutions (when we get that working),
     * and whatever else isn't fit for mass distribution.  A good way to
     * implement this would be a file filter that knows which resources
     * to exclude.
     * 
     * @param location the file to save into (doesn't have to exist yet). If
     * this argument is null, the project will be saved to {@link #fileLocation}.
     * If that is also null, a NullPointerException will be thrown.
     * 
     * @throws IOException If there are any problems during the save operation
     */
    public void saveLevelPack(File location) throws IOException {
        saveMapFile();
        if (location == null && fileLocation != null && fileLocation.isDirectory()) {
            debug("Not creating level pack because save location is a directory (the map file has been saved)");
            return;
        }
        if (location == null) {
            location = fileLocation;
        }
        if (location == null) {
            throw new NullPointerException("Don't know where to save the project (both locations are null)");
        }
        ResourceManager saveThisOne = getResourceManager();
        if (saveThisOne instanceof CompoundResourceManager) {
            saveThisOne = ((CompoundResourceManager) saveThisOne).getPrimary();
        } else {
            throw new IllegalStateException("The project's resource manager is not what it used to be!");
        }
        ResourceUtils.createResourceJar(saveThisOne, location);
        fileLocation = location;
    }
    
    /**
     * Saves the current game configuration to this project's resource manager.
     * To export a single JAR file that contains the whole project, use
     * {@link #saveLevelPack(File)}.
     * 
     * @throws IOException
     *             if there are any problems saving the resources
     */
    private void saveMapFile() throws IOException {
        String encoding = "utf-8";
        OutputStream out = getResourceManager().openForWrite(LevelStore.DEFAULT_MAP_RESOURCE_PATH, true);
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, encoding));
        LevelStore.save(writer, getGameConfig(), encoding);
        writer.flush();
        writer.close();
    }

    /**
     * Creates a new Robot which has properties the same as the default
     * robot.
     * 
     * @return the new robot instance.
     */
    public Robot createRobot(LevelConfig targetLevel) {
        Robot r = new Robot(defaultRobot, targetLevel);
        return r;
    }
    
    /**
     * Creates a new Switch which is identical to this project's prototypical
     * switch instance.
     */
    public Switch createSwitch() {
        return new LevelConfig.Switch(defaultSwitch);
    }

    /**
     * See {@link #fileLocation}.
     */
    public File getFileLocation() {
        return fileLocation;
    }

    /**
     * See {@link #fileLocation}.
     */
    public void setFileLocation(File fileLocation) {
        this.fileLocation = fileLocation;
    }
    
    /**
     * Notifies this project that it is no longer in use. Causes a
     * <tt>projectClosing</tt> event. It is essential that this method is
     * called for every project instance, because the event it fires can lead to
     * temp file cleanup, closing the project's GameConfig, and other important
     * housekeeping tasks.
     */
    public void close() {
        fireProjectClosing();
        gameConfig.close();
    }

    /**
     * Adds the given lifecycle listener to this project.
     * 
     * @param l The listener to add. Must not be null.
     */
    public void addLifecycleListener(LifecycleListener l) {
        if (l == null) throw new NullPointerException("Null lifecycle listener");
        lifecycleListeners.add(l);
    }

    /**
     * Removes the given lifecycle listener from this project.
     * If the given listener was not attached to this project,
     * this method has no effect.
     */
    public void removeLifecycleListener(LifecycleListener l) {
        lifecycleListeners.remove(l);
    }
    
    /**
     * Fires a projectClosing event.  This is normally called via the
     * {@link #close()} method.
     */
    private void fireProjectClosing() {
        LifecycleEvent evt = new LifecycleEvent(this);
        for (int i = lifecycleListeners.size() - 1; i >= 0; i--) {
            lifecycleListeners.get(i).lifecycleEnding(evt);
        }
    }
}
