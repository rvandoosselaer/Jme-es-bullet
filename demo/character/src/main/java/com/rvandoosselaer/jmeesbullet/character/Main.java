package com.rvandoosselaer.jmeesbullet.character;

import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.rvandoosselaer.jmeesbullet.BulletSystem;
import com.rvandoosselaer.jmeesbullet.CollisionShapeHelper;
import com.rvandoosselaer.jmeesbullet.DefaultPhysicalShapeRegistry;
import com.rvandoosselaer.jmeesbullet.PhysicalShapeRegistry;
import com.rvandoosselaer.jmeesbullet.debug.BulletSystemDebugState;
import com.rvandoosselaer.jmeesbullet.debug.PhysicalEntityDebugPublisher;
import com.rvandoosselaer.jmeesbullet.es.Mass;
import com.rvandoosselaer.jmeesbullet.es.PhysicalShape;
import com.rvandoosselaer.jmeesbullet.es.WarpPosition;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.input.AnalogFunctionListener;
import com.simsilica.lemur.input.FunctionId;
import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.input.InputState;
import com.simsilica.lemur.input.StateFunctionListener;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.state.GameSystemsState;

public class Main extends SimpleApplication implements StateFunctionListener, AnalogFunctionListener {

    public static final FunctionId FUNCTION_MOVE = new FunctionId("move");
    public static final FunctionId FUNCTION_STRAFE = new FunctionId("strafe");
    public static final FunctionId FUNCTION_JUMP = new FunctionId("jump");

    private GameSystemsState systems;
    private EntityData entityData;
    private BulletSystem bulletSystem;
    private PhysicalShapeRegistry shapeRegistry;
    private AmbientLight ambientLight;
    private DirectionalLight directionalLight;
    private ModelRegistry modelRegistry;
    private EntityId playerId;
    private WatchedEntity player;
    private InputMapper inputMapper;
    private Node cameraTarget = new Node();

    private boolean setupComplete = false;
    private boolean rotating = false;
    private float yaw;
    private float turnSpeed = 1;
    private float moveSpeed = 1;
    private float move;
    private float strafe;
    private boolean jumping;

    public static void main(String[] args) {
        new Main().start();
    }

    public Main() {
        super(new StatsAppState(),
                new GameSystemsState(true),
                new ChaseCameraAppState());
    }

    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        BaseStyles.loadGlassStyle();
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");

        setupGameSystems();

        setupLights();

        setupPostProcessing();

        inputMapper = GuiGlobals.getInstance().getInputMapper();
        inputMapper.map(FUNCTION_MOVE, KeyInput.KEY_W);
        inputMapper.map(FUNCTION_MOVE, KeyInput.KEY_UP);
        inputMapper.map(FUNCTION_MOVE, InputState.Negative, KeyInput.KEY_S);
        inputMapper.map(FUNCTION_MOVE, InputState.Negative, KeyInput.KEY_DOWN);
        inputMapper.map(FUNCTION_STRAFE, InputState.Negative, KeyInput.KEY_A);
        inputMapper.map(FUNCTION_STRAFE, KeyInput.KEY_D);
        inputMapper.map(FUNCTION_JUMP, KeyInput.KEY_SPACE);

        inputMapper.addStateListener(this, FUNCTION_JUMP);
        inputMapper.addAnalogListener(this, FUNCTION_MOVE, FUNCTION_STRAFE);

