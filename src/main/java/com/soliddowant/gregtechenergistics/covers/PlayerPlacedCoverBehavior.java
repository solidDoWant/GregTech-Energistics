package com.soliddowant.gregtechenergistics.covers;

import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.ICoverable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public abstract class PlayerPlacedCoverBehavior extends CoverBehavior {
    protected EntityPlayer placingPlayer;

    public PlayerPlacedCoverBehavior(ICoverable coverHolder, EnumFacing attachedSide) {
        super(coverHolder, attachedSide);
    }

    public EntityPlayer getPlacingPlayer() {
        return placingPlayer;
    }

    public void setPlacingPlayer(EntityPlayer player) {
        this.placingPlayer = player;
    }
}
