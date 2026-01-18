package com.soliddowant.gregtechenergistics.parts;

import javax.annotation.Nonnull;

import com.soliddowant.gregtechenergistics.GregTechEnergisticsMod;
import com.soliddowant.gregtechenergistics.Tags;
import com.soliddowant.gregtechenergistics.gui.GuiProxy;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.parts.IPartModel;
import appeng.api.storage.data.IAEItemStack;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractPartDisplay;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandler;

public class ExtendedPatternTerminalPart extends AbstractPartDisplay implements IAEAppEngInventory {
    public static ResourceLocation[] MODELS = new ResourceLocation[] {
            new ResourceLocation(Tags.MODID, "part/extendedpattern.terminal/on"), // 0
            new ResourceLocation(Tags.MODID, "part/extendedpattern.terminal/off"), // 1
    };
    public static final ResourceLocation MODEL_OFF = MODELS[1];
    public static final ResourceLocation MODEL_ON = MODELS[0];
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    // Pattern encoding inventories
    protected AppEngInternalInventory crafting;  // 20 input slots (5x4)
    protected AppEngInternalInventory output;    // 12 output slots (4x3)
    protected AppEngInternalInventory pattern;   // 2 slots (blank in, encoded out)

    public ExtendedPatternTerminalPart(final ItemStack is) {
        super(is);
        this.crafting = new AppEngInternalInventory(this, 20);  // 5x4 grid
        this.output = new AppEngInternalInventory(this, 12);    // 12 outputs
        this.pattern = new AppEngInternalInventory(this, 2);    // blank/encoded patterns
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d pos) {
        if (super.onPartActivate(player, hand, pos))
            return false;

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            player.openGui(GregTechEnergisticsMod.instance,
                    GuiProxy.getOrdinalFromGuiId(1) | this.getSide().ordinal(),
                    player.getEntityWorld(),
                    this.getTile().getPos().getX(),
                    this.getTile().getPos().getY(),
                    this.getTile().getPos().getZ());
        }

        return true;
    }

    @Override
    public void getDrops(final java.util.List<ItemStack> drops, final boolean wrenched) {
        super.getDrops(drops, wrenched);

        // Drop any patterns stored in the terminal
        for (final ItemStack is : this.pattern) {
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.pattern.readFromNBT(data, "pattern");
        this.output.readFromNBT(data, "outputList");
        this.crafting.readFromNBT(data, "crafting");
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.pattern.writeToNBT(data, "pattern");
        this.output.writeToNBT(data, "outputList");
        this.crafting.writeToNBT(data, "crafting");
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
            final ItemStack removed, final ItemStack added) {
        if (inv == this.pattern && slot == 1) {
            // Pattern was placed in output slot - decode it back to inputs/outputs
            final ItemStack is = this.pattern.getStackInSlot(1);
            if (!is.isEmpty() && is.getItem() instanceof ICraftingPatternItem) {
                final ICraftingPatternItem pattern = (ICraftingPatternItem) is.getItem();
                final ICraftingPatternDetails details = pattern.getPatternForItem(is, this.getHost().getTile().getWorld());
                if (details != null) {
                    // Load inputs
                    for (int x = 0; x < this.crafting.getSlots() && x < details.getInputs().length; x++) {
                        final IAEItemStack item = details.getInputs()[x];
                        this.crafting.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
                    }

                    // Load outputs
                    for (int x = 0; x < this.output.getSlots(); x++) {
                        final IAEItemStack item;
                        if (x < details.getOutputs().length) {
                            item = details.getOutputs()[x];
                        } else {
                            item = null;
                        }
                        this.output.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
                    }
                }
            }
        }

        this.getHost().markForSave();
    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("crafting")) {
            return this.crafting;
        }

        if (name.equals("output")) {
            return this.output;
        }

        if (name.equals("pattern")) {
            return this.pattern;
        }

        return super.getInventoryByName(name);
    }

    @Override
    public void saveChanges() {
        this.getHost().markForSave();
    }
}
