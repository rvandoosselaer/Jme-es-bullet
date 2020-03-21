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
package org.chimpstack.jme3.es.bullet.debug;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.lemur.GuiGlobals;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chimpstack.jme3.ApplicationGlobals;
import org.chimpstack.jme3.es.bullet.PhysicalShape;
import org.chimpstack.jme3.es.bullet.PhysicalShapeRegistry;
import org.chimpstack.jme3.util.SpatialUtils;

import java.util.Objects;

/**
 * An application state that shows debug meshes of all physical entities that are managed by the ES. Objects that are
 * directly added to the physics space will not be shown!
 * The {@link PhysicalEntityDebugPublisher} should be added to the {@link org.chimpstack.jme3.es.bullet.BulletSystem}
 * for this state to show anything.
 */
@Slf4j
@RequiredArgsConstructor
public class BulletSystemDebugState extends BaseAppState {

    private final EntityData entityData;
    private final PhysicalShapeRegistry shapeRegistry;

    private Material activeMaterial;
    private Material inActiveMaterial;
    private Material staticMaterial;
    private ColorRGBA activeColor = ColorRGBA.Green;
    private ColorRGBA inActiveColor = ColorRGBA.Blue;
    private ColorRGBA staticColor = ColorRGBA.White;
    private Node debugNode = new Node("BulletSystem - debug");
    private DebugObjects debugObjects;

    @Override
    protected void initialize(Application app) {
        debugObjects = new DebugObjects(entityData);
        debugObjects.start();
    }

    @Override
    protected void onEnable() {
        ApplicationGlobals.getInstance().getRootNode().attachChild(debugNode);
    }

    @Override
    public void update(float tpf) {
        debugObjects.update();
    }

    @Override
    protected void onDisable() {
        debugNode.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        debugObjects.stop();
    }

    /**
     * returns the material based on the status of the physical entity.
     */
    private Material getMaterial(int status) {
        if (status == PhysicalEntityDebug.ACTIVE) {
            if (activeMaterial == null) {
                activeMaterial = GuiGlobals.getInstance().createMaterial(activeColor, false).getMaterial();
                activeMaterial.getAdditionalRenderState().setWireframe(true);
            }
            return activeMaterial;
        } else if (status == PhysicalEntityDebug.INACTIVE) {
            if (inActiveMaterial == null) {
                inActiveMaterial = GuiGlobals.getInstance().createMaterial(inActiveColor, false).getMaterial();
                inActiveMaterial.getAdditionalRenderState().setWireframe(true);
            }
            return inActiveMaterial;
        } else if (status == PhysicalEntityDebug.STATIC) {
            if (staticMaterial == null) {
                staticMaterial = GuiGlobals.getInstance().createMaterial(staticColor, false).getMaterial();
                staticMaterial.getAdditionalRenderState().setWireframe(true);
            }
            return staticMaterial;
        }
        return null;
    }

    private class DebugObjects extends EntityContainer<Spatial> {

        public DebugObjects(EntityData ed) {
            super(ed, PhysicalShape.class, PhysicalEntityDebug.class);
        }

        @Override
        protected Spatial addObject(Entity e) {
            PhysicalShape physicalShape = e.get(PhysicalShape.class);
            PhysicalEntityDebug entityDebug = e.get(PhysicalEntityDebug.class);

            Spatial debugShape = DebugShapeFactory.getDebugShape(shapeRegistry.get(physicalShape));
            debugShape.setName("debug-shape-" + e.getId());
            debugShape.setLocalTranslation(entityDebug.getLocation());
            debugShape.setLocalRotation(entityDebug.getRotation());
            debugShape.setMaterial(getMaterial(entityDebug.getStatus()));

            log.trace("Adding {} to {}", debugShape, debugNode);
            debugNode.attachChild(debugShape);

            return debugShape;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            PhysicalEntityDebug entityDebug = e.get(PhysicalEntityDebug.class);

            object.setLocalTranslation(entityDebug.getLocation());
            object.setLocalRotation(entityDebug.getRotation());

            SpatialUtils.getFirstGeometry(object).ifPresent(g -> {
                Material material = getMaterial(entityDebug.getStatus());
                if (!Objects.equals(material, g.getMaterial())) {
                    g.setMaterial(material);
                }
            });
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            log.trace("Removing {} from {}", object, debugNode);
            object.removeFromParent();
        }

    }

}
