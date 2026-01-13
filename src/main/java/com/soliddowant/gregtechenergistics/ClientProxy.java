package com.soliddowant.gregtechenergistics;

import appeng.api.util.AEColor;
import appeng.client.render.StaticItemColor;
import codechicken.lib.texture.TextureUtils;
import com.soliddowant.gregtechenergistics.items.MetaItems;
import com.soliddowant.gregtechenergistics.render.FluidEncoderModelLoader;
import com.soliddowant.gregtechenergistics.render.Textures;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	@Override
	public void init(FMLInitializationEvent e) {
		super.init(e);
		// Register default AE2 color handler for other meta items
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(
				new StaticItemColor(AEColor.TRANSPARENT), MetaItems.metaItem1);
	}

	@Override
	public void preInit(FMLPreInitializationEvent e) {
		super.preInit(e);
		TextureUtils.addIconRegister(Textures::register);
		// Register custom model loader for fluid encoder
		ModelLoaderRegistry.registerLoader(FluidEncoderModelLoader.INSTANCE);
	}
}
