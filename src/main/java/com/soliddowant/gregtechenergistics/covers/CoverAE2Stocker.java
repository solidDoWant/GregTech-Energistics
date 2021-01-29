package com.soliddowant.gregtechenergistics.covers;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.networking.*;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.events.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.fluids.util.AEFluidStack;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import appeng.util.item.AEItemStack;
import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.ImmutableSet;
import com.soliddowant.gregtechenergistics.capability.impl.ItemHandlerListFixed;
import com.soliddowant.gregtechenergistics.gui.widgets.AE2PatternSlotWidget;
import com.soliddowant.gregtechenergistics.gui.widgets.AE2UpgradeSlotWidget;
import com.soliddowant.gregtechenergistics.helpers.CraftingTracker;
import com.soliddowant.gregtechenergistics.items.behaviors.FluidEncoderBehaviour;
import com.soliddowant.gregtechenergistics.render.Textures;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class CoverAE2Stocker extends CoverBehavior
        implements CoverWithUI, ITickable, IControllable, IGridBlock, IGridHost, IActionHost, ICraftingRequester {

    public final int tier;
    public final long maxItemsStocked;
    protected final IActionSource machineActionSource;
    protected final IItemStorageChannel itemChannel;
    protected final IFluidStorageChannel fluidChannel;
    protected IItemHandler machineItemInputHandler;
    protected IItemHandler machineItemExportHandler;
    protected IFluidHandler machineFluidInputHandler;
    protected IFluidHandler machineFluidExportHandler;
    protected boolean doesOtherAllowsWorking = true;
    protected long stockCount; // AE2 uses 'long's for most stack sizes, so we will too
    protected IGridNode node;
    protected IMEMonitor<IAEItemStack> attachedAE2ItemInventory;
    protected IMEMonitor<IAEFluidStack> attachedAE2FluidInventory;
    protected boolean shouldInsert = true;
    protected boolean isGridConnected = false;
    protected boolean useFluids = false;
    protected List<IAEFluidStack> patternInputFluids;
    protected List<IAEItemStack> patternInputItems;
    protected List<IAEFluidStack> remainingInputFluids;
    protected List<IAEItemStack> remainingInputItems;
    protected List<IAEFluidStack> patternOutputFluids;
    protected List<IAEItemStack> patternOutputItems;
    protected CoverStatus currentStatus;
    protected CraftingTracker craftingTracker;
    protected List<IAEItemStack> missingInputItems;
    protected AE2UpgradeSlotWidget upgradeSlotWidget;
    protected MultiblockControllerBase controller;
    protected AE2PatternSlotWidget patternSlotWidget;

    public CoverAE2Stocker(ICoverable coverable, EnumFacing attachedSide, int tier, long maxStockCount) {
        super(coverable, attachedSide);
        this.tier = tier;
        this.maxItemsStocked = maxStockCount;
        this.stockCount = this.maxItemsStocked;
        this.patternSlotWidget = new AE2PatternSlotWidget(coverable.getWorld());
        this.patternSlotWidget.setContentsChangedCallback(this::patternChangeCallback);
        this.upgradeSlotWidget = new AE2UpgradeSlotWidget(Upgrades.CRAFTING);
        this.upgradeSlotWidget.setContentsChangedCallback(wasInserted -> coverHolder.markDirty());

        if (Platform.isServer())
            node = AEApi.instance().grid().createGridNode(this);

        this.machineActionSource = new MachineSource(this);
        this.itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        this.fluidChannel = AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class);
        this.craftingTracker = new CraftingTracker(this, machineActionSource);

        // In the case of non-multiblocks this only needs to be done once and can be done on instantiation
        if (!isHolderMultiblock())
            registerSingleBlockHandlers();
    }

    public static boolean checkIfICoveraebleContainsCover(ICoverable coverHolder) {
        for (EnumFacing side : EnumFacing.VALUES)
            if (coverHolder.getCoverAtSide(side) instanceof CoverAE2Stocker)
                return true;

        return false;
    }

    protected void patternChangeCallback(boolean patternInserted) {
        updatePatternCaches();

        if (patternInserted)
            shouldInsert = true;
        else
            craftingTracker.cancelAll();

        coverHolder.markDirty();
    }

    @Override
    public boolean canAttach() {
        if (isHolderMultiblock()) {
            // Cover holder is a multiblock part
            // Unfortunately this has to check specifically for a MetaTileEntityMultiblockPart here
            // (rather than IMultiblockPart) because this needs to get the controller from the part.
            MetaTileEntityMultiblockPart castedHolder = (MetaTileEntityMultiblockPart) coverHolder;
            MultiblockControllerBase controller = castedHolder.getController();

            if (controller == null)
                return false;

            if (!controller.isStructureFormed())
                return false;

            for (IMultiblockPart part : controller.getMultiblockParts())
                if (part instanceof ICoverable)
                    if (checkIfICoveraebleContainsCover((ICoverable) part))
                        return false;

            // Check to make sure the multiblock has at least one input and export capability
            if (controller.getAbilities(MultiblockAbility.IMPORT_ITEMS).isEmpty() &&
                    controller.getAbilities(MultiblockAbility.IMPORT_FLUIDS).isEmpty())
                return false;

            //noinspection RedundantIfStatement // This is easier to read than the alternative
            if (controller.getAbilities(MultiblockAbility.EXPORT_ITEMS).isEmpty() &&
                    controller.getAbilities(MultiblockAbility.EXPORT_FLUIDS).isEmpty())
                return false;

            return true;
        } else {
            if (checkIfICoveraebleContainsCover(coverHolder))
                return false;

            return coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, attachedSide) != null ||
                    coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, attachedSide) != null;
        }
    }

    protected void updateMultiblockInformation() {
        if (!isHolderMultiblock())
            return;

        controller = ((MetaTileEntityMultiblockPart) coverHolder).getController();
        if (controller == null)
            return;

        if (controller instanceof RecipeMapMultiblockController)
            registerRecipeMapMultiblockControllerHandlers();
        else
            registerGenericMetaTileEntityMultiblockPartHandlers();
    }

    public boolean isHolderMultiblock() {
        return coverHolder instanceof MetaTileEntityMultiblockPart;
    }

    protected void registerSingleBlockHandlers() {
        machineItemInputHandler = coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                this.attachedSide);
        machineItemExportHandler = machineItemInputHandler;
        machineFluidInputHandler = coverHolder.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                this.attachedSide);
        machineFluidExportHandler = machineFluidInputHandler;
    }

    // Will remove these warnings after ItemHandlerList bug is fixed
    @SuppressWarnings({"CommentedOutCode", "DuplicatedCode"})
    protected void registerRecipeMapMultiblockControllerHandlers() {
        RecipeMapMultiblockController castedController = (RecipeMapMultiblockController) controller;

        // Temporary fix for ItemHandlerList bug
        List<IItemHandlerModifiable> itemInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
        if (!itemInputHandlers.isEmpty())
            machineItemInputHandler = new ItemHandlerListFixed(itemInputHandlers);

        List<IItemHandlerModifiable> itemExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_ITEMS);
        if (!itemExportHandlers.isEmpty())
            machineItemExportHandler = new ItemHandlerListFixed(itemExportHandlers);
