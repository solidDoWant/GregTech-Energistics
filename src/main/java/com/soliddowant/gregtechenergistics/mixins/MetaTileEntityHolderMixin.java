package com.soliddowant.gregtechenergistics.mixins;

import javax.annotation.Nonnull;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.soliddowant.gregtechenergistics.covers.CoverAE2Stocker;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.util.EnumFacing;

/**
 * Mixin to make GregTech's MetaTileEntityHolder implement IGridHost.
 * This allows third-party AE2 conduits (like EnderIO ME Conduits) to detect
 * and connect to GregTech machines that have AE2 covers attached.
 */
@Mixin(value = MetaTileEntityHolder.class, remap = false)
public abstract class MetaTileEntityHolderMixin implements IGridHost {

    @Shadow
    public abstract MetaTileEntity getMetaTileEntity();

    /**
     * Helper method to find an AE2 cover on a specific side.
     */
    @Unique
    private CoverAE2Stocker gregtechenergistics$findAE2CoverOnSide(EnumFacing side) {
        MetaTileEntity holder = getMetaTileEntity();
        if (holder == null) {
            return null;
        }
        CoverBehavior cover = holder.getCoverAtSide(side);
        if (cover instanceof CoverAE2Stocker) {
            return (CoverAE2Stocker) cover;
        }
        return null;
    }

    /**
     * Helper method to find any AE2 cover on this holder.
     */
    @Unique
    private CoverAE2Stocker gregtechenergistics$findAnyAE2Cover() {
        for (EnumFacing side : EnumFacing.VALUES) {
            CoverAE2Stocker cover = gregtechenergistics$findAE2CoverOnSide(side);
            if (cover != null) {
                return cover;
            }
        }
        return null;
    }

    /**
     * Returns the grid node for the given direction.
     * Delegates to attached AE2 covers if present.
     */
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        // For directional queries, check the specific side
        if (dir != AEPartLocation.INTERNAL) {
            EnumFacing facing = dir.getFacing();
            if (facing != null) {
                CoverAE2Stocker cover = gregtechenergistics$findAE2CoverOnSide(facing);
                if (cover != null) {
                    return cover.getGridNode(dir);
                }
            }
        }

        // For internal or fallback, find any AE2 cover
        CoverAE2Stocker cover = gregtechenergistics$findAnyAE2Cover();
        if (cover != null) {
            return cover.getGridNode(dir);
        }

        return null;
    }

    /**
     * Returns the cable connection type for the given direction.
     * Delegates to attached AE2 covers if present.
     */
    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        // For directional queries, check the specific side
        if (dir != AEPartLocation.INTERNAL) {
            EnumFacing facing = dir.getFacing();
            if (facing != null) {
                CoverAE2Stocker cover = gregtechenergistics$findAE2CoverOnSide(facing);
                if (cover != null) {
                    return cover.getCableConnectionType(dir);
                }
            }
        }

        // For internal or fallback, find any AE2 cover
        CoverAE2Stocker cover = gregtechenergistics$findAnyAE2Cover();
        if (cover != null) {
            return cover.getCableConnectionType(dir);
        }

        return AECableType.NONE;
    }

    /**
     * Called when security is breached. Delegates to attached AE2 covers.
     */
    @Override
    public void securityBreak() {
        CoverAE2Stocker cover = gregtechenergistics$findAnyAE2Cover();
        if (cover != null) {
            cover.securityBreak();
        }
    }
}
