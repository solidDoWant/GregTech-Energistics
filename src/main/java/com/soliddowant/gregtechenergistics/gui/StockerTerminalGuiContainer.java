package com.soliddowant.gregtechenergistics.gui;

import appeng.api.AEApi;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.util.AEPartLocation;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.ClientDCInternalInv;
import appeng.client.me.SlotDisconnected;
import appeng.util.Platform;
import appeng.util.ReadableNumberConverter;
import com.google.common.collect.HashMultimap;
import com.soliddowant.gregtechenergistics.parts.StockerTerminalPart;
import com.soliddowant.gregtechenergistics.covers.CoverStatus;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.*;

public class StockerTerminalGuiContainer extends AEBaseGui {
	protected final int offsetX = 9;
	protected static final int LINES_ON_PAGE = 6;

	protected MEGuiTextField searchField;
	protected final HashMap<Long, StockerInformation> byId = new HashMap<>();
	protected final ArrayList<Object> lines = new ArrayList<>();
	protected final HashMultimap<String, StockerInformation> byName = HashMultimap.create();
	protected final ArrayList<String> names = new ArrayList<>();

	protected boolean refreshList = false;
	protected final Map<String, Set<Object>> cachedSearches = new WeakHashMap<>();

	public StockerTerminalGuiContainer(final InventoryPlayer inventoryPlayer, final StockerTerminalPart te) {
		super(new StockerTerminalContainer(inventoryPlayer, te));

		final GuiScrollbar scrollbar = new GuiScrollbar();
		this.setScrollBar(scrollbar);
		this.xSize = 195;
		this.ySize = 222;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.getScrollBar().setLeft(175);
		this.getScrollBar().setHeight(106);
		this.getScrollBar().setTop(18);

		this.searchField = new MEGuiTextField(this.fontRenderer, this.guiLeft + Math.max(104, this.offsetX),
				this.guiTop + 4, 65, 12);
		this.searchField.setEnableBackgroundDrawing(false);
		this.searchField.setMaxStringLength(25);
		this.searchField.setTextColor(0xFFFFFF);
		this.searchField.setVisible(true);
		this.searchField.setFocused(true);
	}

	@Override
	public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		this.fontRenderer.drawString(I18n.format("gui.gregtechenergistics.terminal.stocker"),
				8, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("gui.gregtechenergistics.terminal.inventory"),
				8, this.ySize - 96 + 3, 4210752);

		final int ex = this.getScrollBar().getCurrentScroll();

		this.inventorySlots.inventorySlots.removeIf(slot -> slot instanceof SlotDisconnected);

