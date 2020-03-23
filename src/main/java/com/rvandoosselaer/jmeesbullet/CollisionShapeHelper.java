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

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * A helper class to create {@link CollisionShape} objects.
 */
public class CollisionShapeHelper {

    public static CollisionShape createBoxShape(Vector3f extent) {
        return new BoxCollisionShape(extent);
    }

    public static CollisionShape createBoxShape(Spatial spatial) {
        return new BoxCollisionShape(((BoundingBox) spatial.getWorldBound()).getExtent(new Vector3f()));
    }

    public static CollisionShape createSphereShape(float radius) {
        return new SphereCollisionShape(radius);
    }

    public static CollisionShape createMeshShape(Spatial spatial) {
        return CollisionShapeFactory.createMeshShape(spatial);
    }

    public static CollisionShape createDynamicMeshShape(Spatial spatial) {
        return CollisionShapeFactory.createDynamicMeshShape(spatial);
    }

    public static CollisionShape createCapsuleShape(Spatial spatial) {
        Vector3f extent = ((BoundingBox) spatial.getWorldBound()).getExtent(new Vector3f());
        return new CapsuleCollisionShape(extent.z, (2 * extent.y) - (2 * extent.z));
    }

    /**
     * The created collisionshape is a capsule collision shape that is attached to a compound collision shape with an
     * offset to set the object center at the bottom of the capsule.
     *
     * @param radius         radius of the capsule
     * @param height         height of the capsule
     * @param centerAtBottom true if the center of the capsule should be at the bottom of the object
     * @return a capsule physical shape
     */
    public static CollisionShape createCapsuleShape(float radius, float height, boolean centerAtBottom) {
        CapsuleCollisionShape capsule = new CapsuleCollisionShape(radius, height - (2 * radius));
        if (centerAtBottom) {
            CompoundCollisionShape compoundShape = new CompoundCollisionShape();
            Vector3f offset = new Vector3f(0, height / 2.0f, 0);
            compoundShape.addChildShape(capsule, offset);
            return compoundShape;
        } else {
            return capsule;
        }
    }

}
