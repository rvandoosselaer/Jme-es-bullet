/**
 * Copyright (c) 2020, rvandoosselaer
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
package com.rvandoosselaer.jmeesbullet;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import com.rvandoosselaer.jmeesbullet.es.Impulse;
import com.rvandoosselaer.jmeesbullet.es.Mass;
import com.rvandoosselaer.jmeesbullet.es.PhysicalShape;
import com.rvandoosselaer.jmeesbullet.es.WarpPosition;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.sim.AbstractGameSystem;
import com.simsilica.sim.SimTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A GameSystem implementation that sets up a Bullet PhysicsSpace and manages physical entities in that space.
 * Physical entities that have a {@link WarpPosition}, {@link Mass} and {@link PhysicalShape} will be picked up and
 * added/updated/removed from the physics space.
 * <p>
 * Other systems can register {@link PhysicalEntityListener} to be notified about changes of the entities.
 * A {@link PhysicalEntityDriver} can be registered on a physical entity using
 * {@link #setPhysicalEntityDriver(EntityId, PhysicalEntityDriver)}. Drivers can be used to steer physical entities.
 */
@Slf4j
public class BulletSystem extends AbstractGameSystem {

    @Getter
    @Setter
    private EntityData entityData;
    @Getter
    private PhysicsSpace physicsSpace;
    @Getter
    @Setter
    private PhysicsSpace.BroadphaseType broadphaseType = PhysicsSpace.BroadphaseType.DBVT;
    @Getter
    @Setter
    private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    @Getter
    @Setter
    private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    @Getter
    @Setter
    private float speed = 1.0f;
    private boolean calculateTicks = true;
    private float timeCounter;
    private int frameCounter;

    // a list of physical entity listeners
    private SafeArrayList<PhysicalEntityListener> physicalEntityListeners = new SafeArrayList<>(PhysicalEntityListener.class);
    // the container of all the rigidbodies
    private RigidBodyContainer rigidBodyContainer;
    // a queue for pending PhysicalEntityDriver setup
    private Queue<PhysicalEntityDriverSetup> pendingDriverSetup = new ConcurrentLinkedQueue<>();
    // the registry of collision shapes
    @Getter
    @Setter
    private PhysicalShapeRegistry shapeRegistry;
    // the entity set of all impulses
    private EntitySet impulses;

    public BulletSystem() {
    }

    public BulletSystem(EntityData entityData) {
        this.entityData = entityData;
    }

