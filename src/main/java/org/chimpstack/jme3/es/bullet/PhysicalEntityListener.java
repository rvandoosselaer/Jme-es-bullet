/**
 * Copyright (c) 2019, Chimpstack
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.chimpstack.jme3.es.bullet;

import com.simsilica.sim.SimTime;

/**
 * A listener that hooks into the {@link BulletSystem} update loop and notifies about physical object changes.
 * The listeners are called multiple times each frame and should be efficient and few.
 */
public interface PhysicalEntityListener {

    /**
     * Called at the start of the physics frame, before the physics calculation.
     * @param time time information
     */
    public void startFrame(SimTime time);

    /**
     * Called when a physical entity is added to the physics space
     * @param physicalEntity the added physical entity
     */
    public void physicalEntityAdded(PhysicalEntity physicalEntity);

    /**
     * Called each frame for all attached physical entities to the physics space after the physics calculation.
     * @param physicalEntity the updated physical entity
     */
    public void physicalEntityUpdated(PhysicalEntity physicalEntity);

    /**
     * Called when a physical entity is removed from the physics space
     * @param physicalEntity the remove physical entity
     */
    public void physicalEntityRemoved(PhysicalEntity physicalEntity);

    /**
     * Called at the end of the physics frame
     * @param time time information
     */
    public void endFrame(SimTime time);
}
