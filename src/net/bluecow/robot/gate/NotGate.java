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
 * Created on Aug 19, 2005
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot.gate;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * The NotGate class represents a single input logical NOT gate.
 *
 * @author fuerth
 * @version $Id$
 */
public class NotGate extends AbstractGate {

    /**
     * Creates a 1-input 1-output NOT gate.
     */
    public NotGate() {
        super(null);
        inputs = new DefaultInput[1];
        inputs[0] = new DefaultInput();
    }

    /**
     * Returns "NOT".
     */
    public String getType() {
        return "NOT";
    }
    
    public NotGate createDisconnectedCopy() {
        final NotGate newGate = new NotGate();
        newGate.copyFrom(this);
        return newGate;
    }

    /**
     * Calculates the logical complement of the current input state.
     */
    public void evaluateInput() {
        nextOutputState = ! inputs[0].getState();
    }

    @Override
    public void drawBody(Graphics2D g2) {
        Rectangle r = getBounds();
        int backX = getInputStickLength();
        g2.drawLine(backX, 0, backX, r.height);
        g2.drawLine(backX, 0, r.width - getOutputStickLength(), r.height/2);
        g2.drawLine(backX, r.height, r.width - getOutputStickLength(), r.height/2);
    }
    
    @Override
    protected boolean isInputInverted() {
        return false;
    }

    @Override
    protected boolean isOutputInverted() {
        return true;
    }

}
