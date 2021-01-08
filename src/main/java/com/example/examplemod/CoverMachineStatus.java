package com.example.examplemod;

import com.example.examplemod.CoverAE2Stocker.CoverStatus;

import codechicken.lib.raytracer.CuboidRayTraceResult;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Matrix4;
import gregtech.api.cover.CoverBehavior;
import gregtech.api.cover.CoverWithUI;
import gregtech.api.cover.ICoverable;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.CycleButtonWidget;
import gregtech.api.gui.widgets.LabelWidget;
import gregtech.api.gui.widgets.WidgetGroup;
import gregtech.api.render.Textures;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;

public class CoverMachineStatus extends CoverBehavior implements ITickable, CoverWithUI {
	private boolean isInverted;
	private CoverStatus checkStatus = CoverStatus.RUNNING;
	public boolean isOutputHigh;

	public CoverMachineStatus(ICoverable coverHolder, EnumFacing attachedSide) {
		super(coverHolder, attachedSide);
	}

	@Override
	public boolean canAttach() {
		return true;
	}

	public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline,
			Cuboid6 plateBox) {
		Textures.SHUTTER.renderSided(attachedSide, plateBox, renderState, pipeline, translation);
	}

	@Override
	public EnumActionResult onScrewdriverClick(EntityPlayer playerIn, EnumHand hand, CuboidRayTraceResult hitResult) {
		if (!coverHolder.getWorld().isRemote) {
			openUI((EntityPlayerMP) playerIn);
		}
		return EnumActionResult.SUCCESS;
	}

	@Override
	public boolean canConnectRedstone() {
		return true;
	}

	@Override
	public void update() {
		// Only update on every 10th tick
		long timer = coverHolder.getTimer();
		if (timer % 5 != 0) {
			return;
		}

		CoverAE2Stocker stockerCover = getAttachedStockerCover();

		if (stockerCover == null) {
			return;
		}

		boolean isStateMatched = stockerCover.getPartialStatus() == getCheckStatus();
		boolean shouldOutputBeHigh = isInverted ? !isStateMatched : isStateMatched;
		if (isOutputHigh != shouldOutputBeHigh) {
			isOutputHigh = shouldOutputBeHigh;
			setRedstoneSignalOutput(isOutputHigh ? 15 : 0);
			coverHolder.getWorld().notifyNeighborsOfStateChange(coverHolder.getPos(),
					coverHolder.getWorld().getTileEntity(coverHolder.getPos()).getBlockType(), shouldOutputBeHigh);
			markAsDirty();
		}
	}

	protected CoverAE2Stocker getAttachedStockerCover() {
		for (EnumFacing side : EnumFacing.VALUES) {
			CoverBehavior sideCover = coverHolder.getCoverAtSide(side);
			if (sideCover instanceof CoverAE2Stocker) {
				return (CoverAE2Stocker) sideCover;
			}
		}

		return null;
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		tagCompound.setBoolean("IsOuptutHigh", isOutputHigh);
		tagCompound.setBoolean("IsInverted", isInverted);
		tagCompound.setInteger("CheckStatus", checkStatus.ordinal());
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if (tagCompound.hasKey("IsOutputHigh")) {
			this.isOutputHigh = tagCompound.getBoolean("IsOutputHigh");
		}

		if (tagCompound.hasKey("IsInverted")) {
			this.isInverted = tagCompound.getBoolean("IsInverted");
		}

		if (tagCompound.hasKey("CheckStatus")) {
			this.checkStatus = CoverStatus.values()[tagCompound.getInteger("CheckStatus")];
		}
	}

	@Override
	public ModularUI createUI(EntityPlayer player) {
		WidgetGroup primaryGroup = new WidgetGroup();

		// Title
		primaryGroup.addWidget(new LabelWidget(6, 5, getUITitle()));

		// Status to check for
		primaryGroup.addWidget(
				new CycleButtonWidget(10, 45, 156, 20, CoverStatus.class, this::getCheckStatus, this::setCheckStatus));

		// Invert output setting
		primaryGroup.addWidget(new CycleButtonWidget(10, 70, 156, 20, this::isInverted, this::setInverted,
				"cover.machine.status.normal", "cover.machine.status.inverted"));

		ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, 166).widget(primaryGroup);
		return buildUI(builder, player);
	}

	public boolean isInverted() {
		return isInverted;
	}

	public void setInverted(boolean inverted) {
		isInverted = inverted;
		markAsDirty();
	}

	public CoverStatus getCheckStatus() {
		return checkStatus;
	}

	public void setCheckStatus(CoverStatus status) {
		this.checkStatus = status;
		markAsDirty();
	}

	protected String getUITitle() {
		return "cover.machine.status.title";
	}

	protected ModularUI buildUI(ModularUI.Builder builder, EntityPlayer player) {
		return builder.build(this, player);
	}

	public void renderCover(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline,
			Cuboid6 plateBox, BlockRenderLayer layer) {
		renderCover(renderState, translation, pipeline, plateBox);
	}
}
