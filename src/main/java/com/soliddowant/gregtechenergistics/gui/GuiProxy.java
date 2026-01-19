package com.soliddowant.gregtechenergistics.gui;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.GuiNull;
import appeng.container.ContainerNull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {
	// GUI ID constants
	public static final int GUI_STOCKER_TERMINAL = 0;
	public static final int GUI_EXTENDED_PATTERN_TERMINAL = 1;

	@Override
	public Object getServerGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
		int guiId = getGuiIdFromOrdinal(ordinal);
		final AEPartLocation side = AEPartLocation.fromOrdinal(ordinal & 0x07);

		Object container = null;
		switch (guiId) {
			case GUI_STOCKER_TERMINAL:
				container = StockerTerminalContainer.getServerGuiContainer(side, player, world, x, y, z);
				break;
			case GUI_EXTENDED_PATTERN_TERMINAL:
				container = ExtendedPatternContainer.getServerGuiContainer(side, player, world, x, y, z);
				break;
		}

		if (container == null)
			return new ContainerNull();

		return container;
	}

	@Override
	public Object getClientGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
		int guiId = getGuiIdFromOrdinal(ordinal);
		final AEPartLocation side = AEPartLocation.fromOrdinal(ordinal & 0x07);

		Object guiContainer = null;
		switch (guiId) {
			case GUI_STOCKER_TERMINAL:
				guiContainer = StockerTerminalGuiContainer.getClientGuiElement(side, player, world, x, y, z);
				break;
			case GUI_EXTENDED_PATTERN_TERMINAL:
				guiContainer = ExtendedPatternGuiContainer.getClientGuiContainer(side, player, world, x, y, z);
				break;
		}

		if (guiContainer == null)
			return new GuiNull(new ContainerNull());

		return guiContainer;
	}

	public static int getGuiIdFromOrdinal(int ordinal) {
		return ordinal >> 3;
	}

	public static int getOrdinalFromGuiId(int id) {
		return id << 3;
	}

	public static <T extends IPart> T getPartAtLocation(World w, int x, int y, int z, AEPartLocation side,
			Class<T> partType) {
		BlockPos pos = new BlockPos(x, y, z);
		TileEntity te = w.getTileEntity(pos);

		if (!(te instanceof IPartHost))
			return null;

		final IPart part = ((IPartHost) te).getPart(side);

		if (!partType.isInstance(part))
			return null;

		return partType.cast(part);
	}
}
