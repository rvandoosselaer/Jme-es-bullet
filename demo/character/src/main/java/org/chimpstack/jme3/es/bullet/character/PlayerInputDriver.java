/**
 * Copyright (c) 2019, Chimpstack
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
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
package org.chimpstack.jme3.es.bullet.character;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.state.GameSystemsState;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.chimpstack.jme3.ApplicationGlobals;
import org.chimpstack.jme3.es.bullet.BulletSystem;
import org.chimpstack.jme3.es.bullet.PhysicalEntity;
import org.chimpstack.jme3.es.bullet.PhysicalEntityDriver;
import org.chimpstack.jme3.es.bullet.RigidBodyEntity;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PlayerInputDriver implements PhysicalEntityDriver {

    private final Entity entity;
    @Setter
    private PlayerInput playerInput;
    private RigidBodyEntity rigidBodyEntity;
    private Vector3f vTemp = new Vector3f();
    private float walkSpeed = 3;
    private boolean jumping;
    private PhysicsSpace physicsSpace;

    @Override
    public void initialize(PhysicalEntity entity) {
        log.trace("Initialize - {}", entity);
        rigidBodyEntity = (RigidBodyEntity) entity.getPhysicalObject();

        // retrieve the physics space, we need this to perform raytests to check if we are on the ground
        GameSystemsState systems = ApplicationGlobals.getInstance().getApplication().getStateManager().getState(GameSystemsState.class);
        physicsSpace = systems.get(BulletSystem.class).getPhysicsSpace();
    }

    @Override
    public void update(float tpf) {
        rigidBodyEntity.getAngularVelocity(vTemp);

        if (vTemp.x != 0 || vTemp.z != 0) {
            vTemp.x = 0;
            vTemp.y *= 0.95f;
            vTemp.z = 0;
            rigidBodyEntity.setAngularVelocity(vTemp);
        }

        Vector3f currentVelocity = rigidBodyEntity.getLinearVelocity();
        entity.set(new Velocity(currentVelocity.length()));

        if (playerInput != null) {
            rigidBodyEntity.setPhysicsRotation(playerInput.getDirection());

            Vector3f desiredVelocity = playerInput.getMovement().normalize().multLocal(walkSpeed);

            Vector3f force = new Vector3f(desiredVelocity).subtractLocal(currentVelocity);
            force.y = 0;
            force.multLocal(1000f);
            rigidBodyEntity.applyCentralForce(force);

            if (playerInput.isJump()) {
                if (isOnGround() && !jumping) {
                    Vector3f jumpForce = currentVelocity.addLocal(0, 6f, 0);
                    rigidBodyEntity.setLinearVelocity(jumpForce);
                    jumping = true;
                }
            } else {
                jumping = false;
            }

            if (desiredVelocity.length() > FastMath.ZERO_TOLERANCE) {
                rigidBodyEntity.setFriction(0.2f);
            } else {
                rigidBodyEntity.setFriction(1);
            }
        }
    }

    @Override
    public void cleanup(PhysicalEntity entity) {
        log.trace("Cleanup - {}", entity);
    }

    /**
     * Checks if the character of the player is on the ground by using a ray test. A ray is send out from the feet of
     * the character, directly down. When the collision data only contains the own rigidbody we are airborne.
     * start point: vec3(location) + (0, 0.3, 0)
     * end point: vec3(location) - (0, 0.2, 0)
     * @return
     */
    private boolean isOnGround() {
        Vector3f top = rigidBodyEntity.getLocation().addLocal(0, 0.3f, 0);
        Vector3f end = top.add(0, -0.5f, 0);
        List<PhysicsRayTestResult> results = physicsSpace.rayTest(top, end);
        log.trace("Ray: {} -> {} hits: {}", top, end, results.size());
        for (PhysicsRayTestResult result : results) {
            if (!result.getCollisionObject().equals(rigidBodyEntity)) {
                return true;
            }
        }
        return false;
    }

}
