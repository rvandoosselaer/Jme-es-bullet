/**
 * Copyright (c) 2020, rvandoosselaer
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
package com.rvandoosselaer.jmeesbullet.debug;

import com.rvandoosselaer.jmeesbullet.PhysicalEntity;
import com.rvandoosselaer.jmeesbullet.PhysicalEntityListener;
import com.rvandoosselaer.jmeesbullet.RigidBodyEntity;
import com.simsilica.es.EntityData;
import com.simsilica.sim.SimTime;
import lombok.RequiredArgsConstructor;

/**
 * A {@link PhysicalEntityListener} implementation that publishes, updates and removes {@link PhysicalEntityDebug}
 * components based on the location, rotation and status of the {@link PhysicalEntity} in the physics space.
 */
@RequiredArgsConstructor
public class PhysicalEntityDebugPublisher implements PhysicalEntityListener {

    private final EntityData entityData;

    @Override
    public void startFrame(SimTime time) {
    }

    @Override
    public void physicalEntityAdded(PhysicalEntity physicalEntity) {
        entityData.setComponent(physicalEntity.getEntityId(),
                new PhysicalEntityDebug(getStatus(physicalEntity), physicalEntity.getLocation(), physicalEntity.getRotation()));
    }

    @Override
    public void physicalEntityUpdated(PhysicalEntity physicalEntity) {
        entityData.setComponent(physicalEntity.getEntityId(),
                new PhysicalEntityDebug(getStatus(physicalEntity), physicalEntity.getLocation(), physicalEntity.getRotation()));
    }

    @Override
    public void physicalEntityRemoved(PhysicalEntity physicalEntity) {
        entityData.removeComponent(physicalEntity.getEntityId(), PhysicalEntityDebug.class);
    }

    @Override
    public void endFrame(SimTime time) {
    }

    private static int getStatus(PhysicalEntity physicalEntity) {
        if (physicalEntity instanceof RigidBodyEntity) {
            RigidBodyEntity rigidBodyEntity = (RigidBodyEntity) physicalEntity;
            return rigidBodyEntity.getMass() == 0 ? PhysicalEntityDebug.STATIC : rigidBodyEntity.isActive() ?
                    PhysicalEntityDebug.ACTIVE : PhysicalEntityDebug.INACTIVE;
        }
        return -1;
    }

}
