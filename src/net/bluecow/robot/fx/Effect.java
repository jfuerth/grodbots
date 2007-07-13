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
 * Created on Feb 18, 2007
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot.fx;

import java.awt.Graphics2D;
import java.awt.Point;

import net.bluecow.robot.sprite.Sprite;

/**
 * Effect is a sprite which keeps track of its own position, and has a limited
 * life span. It is actually closer to the traditional definition of a sprite
 * than the Sprite interface represents.
 * 
 * @author fuerth
 * @version $Id$
 */
public interface Effect extends Sprite {
    
    /**
     * The position where the top left-hand corner of this effect will
     * paint.
     */
    public Point getPosition();
    
    /**
     * This effect's X position.
     */
    public int getX();
    
    /**
     * This effect's Y position.
     */
    public int getY();
    
    /**
     * Paints this effect at its current position.
     */
    public void paint(Graphics2D g2);
    
    /**
     * Returns true if this effect has completed, and should no longer be
     * painted.
     */
    public boolean isFinished();
    
}
