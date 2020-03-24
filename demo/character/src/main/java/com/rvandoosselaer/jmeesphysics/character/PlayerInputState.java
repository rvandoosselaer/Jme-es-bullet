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
package com.rvandoosselaer.jmeesphysics.character;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.rvandoosselaer.jmeesphysics.BulletSystem;
import com.rvandoosselaer.jmeesphysics.es.Mass;
import com.rvandoosselaer.jmeesphysics.es.PhysicalShape;
import com.rvandoosselaer.jmeesphysics.es.WarpPosition;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityContainer;
import com.simsilica.es.EntityData;
import com.simsilica.state.GameSystemsState;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerInputState extends BaseAppState {

    private final EntityData entityData;
    private final BulletSystem bulletSystem;

    private PlayerInputContainer drivers;

    @Override
    protected void initialize(Application app) {
        drivers = new PlayerInputContainer(entityData);
        drivers.start();
    }

    @Override
    protected void cleanup(Application app) {
        drivers.stop();
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        drivers.update();
    }

    private class PlayerInputContainer extends EntityContainer<PlayerInputDriver> {

        public PlayerInputContainer(EntityData ed) {
            super(ed, PlayerInput.class, Mass.class, WarpPosition.class, PhysicalShape.class);
        }

        @Override
        protected PlayerInputDriver addObject(Entity e) {
            PlayerInputDriver driver = new PlayerInputDriver(e, getState(GameSystemsState.class).get(BulletSystem.class).getPhysicsSpace());
            bulletSystem.setPhysicalEntityDriver(e.getId(), driver);
            updateObject(driver, e);
            return driver;
        }

        @Override
        protected void updateObject(PlayerInputDriver object, Entity e) {
            object.setPlayerInput(e.get(PlayerInput.class));
        }

        @Override
        protected void removeObject(PlayerInputDriver object, Entity e) {
            bulletSystem.setPhysicalEntityDriver(e.getId(), null);
        }
    }
}
