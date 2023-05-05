package com.modularwarfare.raycast;

import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.guns.raytracing.FlansModRaytracer;
import com.modularwarfare.common.entity.FlansDriveableHitWrapperEntity;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import com.modularwarfare.common.hitbox.hits.OBBHit;
import com.modularwarfare.common.vector.Matrix4f;
import com.modularwarfare.common.vector.Vector3f;
import com.modularwarfare.raycast.obb.OBBModelBox;
import com.modularwarfare.raycast.obb.OBBPlayerManager;
import com.modularwarfare.raycast.obb.OBBPlayerManager.Line;
import com.modularwarfare.raycast.obb.OBBPlayerManager.PlayerOBBModelObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class FlansRayCasting extends DefaultRayCasting {

    public BulletHit computeDetection(World world, float x, float y, float z, float tx, float ty, float tz, float borderSize, HashSet<Entity> excluded, boolean collideablesOnly, int ping) {
        Vec3d startVec = new Vec3d(x, y, z);
        Vec3d endVec = new Vec3d(tx, ty, tz);
        Vec3d lookVec = endVec.subtract(startVec);

        float minX = Math.min(x, tx);
        float minY = Math.min(y, ty);
        float minZ = Math.min(z, tz);
        float maxX = Math.max(x, tx);
        float maxY = Math.max(y, ty);
        float maxZ = Math.max(z, tz);
        AxisAlignedBB bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ).grow(borderSize, borderSize, borderSize);
        List<Entity> allEntities = world.getEntitiesWithinAABBExcludingEntity(null, bb);
        RayTraceResult blockHit = rayTraceBlocks(world, startVec, endVec, true, true, false);

        startVec = new Vec3d(x, y, z);
        endVec = new Vec3d(tx, ty, tz);
        float maxDistance = (float) endVec.distanceTo(startVec);
        if (blockHit != null) {
            maxDistance = (float) blockHit.hitVec.distanceTo(startVec);
            endVec = blockHit.hitVec;
        }

        Vector3f rayVec=new Vector3f(endVec.x-startVec.x, endVec.y-startVec.y, endVec.z-startVec.z);
        float len=rayVec.length();
        Vector3f normlVec=rayVec.normalise(null);
        OBBModelBox ray=new OBBModelBox();
        float pitch=(float) Math.asin(normlVec.y);
        normlVec.y=0;
        normlVec=normlVec.normalise(null);
        float yaw=(float)Math.asin(normlVec.x);
        if(normlVec.z<0) {
            yaw=(float) (Math.PI-yaw);
        }
        Matrix4f matrix=new Matrix4f();
        matrix.rotate(yaw, new Vector3f(0, 1, 0));
        matrix.rotate(pitch, new Vector3f(-1, 0, 0));
        ray.center=new Vector3f((startVec.x+endVec.x)/2, (startVec.y+endVec.y)/2, (startVec.z+endVec.z)/2);
        ray.axis.x=new Vector3f(0, 0, 0);
        ray.axis.y=new Vector3f(0, 0, 0);
        ray.axis.z=Matrix4f.transform(matrix, new Vector3f(0, 0, len/2), null);
        ray.axisNormal.x=Matrix4f.transform(matrix, new Vector3f(1, 0, 0), null);
        ray.axisNormal.y=Matrix4f.transform(matrix, new Vector3f(0, 1, 0), null);
        ray.axisNormal.z=Matrix4f.transform(matrix, new Vector3f(0, 0, 1), null);


        OBBPlayerManager.lines.add(new Line(ray));
        OBBPlayerManager.lines.add(new Line(new Vector3f(startVec), new Vector3f(endVec)));
        //Iterate over all entities
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity obj = world.loadedEntityList.get(i);

            if (excluded == null || !excluded.contains(obj)) {
                if (obj instanceof EntityPlayer) {
                    PlayerOBBModelObject obbModelObject = OBBPlayerManager.getPlayerOBBObject(obj.getName());
                    OBBModelBox finalBox=null;
                    boolean isBodyShot=false;
                    List<OBBModelBox> boxes = obbModelObject.calculateIntercept(ray);
                    if (boxes.size() > 0) {
                        double distanceSq = Double.MAX_VALUE;
                        for (OBBModelBox obb : boxes) {
                            if (obb.name.equals("obb_head")) {
                                finalBox=obb;
                                break;
                            } else if (obb.name.equals("obb_body")) {
                                finalBox=obb;
                                isBodyShot=true;
                            } else if (!isBodyShot) {
                                double d = startVec
                                        .squareDistanceTo(new Vec3d(obb.center.x, obb.center.y, obb.center.z));
                                if (d < distanceSq) {
                                    distanceSq = d;
                                    finalBox=obb;
                                }
                            }
                        }
                        RayTraceResult intercept = new RayTraceResult(obj, new Vec3d(finalBox.center.x, finalBox.center.y, finalBox.center.z));
                        return new OBBHit((EntityPlayer)obj,finalBox.copy(), intercept);
                    }
                }
            }
        }

        Entity closestHitEntity = null;
        Vec3d hit = null;
        float closestHit = maxDistance;
        float currentHit = 0.f;
        AxisAlignedBB entityBb;
        RayTraceResult intercept;
        for (Entity ent : allEntities) {
            if ((ent.canBeCollidedWith() || !collideablesOnly) && (excluded == null || !excluded.contains(ent))) {
                if (ent instanceof EntityLivingBase && !(ent instanceof EntityPlayer)) {
                    EntityLivingBase entityLivingBase = (EntityLivingBase) ent;
                    if (!ent.isDead && entityLivingBase.getHealth() > 0.0F) {
                        float entBorder = ent.getCollisionBorderSize();
                        entityBb = ent.getEntityBoundingBox();
                        entityBb = entityBb.grow(entBorder, entBorder, entBorder);
                        intercept = entityBb.calculateIntercept(startVec, endVec);
                        if (intercept != null) {
                            currentHit = (float) intercept.hitVec.distanceTo(startVec);
                            hit = intercept.hitVec;
                            if (currentHit < closestHit || currentHit == 0) {
                                closestHit = currentHit;
                                closestHitEntity = ent;
                            }
                        }
                    }
                } else if (ent instanceof EntityGrenade) {
                    float entBorder = ent.getCollisionBorderSize();
                    entityBb = ent.getEntityBoundingBox();
                    entityBb = entityBb.grow(entBorder, entBorder, entBorder);
                    intercept = entityBb.calculateIntercept(startVec, endVec);
                    if (intercept != null) {
                        hit = intercept.hitVec;
                        currentHit = (float) hit.distanceTo(startVec);
                        if (currentHit < closestHit || currentHit == 0) {
                            closestHit = currentHit;
                            closestHitEntity = ent;
                        }
                    }
                } else if (ent instanceof EntityDriveable) {
                    EntityDriveable driveable = (EntityDriveable) ent;
                    if (!driveable.isPartOfThis(Minecraft.getMinecraft().player)) {
                        if (driveable.getDistanceSq(startVec.x, startVec.y, startVec.z)
                                <= Math.pow(driveable.getDriveableType().bulletDetectionRadius + 500f, 2)) {
                            com.flansmod.common.vector.Vector3f origin = new com.flansmod.common.vector.Vector3f(startVec);
                            com.flansmod.common.vector.Vector3f motion = new com.flansmod.common.vector.Vector3f(lookVec);
                            Optional<FlansModRaytracer.BulletHit> optionalHit = driveable.attackFromBullet(origin, motion).stream().sorted().findFirst();
                            if (optionalHit.isPresent()) {
                                FlansModRaytracer.DriveableHit driveableHit = (FlansModRaytracer.DriveableHit) optionalHit.get();
                                driveable = driveableHit.driveable;
                                ent = new FlansDriveableHitWrapperEntity(null, driveable, driveableHit.part);
                                hit = driveable.getPositionVector(); // approximate
                                currentHit = (float) hit.distanceTo(startVec);
                                if (currentHit < closestHit || currentHit == 0) {
                                    closestHit = currentHit;
                                    closestHitEntity = ent;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (closestHitEntity != null && hit != null) {
            blockHit = new RayTraceResult(closestHitEntity, hit);
        }

        return new BulletHit(blockHit);
    }
}