//        machineItemInputHandler = castedController.getInputInventory();
//        machineItemExportHandler = castedController.getOutputInventory();

        machineFluidInputHandler = castedController.getInputFluidInventory();
        machineFluidExportHandler = castedController.getOutputFluidInventory();
    }

    // Will remove these warnings after ItemHandlerList bug is fixed
    @SuppressWarnings("DuplicatedCode")
    protected void registerGenericMetaTileEntityMultiblockPartHandlers() {
        List<IItemHandlerModifiable> itemInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_ITEMS);
        if (!itemInputHandlers.isEmpty())
            machineItemInputHandler = new ItemHandlerListFixed(itemInputHandlers);

        List<IItemHandlerModifiable> itemExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_ITEMS);
        if (!itemExportHandlers.isEmpty())
            machineItemExportHandler = new ItemHandlerListFixed(itemExportHandlers);

        List<IFluidTank> fluidInputHandlers = controller.getAbilities(MultiblockAbility.IMPORT_FLUIDS);
        if (!fluidInputHandlers.isEmpty())
            machineFluidInputHandler = new FluidTankList(false, fluidInputHandlers);

        List<IFluidTank> fluidExportHandlers = controller.getAbilities(MultiblockAbility.EXPORT_FLUIDS);
        if (!fluidExportHandlers.isEmpty())
            machineFluidExportHandler = new FluidTankList(false, fluidExportHandlers);
    }

    @Override
    public void onRemoved() {
        node.destroy();

        if (doesOtherAllowsWorking)
            getControllable().setWorkingEnabled(true);

        NonNullList<ItemStack> drops = NonNullList.create();
        MetaTileEntity.clearInventory(drops, patternSlotWidget.getSlotHandler());
        for (ItemStack itemStack : drops)
            Block.spawnAsEntity(coverHolder.getWorld(), coverHolder.getPos(), itemStack);
    }

    // The two renderCover methods provide backwards and forwards compatibility with GTCE versions
    public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline,
                            Cuboid6 plateBox) {
        Textures.STOCKER_OVERLAY.renderSided(attachedSide, plateBox, renderState, pipeline, translation);
    }

    public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline,
                            Cuboid6 plateBox, BlockRenderLayer layer) {
        renderCover(renderState, translation, pipeline, plateBox);
    }

    @Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (Platform.isServer())
            openUI((EntityPlayerMP) playerIn);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, T defaultValue) {
        if (capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE)
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        return defaultValue;
    }

    protected String getUITitle() {
        return "cover.stocker.title";
    }

    protected ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
        return builder.build(this, player);
    }

    @Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup primaryGroup = new WidgetGroup();

        // Title
        primaryGroup.addWidget(new LabelWidget(6, 5, getUITitle(), GTValues.VN[tier]));

        // Stock amount
        long incrementSize = maxItemsStocked / 100;
        String readableIncrementSize = ReadableNumberConverter.INSTANCE.toWideReadableForm(incrementSize);
        primaryGroup.addWidget(new ClickButtonWidget(10, 20, 40, 20,
                "-" + readableIncrementSize,
                data -> adjustStockCount(data.isShiftClick ? -10 * incrementSize : -incrementSize)));
        primaryGroup.addWidget(new ClickButtonWidget(126, 20, 40, 20,
                "+" + readableIncrementSize,
                data -> adjustStockCount(data.isShiftClick ? 10 * incrementSize : incrementSize)));
        primaryGroup.addWidget(new ImageWidget(50, 20, 76, 20, GuiTextures.DISPLAY));
        primaryGroup.addWidget(new SimpleTextWidget(88, 30, "cover.stocker.stock_count",
                0xFFFFFF, () -> ReadableNumberConverter.INSTANCE.toWideReadableForm(stockCount)));

        // Pattern
        primaryGroup.addWidget(new LabelWidget(32, 45 + 5, "cover.stocker.pattern.title"));
        this.patternSlotWidget.initUI(11, 45, primaryGroup::addWidget);

        // Upgrade
        primaryGroup.addWidget(new LabelWidget(32, 68, "cover.stocker.upgrade.label"));
        this.upgradeSlotWidget.initUI(11, 63, primaryGroup::addWidget);

        // Fluids
        primaryGroup.addWidget(new CycleButtonWidget(10, 86, 156, 20,
                this::shouldUseFluids, this::setUseFluids, "cover.stocker.fluids.disable",
                "cover.stocker.fluids.enable"));

        // Status
        primaryGroup.addWidget(new SimpleTextWidget(88, 120, "cover.stocker.status",
                () -> getCurrentStatus().toString()));

        ModularUI.Builder builder = ModularUI.extendedBuilder().widget(primaryGroup)
                .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 7, 216 - 84);
        return buildUI(builder, player);
    }

    protected boolean shouldUseFluids() {
        return useFluids;
    }

    protected void setUseFluids(boolean useFluids) {
        this.useFluids = useFluids;
        updatePatternCaches();
        shouldInsert = true;
    }

    protected void updatePatternCaches() {
        updatePatternInputItems();
        updatePatternInputFluids();
        updatePatternOutputItems();
        updatePatternOutputFluids();
    }

    protected void updatePatternInputFluids() {
        patternInputFluids = getFluidStacksFromItems(patternSlotWidget.getInputItems());
        remainingInputFluids = patternInputFluids == null ? null : copyAEStackList(patternInputFluids);
    }

    protected void updatePatternInputItems() {
        patternInputItems = getNonFluidStacksFromItems(patternSlotWidget.getInputItems());
        remainingInputItems = patternInputItems == null ? null : copyAEStackList(patternInputItems);
    }

    protected void updatePatternOutputFluids() {
        patternOutputFluids = getFluidStacksFromItems(patternSlotWidget.getOutputItems());
    }

    protected void updatePatternOutputItems() {
        patternOutputItems = getNonFluidStacksFromItems(patternSlotWidget.getOutputItems());
    }

    protected LinkedList<IAEFluidStack> getFluidStacksFromItems(IAEItemStack[] aeItemStacks) {
        if (!shouldUseFluids() || aeItemStacks == null)
            return null;

        // Items that can hold fluids can hold fluids can potentially hold multiple
        // tanks/types of fluids. This makes pattern fluid requirement particularly
        // tricky.
        LinkedList<IAEFluidStack> foundFluids = new LinkedList<>();
        for (IAEItemStack aeItemStack : aeItemStacks) {
            // getDefinition is used here as there shouldn't be any modifications of the item stack anywhere it's passed.
            ItemStack itemStack = aeItemStack.getDefinition();

            if (!FluidEncoderBehaviour.hasStackBehavior(itemStack))
                continue;

            FluidStack fluidStack = FluidEncoderBehaviour.getFluidStack(itemStack);
            if (fluidStack == null)
                continue;

            int fluidAmount = FluidEncoderBehaviour.getFluidAmount(itemStack);
            if (fluidAmount == 0)
                continue;

            Optional<IAEFluidStack> matchedFluid = foundFluids.stream()
                    .filter(foundFluid -> foundFluid.getFluidStack().isFluidEqual(fluidStack)).findAny();

            if (matchedFluid.isPresent())
                matchedFluid.get().incStackSize(fluidAmount);
            else {
                IAEFluidStack unMatchedFluid = AEFluidStack.fromFluidStack(fluidStack);
                unMatchedFluid.setStackSize(fluidAmount);
                foundFluids.add(unMatchedFluid);
            }
        }

        return foundFluids;
    }

    protected LinkedList<IAEItemStack> getNonFluidStacksFromItems(IAEItemStack[] aeItemStacks) {
        if (aeItemStacks == null)
            return null;

        Stream<IAEItemStack> items = Arrays.stream(aeItemStacks)
                .filter(item -> item.getStackSize() > 0);

        if (shouldUseFluids())
            items = items.filter(item -> !(FluidEncoderBehaviour.hasFluidStack(item.getDefinition())));

        LinkedList<IAEItemStack> reducedItems = new LinkedList<>();

        items.forEach(item -> {
            Optional<IAEItemStack> existingItem = reducedItems.stream()
                    .filter(reducedItem -> reducedItem.getDefinition().isItemEqual(item.getDefinition())).findAny();

            if (existingItem.isPresent())
                existingItem.get().incStackSize(item.getStackSize());
            else
                reducedItems.add(item);
        });

        return reducedItems;
    }

    protected void adjustStockCount(long amount) {
        setStockCount((long) MathHelper.clamp(stockCount + amount, 0, maxItemsStocked));
    }

    @Override
    public boolean isWorkingEnabled() {
        return getCurrentStatus() == CoverStatus.RUNNING;
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.doesOtherAllowsWorking = isActivationAllowed;
    }

    public CoverStatus getCurrentStatus() {
        return currentStatus;
    }

    protected CoverStatus getPartialStatus() {
        // Checks are in order of what's easy to check, then what (IMO) is likely to
        // fail first.
        if (!doesOtherAllowsWorking)
            return CoverStatus.OTHER_DISABLED;
        if (!isPatternAvailable())
            return CoverStatus.PATTERN_NOT_INSERTED;
        if (!isGridConnected())
            return CoverStatus.GRID_DISCONNECTED;
        if (isFullyStocked())
            return CoverStatus.FULLY_STOCKED;
        if (!areAllInputsAvailable())
            return CoverStatus.MISSING_INPUTS;
        if (isHolderMultiblock() && (controller == null || !controller.isStructureFormed()))
            return CoverStatus.INVALID_MULTIBLOCK;
        if (!isInputSpaceAvailable())
            return CoverStatus.MISSING_INPUT_SPACE;
        if (!isOutputSpaceAvailable())
            return CoverStatus.MISSING_OUTPUT_SPACE;

        return CoverStatus.RUNNING;
    }

    // This runs through all the checks that are possible, making it potentially
    // much slower than a partial check. This should be used for in-game
    // information, not checking if the machine should be running.
    @SuppressWarnings("unused")
    protected EnumSet<CoverStatus> getFullStatus() {
        EnumSet<CoverStatus> status = EnumSet.noneOf(CoverStatus.class);

        boolean patternAvailable = true; // Some checks rely on a pattern being available
        if (!doesOtherAllowsWorking)
            status.add(CoverStatus.OTHER_DISABLED);
        if (!patternSlotWidget.hasStack()) {
            status.add(CoverStatus.PATTERN_NOT_INSERTED);
            patternAvailable = false;
        }
        if (!isGridConnected)
            status.add(CoverStatus.GRID_DISCONNECTED);
        else if (patternAvailable) {
            // These checks all rely on a grid connection, as well as a pattern
            if (isFullyStocked())
                status.add(CoverStatus.FULLY_STOCKED);
            if (!areAllInputsAvailable())
                status.add(CoverStatus.MISSING_INPUTS);
            if (!isOutputSpaceAvailable())
                status.add(CoverStatus.MISSING_OUTPUT_SPACE);
        }
        if (patternAvailable && !isInputSpaceAvailable())
            status.add(CoverStatus.MISSING_INPUT_SPACE);

        if (status.isEmpty())
            return EnumSet.of(CoverStatus.RUNNING);

        return status;
    }

    protected boolean isPatternAvailable() {
        return patternSlotWidget.hasStack();
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("OtherAllowsWorking", doesOtherAllowsWorking);
        tagCompound.setLong("StockCount", stockCount);
        tagCompound.setTag("Pattern", patternSlotWidget.serializeNBT());
        tagCompound.setInteger("Status", currentStatus.ordinal());
        tagCompound.setBoolean("ShouldInsert", shouldInsert);
        tagCompound.setBoolean("UseFluids", useFluids);
        tagCompound.setTag("UpgradeSlot", upgradeSlotWidget.serializeNBT());
        tagCompound.setTag("CraftingTag", craftingTracker.serializeNBT());
        tagCompound.setTag("RemainingItems", serializeRemainingInputItems());
        tagCompound.setTag("RemainingFluids", serializeRemainingInputFluids());
        node.saveToNBT("node", tagCompound);
    }

    protected NBTTagCompound serializeRemainingInputItems() {
        NBTTagCompound remainingItems = new NBTTagCompound();
        if (remainingInputItems != null) {
            int i = 0;
            for (IAEItemStack remainingInputItem : remainingInputItems) {
                NBTTagCompound remainingItem = new NBTTagCompound();
                remainingInputItem.writeToNBT(remainingItem);
                remainingItems.setTag(String.valueOf(i++), remainingItem);
            }
        }

        return remainingItems;
    }

    protected NBTTagCompound serializeRemainingInputFluids() {
        NBTTagCompound remainingFluids = new NBTTagCompound();
        if (remainingInputFluids != null) {
            int i = 0;
            for (IAEFluidStack remainingInputFluid : remainingInputFluids) {
                NBTTagCompound remainingFluid = new NBTTagCompound();
                remainingInputFluid.writeToNBT(remainingFluid);
                remainingFluids.setTag(String.valueOf(i++), remainingFluid);
            }
        }

        return remainingFluids;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);

        if (tagCompound.hasKey("UseFluids"))
            this.useFluids = tagCompound.getBoolean("UseFluids");

        if (tagCompound.hasKey("Pattern")) {
            NBTTagCompound patternContainer = tagCompound.getCompoundTag("Pattern");
            this.patternSlotWidget.deserializeNBT(patternContainer);
            updatePatternCaches();
        }

        if (tagCompound.hasKey("OtherAllowsWorking"))
            this.doesOtherAllowsWorking = tagCompound.getBoolean("OtherAllowsWorking");

        if (tagCompound.hasKey("StockCount"))
            this.stockCount = tagCompound.getLong("StockCount");

        if (tagCompound.hasKey("node"))
            this.node.loadFromNBT("node", tagCompound);

        if (tagCompound.hasKey("ShouldInsert"))
            this.shouldInsert = tagCompound.getBoolean("ShouldInsert");

        if (tagCompound.hasKey("RemainingItems")) {
            NBTTagCompound remainingItemsTag = tagCompound.getCompoundTag("RemainingItems");
            this.remainingInputItems = remainingItemsTag.getKeySet().stream()
                    .map(key -> AEItemStack.fromNBT(remainingItemsTag.getCompoundTag(key)))
                    .collect(Collectors.toList());
        } else
            this.remainingInputItems = new LinkedList<>();

        if (tagCompound.hasKey("RemainingFluids")) {
            NBTTagCompound remainingFluidsTag = tagCompound.getCompoundTag("RemainingFluids");
            this.remainingInputFluids = remainingFluidsTag.getKeySet().stream()
                    .map(key -> AEFluidStack.fromNBT(remainingFluidsTag.getCompoundTag(key)))
                    .collect(Collectors.toList());
        } else
            remainingInputFluids = new LinkedList<>();

        if (tagCompound.hasKey("Status"))
            currentStatus = CoverStatus.values()[tagCompound.getInteger("Status")];

        if (tagCompound.hasKey("UpgradeSlot"))
            this.upgradeSlotWidget.deserializeNBT(tagCompound.getCompoundTag("UpgradeSlot"));

        if (tagCompound.hasKey("CraftingTracker"))
            craftingTracker.deserializeNBT(tagCompound.getCompoundTag("CraftingTracker"));
    }

    @Override
    public void update() {
        // Only update on every 5th tick
        long timer = coverHolder.getTimer();
        if (timer % 5 != 0)
            return;

        // Covers cannot currently tell when a neighboring block changes. This is a
        // workaround so that when a new cable is placed the cover can connect to it.
        // This is currently the highest cost operation in the update loop.
        if (coverHolder.getWorld() != null && !isGridConnected())
            node.updateState();

        // If the cover holder is/should be a part of a multiblock and the multiblock changed, this will deal with the
        // new/changed handlers.
        updateMultiblockInformation();

        // Upon checking this we should know several things and don't have to check
        // them:
        // * There is a pattern available
        // * A the machine is attached to the grid
        // * The stock count of all pattern output items have not been met
        // * All input items are available for another pattern's worth of crafts
        // * There is at least some space (though may not be all) for another pattern's
        // worth of inputs
        // * There is at least some space (though may not be all) for another pattern's
        // worth of outputs
        // Therefore for only space available for insertion and extraction needs to be
        // checked
        currentStatus = getPartialStatus();
        if (!isWorkingEnabled()) {
            // If the cover holder is/should be a part of a multiblock and the current status is missing input or output
            // space, then it's likely that the multiblock needs to be reformed with new hatches/busses. If working is
            // disabled then the multiblock cannot reform, resulting in the status to always return the initial missing
            // input/output space. While working is enabled, the method is exited afterwords as not all checks have been
            // passed.
            //noinspection RedundantIfStatement // This is easier to read than the alternative
            if (!isHolderMultiblock() ||
                    (currentStatus != CoverStatus.MISSING_INPUT_SPACE && currentStatus != CoverStatus.MISSING_OUTPUT_SPACE))
                setWorkingStatus(false);
            else
                setWorkingStatus(true);

            // Order missing items
            if (currentStatus == CoverStatus.MISSING_INPUTS && upgradeSlotWidget.hasStack())
                orderMissingItems();

            return;
        }
        setWorkingStatus(true);

        // Structure not formed, so don't do anything.
        if (isHolderMultiblock() && controller == null)
            return;

        // Do the insert/extract operations.
        if (shouldInsert)
            shouldInsert = !doInsert();

        if (!shouldInsert)
            shouldInsert = doExtract();
    }

    // Returns true if at least one item or fluid was extracted, and there was no missing output space.
    // Extract produced items
    // This will extract all items, not just what's in the pattern.
    // Good for things like electrolyzing clay dust where there are 4 outputs but
    // the pattern only supports 3.
    // This may extract up to one update()'s worth of outputs more than the targeted
    // amount.
    // This is something that could be fixed/changed but IMO it's not worth the
    // extra per tick cost.
    protected boolean doExtract() {
        boolean missingOutputSpace = false;
        boolean hasRemovedSomething = false;
        if (machineItemExportHandler != null)
            for (int slot = 0; slot < (machineItemExportHandler == null ? 0 : machineItemExportHandler.getSlots()); slot++) {
                ItemStack slotStack = machineItemExportHandler.getStackInSlot(slot);
                if (slotStack.isEmpty())
                    continue;

                int availableCount = slotStack.getCount();

                // Test to see how many items can be removed. Some slots (i.e. inputs) might not
                // be removable.
                int amountAvailableToRemove = machineItemExportHandler.extractItem(slot, availableCount, true).getCount();

                if (amountAvailableToRemove == 0)
                    continue;

                hasRemovedSomething = true;

                // Insert into AE2 grid and extract the actual amount removed from the machine
                // inventory
                ItemStack insertionStack = slotStack.copy();
                insertionStack.setCount(amountAvailableToRemove);
                IAEItemStack remainingItemStack = attachedAE2ItemInventory
                        .injectItems(AEItemStack.fromItemStack(slotStack), Actionable.MODULATE, machineActionSource);
                long missingSpace = remainingItemStack == null ? 0 : remainingItemStack.getStackSize();
                int insertedAmount = (int) (availableCount - missingSpace);

                machineItemExportHandler.extractItem(slot, insertedAmount, false);

                if (missingSpace > 0)
                    missingOutputSpace = true;
            }

        if (shouldUseFluids() && machineFluidExportHandler != null)
            for (IFluidTankProperties tankProperties : machineFluidExportHandler.getTankProperties()) {
                if (!tankProperties.canDrain())
                    continue;

                // Test to see how much of what fluid can be removed
                FluidStack availableFluidStack = machineFluidExportHandler.drain(tankProperties.getContents(), false);
                if (availableFluidStack == null)
                    continue;

                int availableFluidAmount = availableFluidStack.amount;
                if (availableFluidAmount == 0)
                    continue;

                hasRemovedSomething = true;

                // Attempt to add the fluid to AE2
                IAEFluidStack remainingItemStack = attachedAE2FluidInventory.injectItems(
                        AEFluidStack.fromFluidStack(availableFluidStack), Actionable.MODULATE, machineActionSource);

                // Calculate how much was actually inserted
                long missingSpace = remainingItemStack == null ? 0 : remainingItemStack.getStackSize();

                // Remove the amount actually inserted
                availableFluidStack.amount = (int) (availableFluidAmount - missingSpace);
                machineFluidExportHandler.drain(availableFluidStack, true);

                if (missingSpace > 0)
                    missingOutputSpace = true;
            }

        // Update the machine state to show that all items have been extracted
        return !missingOutputSpace && hasRemovedSomething;
    }

    // Returns true if all items were inserted.
    // This will track how much of the recipe is actually inserted so that multiple sets
    // of the same recipe aren't inserted when input space runs out.
    protected boolean doInsert() {
        LinkedList<IAEItemStack> newRemainingInputItems = insertItems();
        LinkedList<IAEFluidStack> newRemainingInputFluids = shouldUseFluids() ? insertFluids() : new LinkedList<>();

        if (newRemainingInputItems.isEmpty() && newRemainingInputFluids.isEmpty()) {
            remainingInputItems = copyAEStackList(patternInputItems);
            remainingInputFluids = copyAEStackList(patternInputFluids);
            return true;
        }

        remainingInputItems = newRemainingInputItems;
        remainingInputFluids = newRemainingInputFluids;
        return false;
    }

    protected LinkedList<IAEFluidStack> insertFluids() {
        LinkedList<IAEFluidStack> newRemainingInputFluids = new LinkedList<>();
        for (IAEFluidStack inputFluid : getRemainingInputFluids()) {
            FluidStack insertingStack = inputFluid.getFluidStack();
            final int insertedCount = machineFluidInputHandler.fill(insertingStack, true);

            // Create a stack to extract from AE2 grid that matches only what was inserted.
            IAEFluidStack extractionStack;
            int remainingCount = insertingStack.amount - insertedCount;
            if (remainingCount != 0) {
                // Haven't inserted a full pattern, likely due to missing input space.
                // Save whatever is left over for the next round.
                extractionStack = AEFluidStack.fromFluidStack(insertingStack);
                extractionStack.setStackSize(remainingCount);
                newRemainingInputFluids.add(extractionStack);

                // Can't insert into this slot for whatever reason. No need to try to extract 0 fluid from grid
                if (insertedCount == 0)
                    continue;
            } else {
                // No need to make a new item stack if they're identical
                extractionStack = inputFluid;
            }

            attachedAE2FluidInventory.extractItems(extractionStack, Actionable.MODULATE, machineActionSource);
        }

        return newRemainingInputFluids;
    }

    protected LinkedList<IAEItemStack>  insertItems() {
        LinkedList<IAEItemStack> newRemainingInputItems = new LinkedList<>();
        for (IAEItemStack inputItem : getRemainingInputItems()) {
            IAEItemStack remainingStack = insertItem(inputItem);
            if(remainingStack != null)
                newRemainingInputItems.add(remainingStack);
        }

        return newRemainingInputItems;
    }

    protected IAEItemStack insertItem(IAEItemStack inputItem) {
        ItemStack insertingStack = inputItem.createItemStack();
        for (int slot = 0; slot < (machineItemInputHandler == null ? 0 : machineItemInputHandler.getSlots()); slot++) {
            final ItemStack remainingStack = machineItemInputHandler.insertItem(slot, insertingStack, false);
            final int insertedCount = insertingStack.getCount() - remainingStack.getCount();

            // Can't insert into this slot for whatever reason
            if (insertedCount == 0)
                continue;

            // Extract only what was inserted
            IAEItemStack extractionStack;
            if (insertedCount != inputItem.getStackSize()) {
                extractionStack = AEItemStack.fromItemStack(insertingStack);
                // extractionStack is only null if insertingStack.isEmpty(), which is checked elsewhere
                //noinspection ConstantConditions
                extractionStack.setStackSize(insertedCount);
            } else {
                // No need to make a new item stack if they're identical
                extractionStack = inputItem;
            }
            attachedAE2ItemInventory.extractItems(extractionStack, Actionable.MODULATE, machineActionSource);

            // Update the next inserting stack with whatever is left
            insertingStack = remainingStack;

            if (remainingStack.isEmpty())
                break;
        }

        // insertingStack is now remainingStack. Add the remainder to the remaining items list
        return insertingStack.isEmpty() ? null : AEItemStack.fromItemStack(insertingStack);
    }

    protected void orderMissingItems() {
        IGrid grid = node.getGrid();
        ICraftingGrid craftingGrid = grid.getCache(ICraftingGrid.class);
        for (IAEItemStack missingInputItem : missingInputItems)
            craftingTracker.handleCrafting(missingInputItem, craftingGrid, coverHolder.getWorld(), grid);
    }

    public void setWorkingStatus(boolean shouldWork) {
        IControllable machine = getControllable();
        if (machine != null)
            machine.setWorkingEnabled(shouldWork);
    }

    @MENetworkEventSubscribe
    public void channelUpdated(final MENetworkChannelChanged c) {
        updateGridConnectionState();
    }

    @MENetworkEventSubscribe
    public void controllerChanged(final MENetworkControllerChange c) {
        updateGridConnectionState();
    }

    @MENetworkEventSubscribe
    public void bootingStatusUpdated(final MENetworkBootingStatusChange c) {
        updateGridConnectionState();
    }

    @MENetworkEventSubscribe
    public void channelsUpdated(final MENetworkChannelsChanged c) {
        updateGridConnectionState();
    }

    @MENetworkEventSubscribe
    public void powerChanged(final MENetworkPowerStatusChange c) {
        updateGridConnectionState();
    }

    protected void updateGridConnectionState() {
        if (node.isActive())
            connectToGrid();
        else
            disconnectFromGrid();
    }

    protected void connectToGrid() {
        IStorageGrid storageGrid = node.getGrid().getCache(IStorageGrid.class);

        attachedAE2ItemInventory = storageGrid.getInventory(itemChannel);
        attachedAE2FluidInventory = storageGrid.getInventory(fluidChannel);

        if (attachedAE2ItemInventory == null || attachedAE2FluidInventory == null) {
            disconnectFromGrid();
            return;
        }

        isGridConnected = true;
    }

    protected void disconnectFromGrid() {
        attachedAE2ItemInventory = null;
        attachedAE2FluidInventory = null;
        isGridConnected = false;
    }

    public boolean isGridConnected() {
        return isGridConnected;
    }

    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return getActionableNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        coverHolder.getWorld().destroyBlock(coverHolder.getPos(), true);
    }

    @Override
    public double getIdlePowerUsage() {
        return 4;
    }

    @Nonnull
    @Override
    public EnumSet<GridFlags> getFlags() {
        return EnumSet.of(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public boolean isWorldAccessible() {
        return true;
    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(coverHolder.getWorld(), coverHolder.getPos());
    }

    @Nonnull
    @Override
    public AEColor getGridColor() {
        return AEColor.TRANSPARENT;
    }

    @Override
    public void onGridNotification(@Nonnull GridNotification notification) {
    }

    @Override
    public void setNetworkStatus(IGrid grid, int channelsInUse) {
    }

    @Nonnull
    @Override
    public EnumSet<EnumFacing> getConnectableSides() {
        return EnumSet.of(this.attachedSide);
    }

    @Nonnull
    @Override
    public IGridHost getMachine() {
        return this;
    }

    @Override
    public void gridChanged() {
    }

    @Nonnull
    @Override
    public ItemStack getMachineRepresentation() {
        if (isHolderMultiblock() && controller != null)
            return controller.getStackForm();

        return coverHolder.getStackForm();
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return node;
    }

    /// Check if output items have been fully stocked.
    /// Returns true if there are at least stockCount items in the AE2 grid for each
    /// output.
    protected boolean isFullyStocked() {
        IItemList<IAEItemStack> storedItems = attachedAE2ItemInventory.getStorageList();
        IItemList<IAEFluidStack> storedFluids = attachedAE2FluidInventory.getStorageList();

        for (IAEItemStack outputItem : patternOutputItems)
            if (getStorageCount(outputItem, storedItems) < stockCount)
                return false;

        if (shouldUseFluids())
            for (IAEFluidStack outputFluid : patternOutputFluids)
                if (getStorageCount(outputFluid, storedFluids) < stockCount)
                    return false;

        return true;
    }

    protected <T extends IAEStack<T>> long getStorageCount(T item, IItemList<T> storedItems) {
        T testStack = item.copy();
        testStack.setStackSize(stockCount);
        // This behavior is very similar to a level emitter.
        // The idea is to check for stored items, not extractable items.
        // I _think_ this is also more efficient than attempted a simulated extraction.
        T availableItems = storedItems.findPrecise(testStack);
        return availableItems == null ? 0 : availableItems.getStackSize();
    }

    public LinkedList<Long> getOutputAvailableCounts() {
        IItemList<IAEItemStack> storedItems = attachedAE2ItemInventory.getStorageList();
        IItemList<IAEFluidStack> storedFluids = attachedAE2FluidInventory.getStorageList();

        LinkedList<Long> outputAvailableCounts = new LinkedList<>();

        for (IAEItemStack outputItem : patternOutputItems)
            outputAvailableCounts.add(getStorageCount(outputItem, storedItems));

        if (shouldUseFluids())
            for (IAEFluidStack outputFluid : patternOutputFluids)
                outputAvailableCounts.add(getStorageCount(outputFluid, storedFluids));

        return outputAvailableCounts;
    }

    public long getOutputLeastAvailableCount() {
        return (isGridConnected() && isPatternAvailable()) ?
                getOutputAvailableCounts().stream().reduce((x, y) -> x < y ? x : y).orElse(0L) : 0;
    }

    /// Check if all items in a pattern are available to insert.
    /// If there is not enough for another craft in AE2, stop working
    /// There is a corner case here where there are no new items available but the
    /// machine is still processing.
    /// In this case, the machine will stop after consuming the input. AFAIK there
    /// isn't a way to tell
    /// if the machine is still running from a cover.
    protected boolean areAllInputsAvailable() {
        if (!upgradeSlotWidget.hasStack()) {
            for (IAEItemStack inputItem : getRemainingInputItems())
                if (!isItemAvailableForExtraction(inputItem))
                    return false;
        } else {
            missingInputItems = new LinkedList<>();
            for (IAEItemStack inputItem : getRemainingInputItems()) {
                long availableCount = getItemAvailableCount(inputItem);
                long requiredCount = inputItem.getStackSize();

                if (availableCount >= requiredCount)
                    continue;

                IAEItemStack missingItemStack = inputItem.copy();
                missingItemStack.setStackSize(requiredCount - availableCount);
                missingInputItems.add(missingItemStack);
            }

            if (!missingInputItems.isEmpty())
                return false;
        }

        if (shouldUseFluids())
            for (IAEFluidStack inputFluid : getRemainingInputFluids())
                if (!isFluidAvailableForExtraction(inputFluid))
                    return false;

        return true;
    }

    protected boolean isFluidAvailableForExtraction(IAEFluidStack fluid) {
        IAEFluidStack availableFluid = attachedAE2FluidInventory.extractItems(fluid, Actionable.SIMULATE,
                machineActionSource);

        return availableFluid != null && availableFluid.getStackSize() == fluid.getStackSize();
    }

    protected boolean isItemAvailableForExtraction(IAEItemStack items) {
        return getItemAvailableCount(items) == items.getStackSize();
    }

    protected long getItemAvailableCount(IAEItemStack item) {
        IAEItemStack availableItems = attachedAE2ItemInventory.extractItems(item, Actionable.SIMULATE,
                machineActionSource);

        return availableItems == null ? 0 : availableItems.getStackSize();
    }

    /// Check if there is room to insert another batch of inputs
    /// There is a corner case here that's not checked: If there is one empty slot
    /// but two non-compatible item stacks
    /// need to be inserted into it, this will pass despite being a conflict.
    protected boolean isInputSpaceAvailable() {
        // If items need to be inserted but the machine can't handle items, fail
        if (!patternInputItems.isEmpty() && machineItemInputHandler == null)
            return false;

        if (isMissingItemInputSpace())
            return false;

        //noinspection RedundantIfStatement
        if (shouldUseFluids() && isMissingFluidInputSpace())
            return false;

        return true;
    }

    protected boolean isMissingFluidInputSpace() {
        // If fluids need to be inserted but the machine can't handle fluids, fail
        if (!patternInputFluids.isEmpty() && machineFluidInputHandler == null)
            return true;

        for (IAEFluidStack aeFluidStack : patternInputFluids) {
            FluidStack fluidStack = aeFluidStack.getFluidStack();
            // If a full set of fluid inputs couldn't be inserted, fail
            if (fluidStack.amount != machineFluidInputHandler.fill(fluidStack, false))
                return true;
        }

        return false;
    }

    protected boolean isMissingItemInputSpace() {
        for (IAEItemStack iaeItemStack : patternInputItems) {
            int targetInsertingCount = (int) iaeItemStack.getStackSize();
            int neededSpace = targetInsertingCount;
            for (int slot = 0; slot < machineItemInputHandler.getSlots(); slot++) {
                int missingSpace = machineItemInputHandler.insertItem(slot, iaeItemStack.createItemStack(), true).getCount();
                int spaceAvailableToInsert = targetInsertingCount - missingSpace;
                neededSpace -= spaceAvailableToInsert;

                // No need to keep checking slots if there is enough space in the previously checked slots
                if (neededSpace <= 0)
                    break;
            }

            if (neededSpace > 0)
                return true;
        }

        return false;
    }

    /// Check if there is room to store another batch of outputs
    /// If there is no more room to insert, stop working
    /// There is a corner case here with multiple output stacks the first inserted
    /// stack will fill up all remaining space
    protected boolean isOutputSpaceAvailable() {
        if (isMissingItemOutputSpace())
            return false;

        //noinspection RedundantIfStatement
        if (shouldUseFluids() && isMissingFluidOutputSpace())
            return false;

        return true;
    }

    protected boolean isMissingFluidOutputSpace() {
        for (IAEFluidStack outputFluid : patternOutputFluids) {
            IAEFluidStack remainingFluid = attachedAE2FluidInventory.injectItems(outputFluid, Actionable.SIMULATE,
                    machineActionSource);

            if (remainingFluid != null && remainingFluid.getStackSize() > 0)
                return true;
        }
        return false;
    }

    protected boolean isMissingItemOutputSpace() {
        for (IAEItemStack outputItem : patternOutputItems) {
            IAEItemStack remainingItems = attachedAE2ItemInventory.injectItems(outputItem, Actionable.SIMULATE,
                    machineActionSource);

            if (remainingItems != null && remainingItems.getStackSize() > 0)
                return true;
        }

        return false;
    }

    protected IControllable getControllable() {
        ICoverable capabilityProvider = controller == null ? coverHolder : controller;
        return capabilityProvider.getCapability(GregtechTileCapabilities.CAPABILITY_CONTROLLABLE, null);
    }

    public String getHolderName() {
        if (isHolderMultiblock() && controller != null)
            return controller.getStackForm().getUnlocalizedName();

        return this.coverHolder.getStackForm().getUnlocalizedName();
    }

    public IItemHandler getPatternHandler() {
        return this.patternSlotWidget.getSlotHandler();
    }

    public long getStockCount() {
        return stockCount;
    }

    protected void setStockCount(long itemsStocked) {
        this.stockCount = itemsStocked;
        coverHolder.markDirty();
    }

    protected List<IAEItemStack> getRemainingInputItems() {
        return remainingInputItems;
    }

    protected List<IAEFluidStack> getRemainingInputFluids() {
        return remainingInputFluids;
    }

    protected <T extends IAEStack<T>> List<T> copyAEStackList(List<T> sourceList) {
        return sourceList == null ?
                null : sourceList.stream().map(T::copy).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return craftingTracker.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack items, Actionable mode) {
        return items;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        craftingTracker.jobStateChange(link);
    }
}