    public BulletSystem(EntityData entityData, PhysicalShapeRegistry shapeRegistry) {
        this.entityData = entityData;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    protected void initialize() {
        if (entityData == null) {
            entityData = getSystem(EntityData.class);
            if (entityData == null) {
                throw new IllegalStateException("EntityData is not set when initializing BulletSystem!");
            }
        }

        if (shapeRegistry == null) {
            shapeRegistry = getSystem(PhysicalShapeRegistry.class);
            if (shapeRegistry == null) {
                throw new IllegalStateException("PhysicalShapeRegistry is not set when initializing BulletSystem!");
            }
        }

        physicsSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
        rigidBodyContainer = new RigidBodyContainer(entityData);
    }

    @Override
    public void start() {
        rigidBodyContainer.start();
        impulses = entityData.getEntities(PhysicalShape.class, Mass.class, Impulse.class);
    }

    @Override
    public void update(SimTime time) {
        // call the start of the physics tick
        startFrame(time);

        // update the entity container
        rigidBodyContainer.update();

        // run pending objects setup
        PhysicalEntityDriverSetup setup = pendingDriverSetup.poll();
        if (setup != null) {
            boolean result = setup.execute();
            if (!result) {
                // setup failed, add it back to the queue.
                pendingDriverSetup.offer(setup);
            }
        }

        // apply impulses
        impulses.applyChanges();
        if (!impulses.isEmpty()) {
            // we don't care if the set is changed, just iterate over all items. The applyImpulses() method will clear
            // the current impulse if the body exists for that entity
            applyImpulses(impulses);
        }

        // calculate the speed of the physics simulation
        float t = (float) time.getTpf() * speed;
        if (t != 0) {

            // update the drivers of the physical entities
            for (PhysicalEntity entity : rigidBodyContainer.getArray()) {
                if (entity.getPhysicalEntityDriver() != null) {
                    entity.getPhysicalEntityDriver().update(t);
                }
            }

            // update the physics space and distribute collision events
            // read: https://hub.jmonkeyengine.org/t/sim-eth-es-troubleshootings/41249/45?u=remy_vd
            physicsSpace.update(t, 0);
            physicsSpace.distributeEvents();

            // notify the listeners for all of the attached entities after the physics calculation
            for (PhysicalEntity entity : rigidBodyContainer.getArray()) {
                physicalObjectUpdated(entity);
            }

        }

        // call the end of the physics tick
        endFrame(time);
    }

    @Override
    public void stop() {
        impulses.release();
        rigidBodyContainer.stop();
    }

    @Override
    protected void terminate() {
        physicsSpace.destroy();
    }

    public void setEntityData(EntityData entityData) {
        if (isInitialized()) {
            throw new IllegalStateException("BulletSystem is already initialized!");
        }

        this.entityData = entityData;
    }

    public void addPhysicalEntityListener(PhysicalEntityListener physicalEntityListener) {
        physicalEntityListeners.add(physicalEntityListener);
    }

    public void removePhysicalEntityListener(PhysicalEntityListener physicalEntityListener) {
        physicalEntityListeners.remove(physicalEntityListener);
    }

    public void setPhysicalEntityDriver(EntityId entityId, PhysicalEntityDriver driver) {
        // add to the setup queue
        pendingDriverSetup.offer(new PhysicalEntityDriverSetup(entityId, driver));
    }

    private void applyImpulses(Set<Entity> impulses) {
        for (Entity e : impulses) {
            RigidBodyEntity body = rigidBodyContainer.getObject(e.getId());
            if (body == null) {
                // skip over missing bodies, they may not have been created
                log.warn("No body found for {}", e.getId());
                continue;
            }

            // apply the impulse
            Impulse impulse = e.get(Impulse.class);
            if (impulse.getLinearVelocity() != null) {
                body.getPhysicalObject().setLinearVelocity(impulse.getLinearVelocity());
            }
            if (impulse.getAngularVelocity() != null) {
                body.getPhysicalObject().setAngularVelocity(impulse.getAngularVelocity());
            }

            // remove the Impulse component
            entityData.removeComponent(e.getId(), Impulse.class);
        }
    }

    private void startFrame(SimTime time) {
        for (PhysicalEntityListener listener : physicalEntityListeners.getArray()) {
            listener.startFrame(time);
        }
    }

    private void physicalObjectAdded(PhysicalEntity physicalEntity) {
        for (PhysicalEntityListener listener : physicalEntityListeners.getArray()) {
            listener.physicalEntityAdded(physicalEntity);
        }
    }

    private void physicalObjectUpdated(PhysicalEntity physicalEntity) {
        for (PhysicalEntityListener listener : physicalEntityListeners.getArray()) {
            listener.physicalEntityUpdated(physicalEntity);
        }
    }

    private void physicalObjectRemoved(PhysicalEntity physicalEntity) {
        for (PhysicalEntityListener listener : physicalEntityListeners.getArray()) {
            listener.physicalEntityRemoved(physicalEntity);
        }
    }

    private void endFrame(SimTime time) {
        for (PhysicalEntityListener listener : physicalEntityListeners.getArray()) {
            listener.endFrame(time);
        }
    }

    // an entity container that handles physical entities
    private class RigidBodyContainer extends EntityContainer<RigidBodyEntity> {

        public RigidBodyContainer(EntityData ed) {
            super(ed, PhysicalShape.class, Mass.class, WarpPosition.class);
        }

        @Override
        protected RigidBodyEntity[] getArray() {
            return super.getArray();
        }

        @Override
        protected RigidBodyEntity addObject(Entity e) {
            Mass mass = e.get(Mass.class);
            PhysicalShape shape = e.get(PhysicalShape.class);
            WarpPosition position = e.get(WarpPosition.class);

            RigidBodyEntity result = new RigidBodyEntity(e.getId(), shapeRegistry.get(shape), mass);

            result.setPhysicsLocation(position.getLocation());
            result.setPhysicsRotation(position.getRotation());

            log.trace("Adding {} to {}", result, physicsSpace);
            physicsSpace.addCollisionObject(result);
            // call the listener that an entity is added to the physics space
            physicalObjectAdded(result);

            return result;
        }

        @Override
        protected void updateObject(RigidBodyEntity object, Entity e) {
            // we only update the position
            WarpPosition position = e.get(WarpPosition.class);

            log.trace("Moving {} to {}", object, position);
            object.setPhysicsLocation(position.getLocation());
            object.setPhysicsRotation(position.getRotation());

            // call the listener that an entity is updated
            physicalObjectUpdated(object);
        }

        @Override
        protected void removeObject(RigidBodyEntity object, Entity e) {
            log.trace("Removing {} from {}", object, physicsSpace);
            physicsSpace.removeCollisionObject(object);
            // make sure to clean up the driver if one was attached
            object.setPhysicalEntityDriver(null);
            // call the listener that an entity is remove from the physics space
            physicalObjectRemoved(object);
        }
    }

    // helper class to setup a driver on an entity, when the setup fails more then 99 times, it's aborted.
    @RequiredArgsConstructor
    private class PhysicalEntityDriverSetup {
        private final EntityId entityId;
        private final PhysicalEntityDriver driver;
        private int tries;

        public boolean execute() {
            RigidBodyEntity rigidBodyEntity = rigidBodyContainer.getObject(entityId);
            if (rigidBodyEntity != null) {
                log.trace("Added {} to {} after {} tries", driver, entityId, tries);
                rigidBodyEntity.setPhysicalEntityDriver(driver);
                return true;
            }
            tries++;

            if (tries > 99) {
                log.error("Tried {} times to setup {} on {}. Aborting!", tries, driver, entityId);
                return true;
            }
            return false;
        }
    }

}



