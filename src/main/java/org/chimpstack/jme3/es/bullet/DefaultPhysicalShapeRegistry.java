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
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link PhysicalShapeRegistry} implementation that uses an internal thread-safe index to look up collision shapes.
 * The {@link #loadCollisionShape(PhysicalShape)} method can be overwritten to allow for custom load behaviour for
 * collision shapes that aren't found in the registry.
 */
@Slf4j
public class DefaultPhysicalShapeRegistry implements PhysicalShapeRegistry {

    private final Map<String, CollisionShape> registry = new ConcurrentHashMap<>();

    @Override
    public CollisionShape register(PhysicalShape physicalShape, CollisionShape collisionShape) {
        registry.put(physicalShape.getShapeId(), collisionShape);
        log.trace("Registering {} -> {}", physicalShape, collisionShape);
        return collisionShape;
    }

    @Override
    public CollisionShape get(PhysicalShape physicalShape) {
        CollisionShape collisionShape = registry.get(physicalShape.getShapeId());
        if (collisionShape != null) {
            log.trace("Retrieving {} -> {}", physicalShape, collisionShape);
            return collisionShape;
        }

        // collision shape isn't found in the registry. Use the custom loadCollisionShape() method.
        collisionShape = loadCollisionShape(physicalShape);
        if (collisionShape == null) {
            throw new IllegalArgumentException("No collision shape could be retrieved for " + physicalShape);
        }

        return register(physicalShape, collisionShape);
    }

    protected CollisionShape loadCollisionShape(PhysicalShape physicalShape) {
        return null;
    }

}
