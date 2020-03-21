//TODO: this should move to the implementation, an example should be provided in the test package
//package org.chimpstack.jme3.es.bullet;
//
//import com.simsilica.es.EntityData;
//import org.impstack.jme.es.Position;
//
///**
// * A {@link PhysicalEntityListener} implementation that publishes and updates {@link org.impstack.jme.es.Position}
// * components based on the {@link PhysicalEntity} location and rotation.
// * When a {@link PhysicalEntity} is removed, the {@link org.impstack.jme.es.Position} component is not removed from the
// * entity.
// */
//public class PhysicalEntityPositionPublisher implements PhysicalEntityListener {
//
//    private final EntityData entityData;
//
//    public PhysicalEntityPositionPublisher(EntityData entityData) {
//        this.entityData = entityData;
//    }
//
//    @Override
//    public void startFrame() {
//    }
//
//    @Override
//    public void physicalEntityAdded(PhysicalEntity physicalEntity) {
//        entityData.setComponent(physicalEntity.getEntityId(), new Position(physicalEntity.getLocation(), physicalEntity.getRotation()));
//    }
//
//    @Override
//    public void physicalEntityUpdated(PhysicalEntity physicalEntity) {
//        entityData.setComponent(physicalEntity.getEntityId(), new Position(physicalEntity.getLocation(), physicalEntity.getRotation()));
//    }
//
//    @Override
//    public void physicalEntityRemoved(PhysicalEntity physicalEntity) {
//    }
//
//    @Override
//    public void endFrame() {
//    }
//
//}
