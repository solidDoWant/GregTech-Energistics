package com.example.examplemod;

import javax.annotation.Nonnull;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.GTValues;
import gregtech.api.capability.GregtechTileCapabilities;
import gregtech.api.capability.IControllable;
import gregtech.api.capability.impl.ItemHandlerDelegate;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.render.Textures;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class CoverAE2Stocker extends CoverBehavior implements CoverWithUI, ITickable, IControllable {
	public final int tier;
	public final int maxItemTransferRate;
	protected int transferRate;
	protected int itemsLeftToTransferLastSecond;
	protected boolean isWorkingAllowed = true;
	protected final PatternContainer patternContainer;
	private CoverableItemHandlerWrapper itemHandlerWrapper;

	public CoverAE2Stocker(ICoverable coverHolder, EnumFacing attachedSide, int tier, int maxItemsPerSecond) {
		super(coverHolder, attachedSide);
		this.tier = tier;
		this.maxItemTransferRate = maxItemsPerSecond;
		this.transferRate = maxItemTransferRate;
		this.itemsLeftToTransferLastSecond = transferRate;
		this.patternContainer = new PatternContainer(this);
	}

	@Override
	public boolean canAttach() {
		return coverHolder.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, attachedSide) != null;
	}
	
	@Override
    public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
        if (!coverHolder.getWorld().isRemote) {
            openUI((EntityPlayerMP) playerIn);
        }
        return EnumActionResult.SUCCESS;
    }
	
	@Override
    public <T> T getCapability(Capability<T> capability, T defaultValue) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IItemHandler delegate = (IItemHandler) defaultValue;
            if (itemHandlerWrapper == null || itemHandlerWrapper.delegate != delegate) {
                this.itemHandlerWrapper = new CoverableItemHandlerWrapper(delegate);
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemHandlerWrapper);
        }
		
        if(capability == GregtechTileCapabilities.CAPABILITY_CONTROLLABLE) {
            return GregtechTileCapabilities.CAPABILITY_CONTROLLABLE.cast(this);
        }
        
        return defaultValue;
    }
	
	@Override
    public ModularUI createUI(EntityPlayer player) {
        WidgetGroup primaryGroup = new WidgetGroup();
        primaryGroup.addWidget(new LabelWidget(6, 5, getUITitle(), GTValues.VN[tier]));
//        primaryGroup.addWidget(new ClickButtonWidget(10, 20, 20, 20, "-10", data -> adjustTransferRate(data.isShiftClick ? -100 : -10)));
//        primaryGroup.addWidget(new ClickButtonWidget(146, 20, 20, 20, "+10", data -> adjustTransferRate(data.isShiftClick ? +100 : +10)));
//        primaryGroup.addWidget(new ClickButtonWidget(30, 20, 20, 20, "-1", data -> adjustTransferRate(data.isShiftClick ? -5 : -1)));
//        primaryGroup.addWidget(new ClickButtonWidget(126, 20, 20, 20, "+1", data -> adjustTransferRate(data.isShiftClick ? +5 : +1)));
//        primaryGroup.addWidget(new ImageWidget(50, 20, 76, 20, GuiTextures.DISPLAY));
//        primaryGroup.addWidget(new SimpleTextWidget(88, 30, "cover.conveyor.transfer_rate", 0xFFFFFF, () -> Integer.toString(transferRate)));
//
//        primaryGroup.addWidget(new CycleButtonWidget(10, 45, 75, 20,
//            GTUtility.mapToString(ConveyorMode.values(), it -> it.localeName),
//            () -> conveyorMode.ordinal(), newMode -> setConveyorMode(ConveyorMode.values()[newMode])));
//
        this.patternContainer.initUI(70, primaryGroup::addWidget);

        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND_EXTENDED, 176, 170 + 82)
            .widget(primaryGroup)
            .bindPlayerInventory(player.inventory, GuiTextures.SLOT, 8, 170);
        return buildUI(builder, player);
    }
	
	protected String getUITitle() {
        return "cover.stocker.title";
    }
	
	protected ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
        return builder.build(this, player);
    }

	@Override
	public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline, Cuboid6 plateBox) {
		Textures.SHUTTER.renderSided(attachedSide, plateBox, renderState, pipeline, translation);		
	}
	
	@Override
	public void update() {
		
	}
	
	@Override
    public boolean isWorkingEnabled() {
        return isWorkingAllowed;
    }

    @Override
    public void setWorkingEnabled(boolean isActivationAllowed) {
        this.isWorkingAllowed = isActivationAllowed;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("WorkingAllowed", isWorkingAllowed);
        tagCompound.setTag("Pattern", this.patternContainer.serializeNBT());
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        if(tagCompound.hasKey("Pattern")) {
            this.patternContainer.deserializeNBT(tagCompound);
        }
        if(tagCompound.hasKey("WorkingAllowed")) {
            this.isWorkingAllowed = tagCompound.getBoolean("WorkingAllowed");
        }
    }
    
    private class CoverableItemHandlerWrapper extends ItemHandlerDelegate {
        public CoverableItemHandlerWrapper(IItemHandler delegate) {
            super(delegate);
        }
    }
}
