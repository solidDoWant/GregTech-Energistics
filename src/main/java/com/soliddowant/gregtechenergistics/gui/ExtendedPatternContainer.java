package com.soliddowant.gregtechenergistics.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.soliddowant.gregtechenergistics.parts.ExtendedPatternTerminalPart;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.util.AEPartLocation;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.slot.IOptionalSlotHost;
import appeng.container.slot.OptionalSlotFake;
import appeng.container.slot.SlotFakeCraftingMatrix;
import appeng.container.slot.SlotPatternOutputs;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import appeng.me.helpers.PlayerSource;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import static appeng.helpers.ItemStackHelper.stackWriteToNBT;

public class ExtendedPatternContainer extends AEBaseContainer implements IContainerCraftingPacket, IOptionalSlotHost {
    protected final ExtendedPatternTerminalPart part;

    protected IItemHandler crafting;  // 20 input slots (5x4)
    protected SlotFakeCraftingMatrix[] craftingSlots;  // 20 slots
    protected OptionalSlotFake[] outputSlots;  // 12 slots
    protected SlotRestrictedInput patternSlotIN;
    protected SlotRestrictedInput patternSlotOUT;

    public ExtendedPatternContainer(final InventoryPlayer ip, final ExtendedPatternTerminalPart part) {
        super(ip, part);
        this.part = part;

        // Initialize slot arrays
        this.craftingSlots = new SlotFakeCraftingMatrix[20];  // 5x4 input grid
        this.outputSlots = new OptionalSlotFake[12];  // 12 output slots

        final IItemHandler patternInv = this.part.getInventoryByName("pattern");
        final IItemHandler output = this.part.getInventoryByName("output");
        this.crafting = this.part.getInventoryByName("crafting");

        // Add crafting input slots (5x4 grid)
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                this.addSlotToContainer(this.craftingSlots[x + y * 5] =
                    new SlotFakeCraftingMatrix(this.crafting, x + y * 5, 8 + x * 18, 25 + y * 18));
            }
        }

        // Add output slots (4x3 grid = 12 slots)
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                int index = x + y * 3;
                this.addSlotToContainer(this.outputSlots[index] =
                    new SlotPatternOutputs(output, this, index, 110 + x * 18, 25 + y * 18, 0, 0, 1));
                this.outputSlots[index].setRenderDisabled(false);
                this.outputSlots[index].setIIcon(-1);
            }
        }

        // Add pattern slots (blank in, encoded out)
        this.addSlotToContainer(this.patternSlotIN =
            new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.BLANK_PATTERN, patternInv, 0, 8, 115, this.getInventoryPlayer()));
        this.addSlotToContainer(this.patternSlotOUT =
            new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, patternInv, 1, 26, 115, this.getInventoryPlayer()));

        this.patternSlotOUT.setStackLimit(1);

        // Bind player inventory (positioned at bottom of GUI)
        this.bindPlayerInventory(ip, 0, 175);
    }

    public static ExtendedPatternContainer getServerGuiContainer(AEPartLocation side, EntityPlayer player, World world,
            int x, int y, int z) {
        ExtendedPatternTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side, ExtendedPatternTerminalPart.class);
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
    public IItemHandler getInventoryByName(String name) {
        if (name.equals("player")) {
            return new net.minecraftforge.items.wrapper.PlayerInvWrapper(this.getInventoryPlayer());
        }
        if (this.part != null) {
            return this.part.getInventoryByName(name);
        }
        return null;
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
    public ItemStack[] getViewCells() {
        return new ItemStack[0];
    }

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

        // if there is no input, this would be silly.
        if (in == null || out == null) {
            return;
        }

        // AE2's pattern rendering may have issues with >9 inputs due to 3x3 grid assumptions
        // Log a warning but allow encoding anyway since the pattern data itself is valid
        if (in.length > 9) {
            appeng.core.AELog.warn("Extended Pattern: Encoding pattern with %d inputs. AE2's pattern preview may not display correctly for patterns with >9 inputs.", in.length);
        }

        // first check the output slots, should either be null, or a pattern
        if (!output.isEmpty() && !this.isPattern(output)) {
            return;
        } // if nothing is there we should snag a new pattern.
        else if (output.isEmpty()) {
            output = this.patternSlotIN.getStack();
            if (output.isEmpty() || !this.isPattern(output)) {
                return; // no blanks.
            }

            // remove one, and clear the input slot.
            output.setCount(output.getCount() - 1);
            if (output.getCount() == 0) {
                this.patternSlotIN.putStack(ItemStack.EMPTY);
            }

            // add a new encoded pattern.
            Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
            if (maybePattern.isPresent()) {
                output = maybePattern.get();
            }
        }

        // encode the slot.
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
        encodedValue.setBoolean("crafting", false);  // Always processing mode
        encodedValue.setBoolean("substitute", false);  // No substitution in processing

        output.setTagCompound(encodedValue);

        // Verify the pattern can be parsed by AE2
        if (output.getItem() instanceof appeng.api.implementations.ICraftingPatternItem) {
            appeng.api.implementations.ICraftingPatternItem patternItem =
                (appeng.api.implementations.ICraftingPatternItem) output.getItem();
            appeng.api.networking.crafting.ICraftingPatternDetails details =
                patternItem.getPatternForItem(output, this.getPlayerInv().player.world);

            if (details == null) {
                appeng.core.AELog.error("Extended Pattern: Failed to create valid pattern - AE2 could not parse the encoded NBT");
                return;
            } else {
                appeng.core.AELog.info("Extended Pattern: Successfully encoded pattern with %d inputs and %d outputs",
                    details.getInputs().length, details.getOutputs().length);
            }
        }

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
