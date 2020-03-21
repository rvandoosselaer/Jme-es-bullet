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

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import lombok.ToString;

/**
 * A bullet rigidbody object directly linked to an entity.
 */
@ToString(of = "entityId")
public class RigidBodyEntity extends PhysicsRigidBody implements PhysicalEntity<PhysicsRigidBody> {

    private final EntityId entityId;
    private PhysicalEntityDriver driver;

    public RigidBodyEntity(EntityId entityId, CollisionShape shape, Mass mass) {
        super(shape, mass.getMass());
        this.entityId = entityId;
    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public PhysicsRigidBody getPhysicalObject() {
        return this;
    }

    @Override
    public Vector3f getLocation() {
        return getPhysicsLocation();
    }

    @Override
    public Quaternion getRotation() {
        return getPhysicsRotation();
    }

    @Override
    public PhysicalEntityDriver getPhysicalEntityDriver() {
        return driver;
    }

    /**
     * Set a driver to control the physical entity or null to remove a previous driver.
     * The appropriate {@link PhysicalEntityDriver} lifecycle methods will be called.
     * @param driver the driver to control the physical entity or null
     */
    public void setPhysicalEntityDriver(PhysicalEntityDriver driver) {
        if (this.driver != null) {
            this.driver.cleanup(this);
        }
        this.driver = driver;
        if (this.driver != null) {
            this.driver.initialize(this);
        }
    }

}