		int offset = 17;
		for (int x = 0; x < LINES_ON_PAGE && ex + x < this.lines.size(); x++) {
			final Object lineObj = this.lines.get(ex + x);
			if (lineObj instanceof StockerInformation) {
				final StockerInformation stockerInformation = (StockerInformation) lineObj;
				this.inventorySlots.inventorySlots
						.add(new SlotDisconnected(stockerInformation.patternInventory, 0, 10, 1 + offset));

				String displayText = ReadableNumberConverter.INSTANCE
						.toWideReadableForm(stockerInformation.availableCount) + "/"
						+ ReadableNumberConverter.INSTANCE.toWideReadableForm(stockerInformation.stockCount);
				displayText += "; " + stockerInformation.status.toString();

				this.fontRenderer.drawString(displayText, 29, 5 + offset, 4210752);
			} else if (lineObj instanceof String) {
				String name = (String) lineObj;
				final int rows = this.byName.get(name).size();
				if (rows > 1)
					name = name + " (" + rows + ')';

				while (name.length() > 2 && this.fontRenderer.getStringWidth(name) > 155)
					name = name.substring(0, name.length() - 1);

				this.fontRenderer.drawString(name, 10, 6 + offset, 4210752);
			}
			offset += 18;
		}
	}

	@Override
	protected void mouseClicked(final int xCoord, final int yCoord, final int btn) throws IOException {
		this.searchField.mouseClicked(xCoord, yCoord, btn);

		if (btn == 1 && this.searchField.isMouseIn(xCoord, yCoord)) {
			this.searchField.setText("");
			this.refreshList();
		}

		super.mouseClicked(xCoord, yCoord, btn);
	}

	@Override
	public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		this.bindTexture("guis/interfaceterminal.png");
		this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);

		int offset = 17;
		final int ex = this.getScrollBar().getCurrentScroll();

		for (int x = 0; x < LINES_ON_PAGE && ex + x < this.lines.size(); x++) {
			final Object lineObj = this.lines.get(ex + x);
			if (lineObj instanceof StockerInformation) {
				final StockerInformation stockerInfo = (StockerInformation) lineObj;

				GlStateManager.color(1, 1, 1, 1);
				final int width = stockerInfo.patternInventory.getInventory().getSlots() * 18;
				this.drawTexturedModalRect(offsetX + 9, offsetY + offset, 7, 139, width, 18);
			}
			offset += 18;
		}

		if (this.searchField != null)
			this.searchField.drawTextBox();
	}

	@Override
	protected void keyTyped(final char character, final int key) throws IOException {
		if (!this.checkHotbarKeys(key)) {
			if (character == ' ' && this.searchField.getText().isEmpty())
				return;

			if (this.searchField.textboxKeyTyped(character, key))
				this.refreshList();
			else
				super.keyTyped(character, key);
		}
	}

	public void postUpdate(final NBTTagCompound in) {
		if (in.getBoolean("clear")) {
			this.byId.clear();
			this.refreshList = true;
		}

		for (final Object oKey : in.getKeySet()) {
			final String key = (String) oKey;
			if (key.startsWith("=")) {
				try {
					final long id = Long.parseLong(key.substring(1), Character.MAX_RADIX);
					final NBTTagCompound invData = in.getCompoundTag(key);
					final StockerInformation current = this.getById(id, invData);

					for (int x = 0; x < current.patternInventory.getInventory().getSlots(); x++) {
						final String which = Integer.toString(x);
						if (invData.hasKey(which))
							current.patternInventory.getInventory().setStackInSlot(x,
									new ItemStack(invData.getCompoundTag(which)));
					}

					current.availableCount = invData.getLong("availableCount");
					current.stockCount = invData.getLong("stockCount");
					current.status = CoverStatus.values()[invData.getInteger("status")];
				} catch (final NumberFormatException ignored) {
				}
			}
		}

		if (this.refreshList) {
			this.refreshList = false;
			// invalid caches on refresh
			this.cachedSearches.clear();
			this.refreshList();
		}
	}

	/**
	 * Rebuilds the list of interfaces.
	 *
	 * Respects a search term if present (ignores case) and adding only matching
	 * patterns.
	 */
	protected void refreshList() {
		this.byName.clear();

		final String searchFilterLowerCase = this.searchField.getText().toLowerCase();

		final Set<Object> cachedSearch = this.getCacheForSearchTerm(searchFilterLowerCase);
		final boolean rebuild = cachedSearch.isEmpty();

		for (final StockerInformation entry : this.byId.values()) {
			// ignore inventory if not doing a full rebuild or cache already marks it as
			// miss.
			if (!rebuild && !cachedSearch.contains(entry))
				continue;

			boolean found = false;

			// Search if the current inventory holds a pattern containing the search term.
			if (!searchFilterLowerCase.isEmpty())
				for (final ItemStack itemStack : entry.patternInventory.getInventory())
					if ((found = this.itemStackMatchesSearchTerm(itemStack, searchFilterLowerCase)))
						break;

			// if found, filter skipped or machine name matching the search term, add it
			if (found || entry.patternInventory.getName().toLowerCase().contains(searchFilterLowerCase)) {
				this.byName.put(entry.patternInventory.getName(), entry);
				cachedSearch.add(entry);
			} else
				cachedSearch.remove(entry);
		}

		this.names.clear();
		this.names.addAll(this.byName.keySet());

		Collections.sort(this.names);

		this.lines.clear();
		this.lines.ensureCapacity(this.getMaxRows());

		for (final String n : this.names) {
			this.lines.add(n);

			final ArrayList<StockerInformation> clientInventories = new ArrayList<>(this.byName.get(n));

			Collections.sort(clientInventories);
			this.lines.addAll(clientInventories);
		}

		this.getScrollBar().setRange(0, this.lines.size() - LINES_ON_PAGE, 2);
	}

	/**
	 * Tries to retrieve a cache for a with search term as keyword.
	 *
	 * If this cache should be empty, it will populate it with an earlier cache if
	 * available or at least the cache for the empty string.
	 *
	 * @param searchTerm the corresponding search
	 *
	 * @return a Set matching a superset of the search term
	 */
	protected Set<Object> getCacheForSearchTerm(final String searchTerm) {
		if (!this.cachedSearches.containsKey(searchTerm))
			this.cachedSearches.put(searchTerm, new HashSet<>());

		final Set<Object> cache = this.cachedSearches.get(searchTerm);

		if (cache.isEmpty() && searchTerm.length() > 1) {
			cache.addAll(this.getCacheForSearchTerm(searchTerm.substring(0, searchTerm.length() - 1)));
			return cache;
		}

		return cache;
	}

	protected boolean itemStackMatchesSearchTerm(final ItemStack itemStack, final String searchTerm) {
		if (itemStack.isEmpty())
			return false;

		final NBTTagCompound encodedValue = itemStack.getTagCompound();

		if (encodedValue == null)
			return false;

		final NBTTagList outTag = encodedValue.getTagList("out", 10);

		for (int i = 0; i < outTag.tagCount(); i++) {
			final ItemStack parsedItemStack = new ItemStack(outTag.getCompoundTagAt(i));
			if (!parsedItemStack.isEmpty()) {
				final String displayName = Platform.getItemDisplayName(AEApi.instance().storage()
						.getStorageChannel(IItemStorageChannel.class).createStack(parsedItemStack)).toLowerCase();
				if (displayName.contains(searchTerm))
					return true;
			}
		}
		return false;
	}

	/**
	 * The max amount of unique names and each inv row. Not affected by the
	 * filtering.
	 *
	 * @return max amount of unique names and each inv row
	 */
	protected int getMaxRows() {
		return this.names.size() + this.byId.size();
	}

	protected StockerInformation getById(final long id, NBTTagCompound tag) {
		StockerInformation o = this.byId.get(id);

		if (o == null) {
			this.byId.put(id, o = StockerInformation.fromNBT(1, id, tag));
			this.refreshList = true;
		}

		return o;
	}

	protected static class StockerInformation implements Comparable<StockerInformation> {
		public ClientDCInternalInv patternInventory;
		public long stockCount;
		public long availableCount;
		public CoverStatus status;

		public StockerInformation(ClientDCInternalInv patternInventory, long stockCount, long availableCount,
				CoverStatus status) {
			this.patternInventory = patternInventory;
			this.stockCount = stockCount;
			this.availableCount = availableCount;
			this.status = status;
		}

		public static StockerInformation fromNBT(int size, long id, NBTTagCompound tag) {
			return new StockerInformation(new ClientDCInternalInv(size, id, tag.getLong("sortBy"),
					tag.getString("un")), tag.getLong("stockCount"), tag.getLong("availableCount"),
					CoverStatus.values()[tag.getInteger("status")]);
		}

		@Override
		public int compareTo(StockerInformation o) {
			return patternInventory.compareTo(o.patternInventory);
		}
	}

	public static StockerTerminalGuiContainer getClientGuiElement(AEPartLocation side, EntityPlayer player, World world,
																  int x, int y, int z) {
		StockerTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side, StockerTerminalPart.class);
		if(part == null)
			return null;

		return new StockerTerminalGuiContainer(player.inventory, part);
	}
}
