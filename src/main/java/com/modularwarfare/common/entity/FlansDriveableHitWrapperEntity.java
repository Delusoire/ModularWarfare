package com.modularwarfare.common.entity;

import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EnumDriveablePart;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class FlansDriveableHitWrapperEntity extends Entity {
    public EntityDriveable driveable;
    public EnumDriveablePart part;

    public FlansDriveableHitWrapperEntity(World worldIn) {
        super(worldIn);
    }

    public FlansDriveableHitWrapperEntity(World worldIn, EntityDriveable driveable, EnumDriveablePart part) {
        super(worldIn);
        this.driveable = driveable;
        this.part = part;
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {

    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {

    }
}
