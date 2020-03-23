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
package com.rvandoosselaer.jmeesbullet.cubes;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Spatial;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VisualState extends BaseAppState {

    private final EntityData entityData;
    private final ModelRegistry modelRegistry;

    private ModelContainer models;

    @Override
    protected void initialize(Application app) {
        models = new ModelContainer(entityData);
        models.start();
    }

    @Override
    protected void cleanup(Application app) {
        models.stop();
    }

    @Override
    public void update(float tpf) {
        models.update();
    }

    private class ModelContainer extends EntityContainer<Spatial> {

        public ModelContainer(EntityData ed) {
            super(ed, Model.class, Position.class);
        }

        @Override
        protected Spatial addObject(Entity e) {
            Model model = e.get(Model.class);
            Position position = e.get(Position.class);

            Spatial spatial = modelRegistry.get(model).clone(false);
            spatial.setLocalTranslation(position.getLocation());
            spatial.setLocalRotation(position.getRotation());
            ((SimpleApplication) getApplication()).getRootNode().attachChild(spatial);

            return spatial;
        }

        @Override
        protected void updateObject(Spatial object, Entity e) {
            Position position = e.get(Position.class);

            object.setLocalTranslation(position.getLocation());
            object.setLocalRotation(position.getRotation());
        }

        @Override
        protected void removeObject(Spatial object, Entity e) {
            object.removeFromParent();
        }

    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

}