        ChaseCameraAppState chaseCameraAppState = getStateManager().getState(ChaseCameraAppState.class);
        chaseCameraAppState.setDragToRotate(true);
        chaseCameraAppState.setDefaultVerticalRotation(45 * FastMath.DEG_TO_RAD);
        chaseCameraAppState.setTarget(cameraTarget);
    }

    protected void setupGameSystems() {
        systems = getStateManager().getState(GameSystemsState.class);

        // register global objects and systems
        entityData = systems.register(EntityData.class, new DefaultEntityData());
        shapeRegistry = systems.register(PhysicalShapeRegistry.class, new DefaultPhysicalShapeRegistry());
        modelRegistry = systems.register(ModelRegistry.class, new DefaultModelRegistry());
        bulletSystem = systems.register(BulletSystem.class, new BulletSystem());
        getStateManager().attach(new BulletSystemDebugState(entityData, shapeRegistry));
        getStateManager().attach(new VisualState(entityData, modelRegistry));
        getStateManager().attach(new PlayerInputState(entityData, bulletSystem));

        // load the level
        Spatial level = getAssetManager().loadModel("Scenes/demo-level.blend.j3o");
        level.depthFirstTraversal(visitor -> {
            if (visitor instanceof Geometry) {
                ColorRGBA diffuse = ((Geometry) visitor).getMaterial().getParamValue("Diffuse");
                ((Geometry) visitor).getMaterial().setColor("Ambient", diffuse);
            }
        });
        level.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(level);

        shapeRegistry.register(new PhysicalShape("level"), CollisionShapeHelper.createMeshShape(level));
        entityData.setComponents(entityData.createEntity(), new Mass(0), new PhysicalShape("level"), new WarpPosition(level.getWorldTranslation(), level.getWorldRotation()));

        // load the player
        shapeRegistry.register(new PhysicalShape("player"), CollisionShapeHelper.createCapsuleShape(0.5f, 1.4f, true));
        Spatial monkey = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        monkey.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        modelRegistry.register(new Model("monkey"), monkey);

        playerId = entityData.createEntity();
        entityData.setComponents(playerId, new PlayerInput(new Vector3f(), new Quaternion(), false), new Mass(80f), new PhysicalShape("player"), new WarpPosition(new Vector3f(-5, 0.1f, 0), new Quaternion()), new Model("monkey"));

        player = entityData.watchEntity(playerId, Position.class, Velocity.class);

        // load another player
        entityData.setComponents(entityData.createEntity(), new Model("monkey"), new Mass(80f), new PhysicalShape("player"), new WarpPosition(new Vector3f(5, 0.1f, 3), new Quaternion()));

        // add bullet listeners
        bulletSystem.addPhysicalEntityListener(new PositionPublisher(entityData));
        bulletSystem.addPhysicalEntityListener(new PhysicalEntityDebugPublisher(entityData));
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

    @Override
    public void valueChanged(FunctionId func, InputState value, double tpf) {
        if (func == FUNCTION_JUMP) {
            jumping = value != InputState.Off;
        }
    }

    @Override
    public void valueActive(FunctionId func, double value, double tpf) {
        if (func == FUNCTION_MOVE) {
            move = (float) value;
        } else if (func == FUNCTION_STRAFE) {
            strafe = (float) -value;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        if (bulletSystem.isInitialized() && !setupComplete) {
            // set gravity
            bulletSystem.getPhysicsSpace().setGravity(new Vector3f(0, -20f, 0));
            // add filter to disable player collisions; return true if the collision should happen, false otherwise
            bulletSystem.getPhysicsSpace().addCollisionGroupListener((nodeA, nodeB) -> {
                // when both PhysicsCollisionObject have a the player's collisionshape, we ignore the collision
                CollisionShape shape = shapeRegistry.get(new PhysicalShape("player"));
                // when one of the collision objects isn't a player, the collision should happen
                return !nodeA.getCollisionShape().equals(shape) || !nodeB.getCollisionShape().equals(shape);
            }, PhysicsCollisionObject.COLLISION_GROUP_01);
            setupComplete = true;
        }

        // input
        Vector3f movement = new Vector3f(strafe * moveSpeed, 0, move * moveSpeed);

        float[] angles = new float[3];
        cam.getRotation().toAngles(angles);
        Quaternion rotation = new Quaternion().fromAngles(0, angles[1], 0);

        movement = rotation.mult(movement);

        player.set(new PlayerInput(movement, rotation, jumping));
        // this does exactly the same as watchedEntity.set()
        // entityData.setComponent(playerId, new PlayerInput(new Vector3f(), new Quaternion().fromAngles(0, angles[1], 0)));

        if (player.applyChanges()) {
            cameraTarget.setLocalTranslation(player.get(Position.class).getLocation());
        }
    }
}
