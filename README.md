# ![Jme-es-minie](icon-64.png) Jme-es-minie

A Minie Zay-ES integration for jMonkeyEngine.

## How to use it

:warning: This library is still under active development! The published version on jcenter is a SNAPSHOT version and could be changed at any time.

## How to use it

This library is still under active development and is not yet placed on a CDN like jcenter or mavencentral.
However, it is possible to build the library and place it in your local maven repository.

To build the library and install it in your local maven repository, run:

```
./gradlew publishtomavenlocal
```

Once installed in your local maven repository, you can include this library in your gradle project by adding this in your `build.gradle` file.

```
repositories {
    mavenLocal()
}
dependencies {
    compile "org.chimpstack:jme3-es-bullet:+"
}
```

## Get started

To use the ES based bullet integration you need to add the `BulletSystem` to the `GameSystemManager`. The BulletSystem
also needs access to the `EntityData` and `PhysicalShapeRegistry`. That's it really!

The EntityData and PhysicalShapeRegistry can be provided in the constructor when initializing the BulletSystem. Or they
can be injected automatically when they are registered as system-level objects to the GameSystemManager.

```java
entityData = systems.register(EntityData.class, new DefaultEntityData());
shapeRegistry = systems.register(PhysicalShapeRegistry.class, new DefaultPhysicalShapeRegistry());
bulletSystem = systems.register(BulletSystem.class, new BulletSystem());

// registered system level objects can be retrieved using the AbstractGameSystem.getSystem() implementation
EntityData entityData = getSystem(EntityData.class);
``` 

or

```java
EntityData entityData = new DefaultEntityData();
PhysicalShapeRegistry shapeRegistry = new DefaultPhysicalShapeRegistry();
bulletSystem = systems.register(BulletSystem.class, new BulletSystem(entityData, shapeRegistry));
```

The PhysicalShapeRegistry is used to link a `CollisionShape` to a physical entity using a `PhysicalShape` component.

```java
PhysicalShapeRegistry shapeRegistry = new DefaultPhysicalShapeRegistry();
shapeRegistry.register(new PhysicalShape("cube"), new BoxCollisionShape(new Vector3f(0.5f, 0.5f, 0.5f)));
```

The library has a `CollisionShapeHelper` class with multiple static methods to facilitate the creation of CollisionShape 
objects.

An entity will be picked up by the BulletSystem when it has the following components:
-   Mass
-   PhysicalShape
-   WarpPosition

Other systems can hook in on the Bullet update loop and can be notified about updates of entities in the physics space 
using a `PhysicalEntityListener`.

A PhysicalEntityListener implementation can for example be used to publish Position components on a physical entity to 
update the location and rotation of the Model of the entity.

```java
bulletSystem.addPhysicalEntityListener(new PositionPublisher());

public class PositionPublisher implements PhysicalEntityListener {

    ...
    
    @Override
    public void physicalEntityUpdated(PhysicalEntity physicalEntity) {
        entityData.setComponent(physicalEntity.getEntityId(), new Position(physicalEntity.getLocation(), physicalEntity.getRotation()));
    }
    
    ...

}
```

### Demo

Two demo applications are included as examples.

The cubes demo is an application where you can shoot boxes and balls on a platform.

You can start the cubes demo using gradle:

```bash
$ ./gradlew :demo:cubes:run
```

The character demo is an application where you control a physics character in a level.

You can start the character demo using gradle:

```bash
$ ./gradlew :demo:character:run
```

### Acknowledgements

-   Icon made by [Freepik](https://www.freepik.com/home) from www.flaticon.com


## Bullet documentation

Some information gathered about bullet during the creation of the Bullet Zay-ES integration library.

### Continuous collision detection

CCD is short for Continuous Collision Detection, which is a workaround for a common problem in game physics: a fast 
moving body might not collide with an obstacle if in one frame it is "before" the obstacle, and in the next one it is 
already "behind" the obstacle. At no frame the fast moving body overlaps with the obstacle, and thus no response is 
created. This is what CCD is for. CCD checks for collisions in between frames, and thus can prevent fast moving objects 
from passing through thin obstacles.

Bullet has built-in support for CCD, but bodies have to be configured properly to enable CCD checks.

When checking for collision in between frames Bullet does not use the full collision shape (or shapes) of a body - this 
would make continuous collision detection too slow. Instead Bullet uses a sphere shape, the so-called "swept sphere". 
"swept" because the sphere is swept from the original position to the new position of the body. So, in order to enable 
CCD checks on a body we have to setup this sphere, and a CCD motion threshold:

```
fastMovingRigidBody.setCcdMotionThreshold(1e-7) (eg. 1e-7 = 1 x 10^-7 = 0.0000007)
fastMovingRigidBody.setCcdSweptSphereRadius(0.50)
```

We have to set up the swept sphere only on the fast moving dynamic bodies. There is no need to do anything for the 
static or slow moving obstacles. The CcdMotionTreshold is an amount of motion that is required to activate continuous 
collision detection.

Set the setCcdMotionThreshold to a value `>0` or `0` to disable.

### Capsule collision shape

The size of the capsule is set using a radius and a height value. These values are the radius of the top and bottom semi
spheres and the radius and height of the cylinder.
If you want a capsule with a radius of 1 and a height of 5, you should give a radius of 1 and a height of 3 to the
capsule collison constructor. The final height of the capsule is the radius of the bottom sphere + the height of the 
cylinder + the radius of the top sphere. 

This behaviour is simplified in the `CollisionShapeHelper.createCapsuleShape(float radius, float height, boolean centerAtBottom)`.
The height value in this factory method is the final totalling height of the capsule.
