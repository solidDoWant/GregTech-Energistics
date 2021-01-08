package com.example.examplemod;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.GuiNull;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerNull;
import appeng.container.ContainerOpenContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiProxy implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
    	int guiId = getGuiIdFromOrdinal(ordinal);
		final AEPartLocation side = AEPartLocation.fromOrdinal(ordinal & 0x07);

    	switch(guiId) {
			case 0:
				BlockPos pos = new BlockPos(x, y, z);
				TileEntity te = world.getTileEntity(pos);

				if (te instanceof IPartHost) {
					final IPart part = ( (IPartHost) te ).getPart( side );

					AEBaseContainer container = null;
					if(part instanceof StockerTerminalPart) {
						container = new StockerTerminalContainer(player.inventory, (StockerTerminalPart) part);
					}

					if(container != null) {
						container.setOpenContext( new ContainerOpenContext(part) );
						container.getOpenContext().setWorld(world);
						container.getOpenContext().setX(x);
						container.getOpenContext().setY(y);
						container.getOpenContext().setZ(z);
						container.getOpenContext().setSide(side);
						return container;
					}
				}
				break;
			case 1:
				break;
		}

        return new ContainerNull();
    }

    @Override
    public Object getClientGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
		int guiId = getGuiIdFromOrdinal(ordinal);
    	final AEPartLocation side = AEPartLocation.fromOrdinal(ordinal & 0x07);

    	switch(guiId) {
			case 0:
				BlockPos pos = new BlockPos(x, y, z);
				TileEntity te = world.getTileEntity(pos);

				if (te instanceof IPartHost) {
					final IPart part = ( (IPartHost) te ).getPart( side );

					if(part instanceof StockerTerminalPart) {
						return new StockerTerminalGuiContainer(player.inventory, (StockerTerminalPart) part);
					}
				}
				break;
		}

        return new GuiNull(new ContainerNull());
    }

    public static int getGuiIdFromOrdinal(int ordinal) {
    	return ordinal >> 3;
	}

	public static int getOrdinalFromGuiId(int id) {
    	return id << 3;
	}
}
