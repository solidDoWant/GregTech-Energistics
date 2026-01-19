package com.soliddowant.gregtechenergistics.gui;

import static appeng.helpers.ItemStackHelper.stackWriteToNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.util.AEPartLocation;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternOutputs;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import appeng.me.helpers.PlayerSource;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public class ExtendedPatternContainer extends ContainerMEMonitorable
        implements IContainerCraftingPacket, IOptionalSlotHost {
    protected final ExtendedPatternTerminalPart part;

    protected IItemHandler crafting; // 20 input slots (5x4)
    protected SlotFakeCraftingMatrix[] craftingSlots; // 20 slots
    protected OptionalSlotFake[] outputSlots; // 12 slots
    protected SlotRestrictedInput patternSlotIN;
    protected SlotRestrictedInput patternSlotOUT;

    public ExtendedPatternContainer(@Nonnull final InventoryPlayer ip,
            @Nonnull final ExtendedPatternTerminalPart part) {
        super(ip, part, false); // Pass false to prevent parent from binding player inventory
        this.part = part;

        // Initialize slot arrays
        this.craftingSlots = new SlotFakeCraftingMatrix[20]; // 5x4 input grid
        this.outputSlots = new OptionalSlotFake[12]; // 12 output slots

        final IItemHandler patternInv = this.part.getInventoryByName("pattern");
        final IItemHandler output = this.part.getInventoryByName("output");
        this.crafting = this.part.getInventoryByName("crafting");

        // Add crafting input slots (5x4 grid)
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                this.addSlotToContainer(this.craftingSlots[x + y * 5] = new SlotFakeCraftingMatrix(this.crafting,
                        x + y * 5, 9 + x * 18, 84 + y * 18));
            }
        }

        // Add output slots (4x3 grid = 12 slots)
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                int index = x + y * 3;
                this.addSlotToContainer(this.outputSlots[index] = new SlotPatternOutputs(output, this, index,
                        117, 84, x, y, 1));
                this.outputSlots[index].setRenderDisabled(false);
                this.outputSlots[index].setIIcon(-1);
            }
        }

        // Add pattern slots (blank in, encoded out)
        this.addSlotToContainer(this.patternSlotIN = new SlotRestrictedInput(
                SlotRestrictedInput.PlacableItemType.BLANK_PATTERN, patternInv, 0, 184, 88, this.getInventoryPlayer()));
        this.addSlotToContainer(
                this.patternSlotOUT = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN,
                        patternInv, 1, 184, 131, this.getInventoryPlayer()));

        this.patternSlotOUT.setStackLimit(1);

        // Bind player inventory (positioned at bottom of GUI)
        this.bindPlayerInventory(ip, 1, 167);

        // Fix the view cell slot positions after parent constructor
        for (int y = 0; y < 5; y++) {
            this.cellView[y].xPos = 223;
            this.cellView[y].yPos = y * 18 + 28;
        }
    }

    @Nullable
    public static ExtendedPatternContainer getServerGuiContainer(AEPartLocation side, EntityPlayer player, World world,
            int x, int y, int z) {
        ExtendedPatternTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side,
                ExtendedPatternTerminalPart.class);
        if (part == null)
            return null;

        ExtendedPatternContainer container = new ExtendedPatternContainer(player.inventory, part);

        container.setOpenContext(new ContainerOpenContext(part));
        container.getOpenContext().setWorld(world);
        container.getOpenContext().setX(x);
        container.getOpenContext().setY(y);
        container.getOpenContext().setZ(z);
        container.getOpenContext().setSide(side);
        return container;
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isClient())
            return;

        super.detectAndSendChanges();
    }

    @Override
    public void onSlotChange(final Slot s) {
        // When an encoded pattern is placed in the output slot, sync all
        // crafting/output slots to client
        if (s == this.patternSlotOUT && Platform.isServer()) {
            for (final IContainerListener listener : this.listeners) {
                for (final Slot slot : this.inventorySlots) {
                    if (slot instanceof OptionalSlotFake || slot instanceof SlotFakeCraftingMatrix) {
                        listener.sendSlotContents(this, slot.slotNumber, slot.getStack());
                    }
                }
                if (listener instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) listener).isChangingQuantityOnly = false;
                }
            }
            this.detectAndSendChanges();
        }
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if (name.equals("player")) {
            return new net.minecraftforge.items.wrapper.PlayerInvWrapper(this.getInventoryPlayer());
        }
        return this.part.getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return false;
    }

    @Override
    public IGridNode getNetworkNode() {
        return this.part.getActionableNode();
    }

    @Override
    public IActionSource getActionSource() {
        return new PlayerSource(this.getPlayerInv().player, this.part);
    }

    @Override
    public boolean isSlotEnabled(int idx) {
        return true;
    }

    // IContainerCraftingPacket implementation
    public void encodeAndMoveToInventory() {
        encode();
        ItemStack output = this.patternSlotOUT.getStack();
        if (!output.isEmpty()) {
            if (!getPlayerInv().addItemStackToInventory(output)) {
                getPlayerInv().player.dropItem(output, false);
            }
            this.patternSlotOUT.putStack(ItemStack.EMPTY);
        }
    }

    public void encode() {
        ItemStack output = this.patternSlotOUT.getStack();

        final ItemStack[] in = this.getInputs();
        final ItemStack[] out = this.getOutputs();

        // No set inputs or outputs, nothing to do
        if (in == null || out == null) {
            return;
        }

        if (!output.isEmpty()) {
            // Verify that the output slot is either empty or a pattern
            if (!this.isPattern(output))
                return;
        } else {
            // Output slot is empty, so try to consume a blank pattern from the input slot
            ItemStack removedBlankPattern = this.patternSlotIN.decrStackSize(1);
            // If the output is _still_ empty or not a blank pattern, abort
            if (removedBlankPattern.isEmpty() || !this.isPattern(removedBlankPattern))
                return;

            // Add a new encoded pattern item to the output slot
            Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
            if (maybePattern.isPresent())
                output = maybePattern.get();
        }

        // Encode inputs/outputs into NBT
        final NBTTagCompound encodedValue = new NBTTagCompound();

        final NBTTagList tagIn = new NBTTagList();
        final NBTTagList tagOut = new NBTTagList();

        for (final ItemStack i : in) {
            tagIn.appendTag(this.createItemTag(i));
        }

        for (final ItemStack i : out) {
            tagOut.appendTag(this.createItemTag(i));
        }

        encodedValue.setTag("in", tagIn);
        encodedValue.setTag("out", tagOut);
        encodedValue.setBoolean("crafting", false); // Always processing mode
        encodedValue.setBoolean("substitute", false); // No substitution in processing

        output.setTagCompound(encodedValue);

        patternSlotOUT.putStack(output);
    }

    protected ItemStack[] getInputs() {
        // For processing mode, only return non-empty inputs (compacted)
        final java.util.List<ItemStack> inputList = new java.util.ArrayList<>();
        boolean hasValue = false;

        for (int x = 0; x < this.craftingSlots.length; x++) {
            ItemStack stack = this.craftingSlots[x].getStack();
            if (!stack.isEmpty()) {
                inputList.add(stack);
                hasValue = true;
            }
        }

        if (hasValue) {
            return inputList.toArray(new ItemStack[0]);
        }

        return null;
    }

    protected ItemStack[] getOutputs() {
        final List<ItemStack> list = new ArrayList<>(outputSlots.length);
        boolean hasValue = false;

        for (final OptionalSlotFake outputSlot : this.outputSlots) {
            final ItemStack out = outputSlot.getStack();

            if (!out.isEmpty() && out.getCount() > 0) {
                list.add(out);
                hasValue = true;
            }
        }

        if (hasValue) {
            return list.toArray(new ItemStack[0]);
        }

        return null;
    }

    boolean isPattern(final ItemStack output) {
        if (output.isEmpty()) {
            return false;
        }

        final IDefinitions definitions = AEApi.instance().definitions();

        boolean isPattern = definitions.items().encodedPattern().isSameAs(output);
        isPattern |= definitions.materials().blankPattern().isSameAs(output);

        return isPattern;
    }

    NBTBase createItemTag(final ItemStack i) {
        final NBTTagCompound c = new NBTTagCompound();

        if (!i.isEmpty()) {
            stackWriteToNBT(i, c);
        }

        return c;
    }

    public void clear() {
        for (final net.minecraft.inventory.Slot s : this.craftingSlots) {
            s.putStack(ItemStack.EMPTY);
        }

        for (final net.minecraft.inventory.Slot s : this.outputSlots) {
            s.putStack(ItemStack.EMPTY);
        }

        this.detectAndSendChanges();
    }
}
