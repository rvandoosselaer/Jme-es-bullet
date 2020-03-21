//package org.chimpstack.jme3.es.bullet;
//
//import com.jme3.math.FastMath;
//import com.jme3.math.Vector3f;
//import com.simsilica.es.EntityData;
//import org.impstack.es.bullet.debug.PhysicalEntityDriverDebug;
//
///**
// * A starting implementation of the {@link PhysicalEntityDriver} interface for autonomous upright characters.
// * The linear velocity of the physical entity can be set using a direction ({@link #setMoveDirection(Vector3f)} and a
// * speed ({@link #setMoveSpeed(float)} value.
// * The angular velocity of the physical entity can be set using a view direction ({@link #setViewDirection(Vector3f)}
// * and a turning speed ({@link #setTurningSpeed(float)} value.
// * When debug is enabled, {@link PhysicalEntityDriverDebug} components are published.
// */
//public class BasePhysicalEntityDriver implements PhysicalEntityDriver {
//
//    // a value lower then 1 will decrease/reduce linear velocity. A value higher then 1 will increase the linear forces
//    protected static final float PHYSICS_DAMPING = 0.9f;
//    // Set the amount of rotation that will be applied. A value of zero will cancel all rotational force outcome.
//    protected static final Vector3f ANGULAR_FACTOR = new Vector3f(0, 1, 0);
//
//    protected boolean initialized = false;
//    protected final Vector3f moveDirection = Vector3f.ZERO;
//    protected final Vector3f viewDirection = new Vector3f(0, 0, 1);
//    protected float moveSpeed = 1.0f;
//    protected float turningSpeed = 1.0f;
//
//    protected PhysicalEntity physicalEntity;
//    protected RigidBodyEntity rigidBodyEntity;
//    protected boolean debugEnabled = false;
//    protected EntityData entityData;
//
//    public BasePhysicalEntityDriver() {
//    }
//
//    public BasePhysicalEntityDriver(EntityData entityData) {
//        this.entityData = entityData;
//    }
//
//    @Override
//    public void initialize(PhysicalEntity entity) {
//        if (!(entity instanceof RigidBodyEntity)) {
//            throw new IllegalArgumentException("PhysicalEntityDriver only supports PhysicalEntities of type RigidBodyEntity!");
//        }
//        this.physicalEntity = entity;
//        this.rigidBodyEntity = (RigidBodyEntity) entity;
//        this.rigidBodyEntity.setAngularFactor(ANGULAR_FACTOR);
//        this.initialized = true;
//    }
//
//    @Override
//    public void update(float tpf) {
//        // move the physical entity using the moveSpeed and moveDirection
//        move(tpf);
//
//        // turn the physical entity using the turningSpeed and viewDirection
//        turn(tpf);
//
//        // publish debug component when debug is enabled
//        if (isDebugEnabled() && entityData != null) {
//            entityData.setComponents(physicalEntity.getEntityId(),
//                    new PhysicalEntityDriverDebug(rigidBodyEntity.getLinearVelocity(), rigidBodyEntity.getRotation().mult(Vector3f.UNIT_Z)));
//        }
//    }
//
//    @Override
//    public void cleanup(PhysicalEntity entity) {
//        this.initialized = false;
//    }
//
//    public void setViewDirection(Vector3f direction) {
//        viewDirection.set(direction).normalizeLocal();
//    }
//
//    public Vector3f getViewDirection() {
//        return viewDirection;
//    }
//
//    public void setMoveDirection(Vector3f direction) {
//        moveDirection.set(direction).normalizeLocal();
//    }
//
//    public Vector3f getMoveDirection() {
//        return moveDirection;
//    }
//
//    public float getMoveSpeed() {
//        return moveSpeed;
//    }
//
//    public void setMoveSpeed(float moveSpeed) {
//        this.moveSpeed = moveSpeed;
//    }
//
//    public float getTurningSpeed() {
//        return turningSpeed;
//    }
//
//    public void setTurningSpeed(float turningSpeed) {
//        this.turningSpeed = turningSpeed;
//    }
//
//    public boolean isDebugEnabled() {
//        return debugEnabled;
//    }
//
//    public void setDebugEnabled(boolean debugEnabled) {
//        this.debugEnabled = debugEnabled;
//    }
//
//    public void setEntityData(EntityData entityData) {
//        this.entityData = entityData;
//    }
//
//    public boolean isInitialized() {
//        return initialized;
//    }
//
//    protected RigidBodyEntity getRigidBodyEntity() {
//        return rigidBodyEntity;
//    }
//
//    protected void move(float tpf) {
//        Vector3f localMoveDirection  = moveDirection.mult(60 * tpf * moveSpeed);
//
//        Vector3f velocity = rigidBodyEntity.getLinearVelocity();
//        Vector3f currentVelocity = velocity.clone();
//
//        // dampen existing x/z forces
//        float existingLeftVelocity = velocity.dot(Vector3f.UNIT_X);
//        float existingForwardVelocity = velocity.dot(Vector3f.UNIT_Z);
//        existingLeftVelocity = existingLeftVelocity * PHYSICS_DAMPING;
//        existingForwardVelocity = existingForwardVelocity * PHYSICS_DAMPING;
//        velocity.addLocal(new Vector3f(-existingLeftVelocity, 0, -existingForwardVelocity));
//
//        float speed = localMoveDirection.length();
//        // calculate the extra needed velocity (desired velocity - current velocity)
//        if (speed > 0) {
//            Vector3f localWalkDirection = localMoveDirection.normalize();
//            float existingVelocity = velocity.dot(localWalkDirection);
//            //calculate the final velocity in the desired direction
//            float finalVelocity = speed - existingVelocity;
//            localWalkDirection.multLocal(finalVelocity);
//            //add resulting vector to existing velocity
//            velocity.addLocal(localWalkDirection);
//        }
//
//        if (currentVelocity.distance(velocity) > FastMath.ZERO_TOLERANCE) {
//            // the current velocity is lower then the calculated velocity, apply the calculated velocity
//            rigidBodyEntity.setLinearVelocity(velocity);
//        }
//    }
//
//    protected void turn(float tpf) {
//        Vector3f currentDirection = rigidBodyEntity.getRotation().mult(Vector3f.UNIT_Z);
//        // get the angle we need to rotate to face the target
//        float angle = currentDirection.angleBetween(viewDirection);
//        Vector3f angleToVector = new Vector3f(0, angle, 0);
//        angleToVector.multLocal(60 * tpf * turningSpeed);
//        // use the cross product to determine the 'shortest' rotation.
//        // eg. when the target is 30° to the left, we want to rotate -30° instead of 330°
//        Vector3f crossProduct = currentDirection.cross(viewDirection);
//        if (crossProduct.y < 0) {
//            angleToVector.negateLocal();
//        }
//        if (angle > FastMath.ZERO_TOLERANCE) {
//            rigidBodyEntity.setAngularVelocity(angleToVector);
//        }
//    }
//
//}
