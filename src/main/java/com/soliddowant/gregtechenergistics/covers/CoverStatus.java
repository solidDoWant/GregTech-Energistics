package com.soliddowant.gregtechenergistics.covers;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.IStringSerializable;

import javax.annotation.Nonnull;

public enum CoverStatus implements IStringSerializable {
    RUNNING("cover.stocker.status.running"),
    OTHER_DISABLED("cover.stocker.status.other_disabled"),
    PATTERN_NOT_INSERTED("cover.stocker.status.pattern_not_inserted"),
    GRID_DISCONNECTED("cover.stocker.status.grid_disconnected"),
    FULLY_STOCKED("cover.stocker.status.fully_stocked"),
    MISSING_INPUTS("cover.stocker.status.missing_inputs"),
    INVALID_MULTIBLOCK("cover.stocker.status.invalid_multiblock"),
    MISSING_INPUT_SPACE("cover.stocker.status.missing_input_space"),
    MISSING_OUTPUT_SPACE("cover.stocker.status.missing_output_space");

    public String displayText;

    CoverStatus(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public String toString() {
        return I18n.hasKey(displayText) ? I18n.format(displayText) : displayText;
    }

    @Nonnull
    @Override
    public String getName() {
        return displayText;
    }
}