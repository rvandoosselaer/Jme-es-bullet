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
package org.chimpstack.jme3.es.bullet;

import com.jme3.app.StatsAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.input.Button;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.state.GameSystemsState;
import org.chimpstack.jme3.ApplicationGlobals;
import org.chimpstack.jme3.JmeLauncher;
import org.chimpstack.jme3.es.bullet.debug.BulletSystemDebugState;
import org.chimpstack.jme3.es.bullet.debug.PhysicalEntityDebugPublisher;
import org.chimpstack.jme3.gui.GuiHelper;
import org.chimpstack.jme3.input.ThirdPersonCamera;
import org.chimpstack.jme3.util.GeometryUtils;

import java.util.Set;
import java.util.stream.IntStream;

public class Main extends JmeLauncher {

    private final FunctionId FUNC_SHOOT_BALL = new FunctionId("shoot-ball");
    private final FunctionId FUNC_SHOOT_CUBE = new FunctionId("shoot-cube");

    private GameSystemsState systems;
    private EntityData entityData;
    private BulletSystem bulletSystem;
    private PhysicalShapeRegistry shapeRegistry;
    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;
    private ModelRegistry modelRegistry;

    private Label physicsTicks;

    public static void main(String[] args) {
        new Main().start();
    }

    public Main() {
        super(new StatsAppState(),
                new GameSystemsState(true),
                new ThirdPersonCamera()
                        .setDragToRotate(true)
                        .setPitch(30 * FastMath.DEG_TO_RAD)
                        .setYaw(FastMath.QUARTER_PI)
                        .setZoomSpeed(20f)
                        .setDistance(20f)
                        .setMinDistance(5f)
                        .setMaxDistance(40f));
    }

    @Override
    public void init() {
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        GuiGlobals.getInstance().getInputMapper().map(FUNC_SHOOT_CUBE, Button.MOUSE_BUTTON1);
        GuiGlobals.getInstance().getInputMapper().map(FUNC_SHOOT_BALL, Button.MOUSE_BUTTON2);
        GuiGlobals.getInstance().getInputMapper().addDelegate(FUNC_SHOOT_CUBE, this, "shootCube");
        GuiGlobals.getInstance().getInputMapper().addDelegate(FUNC_SHOOT_BALL, this, "shootBall");

        setupGameSystems();

        setupLights();

        setupPostProcessing();

        physicsTicks = new Label(String.format("%d ticks per second", -1));
        physicsTicks.setLocalTranslation(GuiHelper.getWidth() - physicsTicks.getPreferredSize().x, GuiHelper.getHeight(), 1);
        ApplicationGlobals.getInstance().getGuiNode().attachChild(physicsTicks);
    }

    //@SneakyThrows(SQLException.class)
    protected void setupGameSystems() {
        systems = getStateManager().getState(GameSystemsState.class);

        // register global objects and systems
        //entityData = systems.register(EntityData.class, new SqlEntityData("~/Projects/Chimpstack/jme3-es-bullet/demo/data/", 1000));
        entityData = systems.register(EntityData.class, new DefaultEntityData());
        shapeRegistry = systems.register(PhysicalShapeRegistry.class, new DefaultPhysicalShapeRegistry());
        modelRegistry = systems.register(ModelRegistry.class, new DefaultModelRegistry());
        bulletSystem = systems.register(BulletSystem.class, new BulletSystem());
        getStateManager().attach(new BulletSystemDebugState(entityData, shapeRegistry));
        getStateManager().attach(new VisualState(entityData, modelRegistry));

        // register some physical shapes
        shapeRegistry.register(new PhysicalShape("floor"), new MeshCollisionShape(new Quad(32f, 32f)));
        shapeRegistry.register(new PhysicalShape("static-box"), new BoxCollisionShape(new Vector3f(1.0f, 1.0f, 1.0f)));
        shapeRegistry.register(new PhysicalShape("cube"), new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.5f)));
        shapeRegistry.register(new PhysicalShape("ball"), new SphereCollisionShape(0.5f));
        // register some models
        Geometry staticBox = GeometryUtils.createGeometry(new Box(1.0f, 1.0f, 1.0f), GuiGlobals.getInstance().createMaterial(ColorRGBA.Brown, true));
        staticBox.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        Geometry cube = GeometryUtils.createGeometry(new Box(0.5f, 0.5f, 0.5f), GuiGlobals.getInstance().createMaterial(ColorRGBA.randomColor(), true));
        cube.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        Geometry sphere = GeometryUtils.createGeometry(new Sphere(16, 16, 0.5f), GuiGlobals.getInstance().createMaterial(ColorRGBA.randomColor(), true));
        sphere.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        modelRegistry.register(new Model("static-box"), staticBox);
        modelRegistry.register(new Model("cube"), cube);
        modelRegistry.register(new Model("sphere"), sphere);

        // add bullet listeners
        bulletSystem.addPhysicalEntityListener(new PhysicalEntityDebugPublisher(entityData));
        bulletSystem.addPhysicalEntityListener(new PositionPublisher(entityData));

        // create static floor
        Geometry floor = GeometryUtils.createGeometry(new Quad(32, 32), GuiGlobals.getInstance().createMaterial(ColorRGBA.LightGray, true));
        floor.setName("Floor");
        floor.setShadowMode(RenderQueue.ShadowMode.Receive);
        floor.move(-16, 0, 16);
        floor.rotate(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        rootNode.attachChild(floor);
        entityData.setComponents(entityData.createEntity(), new Mass(0), new PhysicalShape("floor"), new WarpPosition(floor.getWorldTranslation(), floor.getWorldRotation()));

        // add some static entities
        int total = 10;
        IntStream.range(0, total).forEach(i -> {
            entityData.setComponents(entityData.createEntity(),
                    new Model("static-box"),
                    new Mass(0),
                    new WarpPosition(new Vector3f(FastMath.nextRandomInt(-15, 15), 1, FastMath.nextRandomInt(-15, 15)), new Quaternion()),
                    new PhysicalShape("static-box"));

        });
    }

    protected void setupLights() {
        ambientLight = new AmbientLight(new ColorRGBA(0.3f, 0.3f, 0.3f, 1.0f));
        directionalLight = new DirectionalLight(new Vector3f(-0.5f, -1.0f, -0.5f).normalizeLocal(), ColorRGBA.White);

        rootNode.addLight(ambientLight);
        rootNode.addLight(directionalLight);
    }

    protected void setupPostProcessing() {
        FilterPostProcessor fpp = new FilterPostProcessor(getAssetManager());
        getViewPort().addProcessor(fpp);

        // check sampling
        int samples = getContext().getSettings().getSamples();
        boolean aa = samples != 0;
        if (aa) {
            fpp.setNumSamples(samples);
        }

        // shadow filter
        DirectionalLightShadowFilter shadowFilter = new DirectionalLightShadowFilter(assetManager, 1024, 4);
        shadowFilter.setLight(directionalLight);
        shadowFilter.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        shadowFilter.setEdgesThickness(2);
        shadowFilter.setShadowIntensity(0.75f);
        shadowFilter.setLambda(0.65f);
        shadowFilter.setShadowZExtend(75);
        shadowFilter.setEnabled(true);
        fpp.addFilter(shadowFilter);

        // SSAO
        SSAOFilter ssaoFilter = new SSAOFilter();
        ssaoFilter.setEnabled(false);
        fpp.addFilter(ssaoFilter);

        // setup FXAA if regular AA is off
        if (!aa) {
            FXAAFilter fxaaFilter = new FXAAFilter();
            fxaaFilter.setEnabled(true);
            fpp.addFilter(fxaaFilter);
        }
    }

    public void shootBall() {
        Vector3f dir = cam.getDirection();

        entityData.setComponents(entityData.createEntity(),
                new Model("sphere"),
                new Mass(5),
                new PhysicalShape("ball"),
                new WarpPosition(new Vector3f(cam.getLocation()), new Quaternion()),
                new Impulse(dir.mult(10)));
    }

    public void shootCube() {
        Vector3f dir = cam.getDirection();

        entityData.setComponents(entityData.createEntity(),
                new Model("cube"),
                new Mass(10),
                new PhysicalShape("cube"),
                new WarpPosition(new Vector3f(cam.getLocation()), new Quaternion()),
                new Impulse(dir.mult(10)));
    }

    private boolean cameraEnabled = false;

    @Override
    public void simpleUpdate(float tpf) {
        ThirdPersonCamera camera = getStateManager().getState(ThirdPersonCamera.class);
        if (!cameraEnabled && camera.isEnabled()) {
            InputMapper inputMapper = GuiGlobals.getInstance().getInputMapper();
            Set<InputMapper.Mapping> dragMappings = inputMapper.getMappings(ThirdPersonCamera.FUNCTION_DRAG);
            dragMappings.forEach(inputMapper::removeMapping);
            inputMapper.map(ThirdPersonCamera.FUNCTION_DRAG, Button.MOUSE_BUTTON3);
            inputMapper.map(ThirdPersonCamera.FUNCTION_DRAG, KeyInput.KEY_V);
            cameraEnabled = true;
        }
        physicsTicks.setText(String.format("%d ticks per second", bulletSystem.getTicksPerSecond()));
    }
}
