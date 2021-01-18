package com.soliddowant.gregtechenergistics.gui;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AEPartLocation;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.helpers.InventoryAction;
import appeng.items.misc.ItemEncodedPattern;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.WrapperRangeItemHandler;
import appeng.util.inv.filter.IAEItemFilter;
import com.soliddowant.gregtechenergistics.networking.NetworkHandler;
import com.soliddowant.gregtechenergistics.networking.PacketCompressedNBT;
import com.soliddowant.gregtechenergistics.parts.StockerTerminalPart;
import com.soliddowant.gregtechenergistics.covers.CoverAE2Stocker;
import com.soliddowant.gregtechenergistics.covers.CoverStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class StockerTerminalContainer extends AEBaseContainer {
    protected static long autoBase = Long.MIN_VALUE;
    protected final Map<CoverAE2Stocker, InvTracker> diList = new HashMap<>();
    protected final Map<Long, InvTracker> byId = new HashMap<>();
    protected IGrid grid;

    public StockerTerminalContainer(final InventoryPlayer ip, final StockerTerminalPart anchor) {
        super(ip, anchor);

        if (Platform.isServer())
            this.grid = anchor.getActionableNode().getGrid();

        this.bindPlayerInventory(ip, 0, 222 - /* height of player inventory */82);
    }

    public static StockerTerminalContainer getServerGuiContainer(AEPartLocation side, EntityPlayer player, World world,
                                                                 int x, int y, int z) {
        StockerTerminalPart part = GuiProxy.getPartAtLocation(world, x, y, z, side, StockerTerminalPart.class);
        if (part == null)
            return null;

        StockerTerminalContainer container = new StockerTerminalContainer(player.inventory, part);

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

        if (this.grid == null)
            return;

        int total = 0;
        boolean missing = false;

        final IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn.isActive()) {
                for (final IGridNode gn : this.grid.getMachines(CoverAE2Stocker.class)) {
                    if (!gn.isActive())
                        continue;

                    final CoverAE2Stocker stocker = (CoverAE2Stocker) gn.getMachine();
                    final InvTracker t = this.diList.get(stocker);
                    if (t == null || !t.unlocalizedName.equals(stocker.getHolderName())) {
                        missing = true;
                        break;
                    }

                    total++;
                }
            }
        }

        NBTTagCompound data = new NBTTagCompound();

        if (total != this.diList.size() || missing)
            this.regenList(data);
        else
            for (final Entry<CoverAE2Stocker, InvTracker> en : this.diList.entrySet()) {
                final InvTracker inv = en.getValue();

                inv.updateServer();

                if (inv.server.isCoverDetailsDifferent(inv.client)) {
                    this.updateTagCoverStatus(data, inv);
                    inv.updateClient();
                }

                for (int i = 0; i < inv.server.itemHandler.getSlots(); i++)
                    if (inv.server.isSlotDifferent(inv.client, i))
                        this.updateTagChildItems(data, inv, i, 1);
            }

        if (data.hasNoTags())
            return;

        NetworkHandler.snw.sendTo(new PacketCompressedNBT(data), (EntityPlayerMP) this.getPlayerInv().player);
    }

    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
        final InvTracker inv = this.byId.get(id);
        if (inv != null) {
            final ItemStack is = inv.server.itemHandler.getStackInSlot(slot);
            final boolean hasItemInHand = !player.inventory.getItemStack().isEmpty();

            final InventoryAdaptor playerHand = new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));

            final IItemHandler theSlot = new WrapperFilteredItemHandler(new WrapperRangeItemHandler(
                    inv.server.itemHandler, slot, slot + 1), new PatternSlotFilter());
            final InventoryAdaptor interfaceSlot = new AdaptorItemHandler(theSlot);

            switch (action) {
                case PICKUP_OR_SET_DOWN:
                    if (hasItemInHand) {
                        ItemStack inSlot = theSlot.getStackInSlot(0);
                        if (inSlot.isEmpty())
                            player.inventory.setItemStack(interfaceSlot.addItems(player.inventory.getItemStack()));
                        else {
                            inSlot = inSlot.copy();
                            final ItemStack inHand = player.inventory.getItemStack().copy();

                            ItemHandlerUtil.setStackInSlot(theSlot, 0, ItemStack.EMPTY);
                            player.inventory.setItemStack(ItemStack.EMPTY);

                            player.inventory.setItemStack(interfaceSlot.addItems(inHand.copy()));

                            if (player.inventory.getItemStack().isEmpty()) {
                                player.inventory.setItemStack(inSlot);
                            } else {
                                player.inventory.setItemStack(inHand);
                                ItemHandlerUtil.setStackInSlot(theSlot, 0, inSlot);
                            }
                        }
                    } else
                        ItemHandlerUtil.setStackInSlot(theSlot, 0, playerHand.addItems(theSlot.getStackInSlot(0)));

                    break;
                case SPLIT_OR_PLACE_SINGLE:
                    if (hasItemInHand) {
                        ItemStack extra = playerHand.removeItems(1, ItemStack.EMPTY, null);
                        if (!extra.isEmpty())
                            extra = interfaceSlot.addItems(extra);
                        if (!extra.isEmpty())
                            playerHand.addItems(extra);
                    } else if (!is.isEmpty()) {
                        ItemStack extra = interfaceSlot.removeItems((is.getCount() + 1) / 2, ItemStack.EMPTY, null);
                        if (!extra.isEmpty())
                            extra = playerHand.addItems(extra);
                        if (!extra.isEmpty())
                            interfaceSlot.addItems(extra);
                    }

                    break;
                case SHIFT_CLICK:
                    final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player);
                    ItemHandlerUtil.setStackInSlot(theSlot, 0, playerInv.addItems(theSlot.getStackInSlot(0)));

                    break;
                case MOVE_REGION:
                    final InventoryAdaptor playerInvAd = InventoryAdaptor.getAdaptor(player);
                    for (int x = 0; x < inv.server.itemHandler.getSlots(); x++)
                        ItemHandlerUtil.setStackInSlot(inv.server.itemHandler, x,
                                playerInvAd.addItems(inv.server.itemHandler.getStackInSlot(x)));

                    break;
                case CREATIVE_DUPLICATE:
                    if (player.capabilities.isCreativeMode && !hasItemInHand)
                        player.inventory.setItemStack(is.isEmpty() ? ItemStack.EMPTY : is.copy());

                    break;
                default:
                    return;
            }

            this.updateHeld(player);
        }
    }

    protected void regenList(final NBTTagCompound data) {
        this.byId.clear();
        this.diList.clear();

        final IActionHost host = this.getActionHost();
        if (host != null) {
            final IGridNode agn = host.getActionableNode();
            if (agn.isActive()) {
                for (final IGridNode gn : this.grid.getMachines(CoverAE2Stocker.class)) {
                    if (!gn.isActive())
                        continue;

                    final CoverAE2Stocker stocker = (CoverAE2Stocker) gn.getMachine();
                    this.diList.put(stocker, InvTracker.build(stocker));
                }
            }
        }

        data.setBoolean("clear", true);

        for (final Entry<CoverAE2Stocker, InvTracker> en : this.diList.entrySet()) {
            final InvTracker inv = en.getValue();
            this.byId.put(inv.which, inv);
            this.buildStockerTag(data, en, 0, inv.server.itemHandler.getSlots());
        }
    }

    protected String getItemTagName(InvTracker tracker) {
        return '=' + Long.toString(tracker.which, Character.MAX_RADIX);
    }

    protected NBTTagCompound getItemTag(NBTTagCompound parentTag, InvTracker tracker) {
        final String name = getItemTagName(tracker);
        final NBTTagCompound tag = parentTag.getCompoundTag(name);

        if (tag.hasNoTags()) {
            tag.setLong("sortBy", tracker.sortBy);
            tag.setString("un", tracker.unlocalizedName);
        }

        return tag;
    }

    protected void updateTagChildItems(NBTTagCompound data, InvTracker tracker, int offset, int length) {
        final NBTTagCompound tag = getItemTag(data, tracker);

        for (int x = 0; x < length; x++) {
            final NBTTagCompound itemNBT = new NBTTagCompound();

            final ItemStack is = tracker.server.itemHandler.getStackInSlot(x + offset);

            // "update" client side.
            ItemHandlerUtil.setStackInSlot(tracker.client.itemHandler, x + offset,
                    is.isEmpty() ? ItemStack.EMPTY : is.copy());

            if (!is.isEmpty())
                is.writeToNBT(itemNBT);

            tag.setTag(Integer.toString(x + offset), itemNBT);
        }

        setItemTag(data, tag, tracker);
    }

    protected void setItemTag(NBTTagCompound parent, NBTTagCompound child, InvTracker inv) {
        if (child.hasNoTags())
            return;

        parent.setTag(getItemTagName(inv), child);
    }

    protected void updateTagCoverStatus(NBTTagCompound data, InvTracker inv) {
        final NBTTagCompound tag = getItemTag(data, inv);

        tag.setLong("stockCount", inv.server.stockAmount);
        tag.setLong("availableCount", inv.server.availableAmount);
        tag.setInteger("status", inv.server.status.ordinal());

        setItemTag(data, tag, inv);
    }

    protected void buildStockerTag(final NBTTagCompound data, final Entry<CoverAE2Stocker, InvTracker> en,
								   @SuppressWarnings("SameParameterValue") final int offset, final int length) {
        InvTracker invTracker = en.getValue();

        updateTagChildItems(data, invTracker, offset, length);
        updateTagCoverStatus(data, invTracker);
    }

    protected static class InvTracker {
        protected final long sortBy;
        protected final long which = autoBase++;
        protected final String unlocalizedName;
        protected final SideDetails client;
        protected final SideDetails server;
        protected final CoverAE2Stocker cover;

        public InvTracker(SideDetails client, SideDetails server, CoverAE2Stocker cover, String unlocalizedName) {
            this.client = client;
            this.server = server;
            this.cover = cover;
            this.unlocalizedName = unlocalizedName;
            BlockPos coverPosition = this.cover.coverHolder.getPos();
            this.sortBy = ((long) coverPosition.getZ() << 24) ^ ((long) coverPosition.getX() << 8) ^ coverPosition.getY();
        }

        public static InvTracker build(final CoverAE2Stocker cover) {
            IItemHandler patterns = cover.getPatternHandler();
            SideDetails server = new SideDetails(patterns, cover);
            SideDetails client = new SideDetails(new AppEngInternalInventory(null, patterns.getSlots()),
					0, 0, null);

            return new InvTracker(client, server, cover, cover.getHolderName());
        }

        public void updateServer() {
            server.update(cover);
        }

        public void updateClient() {
            client.update(cover);
        }
    }

    protected static class SideDetails {
        public IItemHandler itemHandler;
        public long stockAmount;
        public long availableAmount;
        public CoverStatus status;

        public SideDetails(IItemHandler itemHandler, long stockAmount, long availableAmount, CoverStatus status) {
            this.itemHandler = itemHandler;
            this.stockAmount = stockAmount;
            this.availableAmount = availableAmount;
            this.status = status;
        }

        public SideDetails(final IItemHandler itemHandler, final CoverAE2Stocker cover) {
            this(itemHandler, cover.getStockCount(), cover.isGridConnected() ? cover.getOutputLeastAvailableCount() : 0,
                    cover.getCurrentStatus());
        }

        public void update(long stockAmount, long availableAmount, CoverStatus status) {
            this.stockAmount = stockAmount;
            this.availableAmount = availableAmount;
            this.status = status;
        }

        public void update(CoverAE2Stocker cover) {
            update(cover.getStockCount(), cover.isGridConnected() ? cover.getOutputLeastAvailableCount() : 0, cover.getCurrentStatus());
        }

        public boolean isSlotDifferent(final SideDetails otherSide, int slot) {
            return !ItemStack.areItemStacksEqual(this.itemHandler.getStackInSlot(slot),
                    otherSide.itemHandler.getStackInSlot(slot));
        }

        public boolean isCoverDetailsDifferent(final SideDetails otherSide) {
            return this.stockAmount != otherSide.stockAmount || this.availableAmount != otherSide.availableAmount
                    || this.status != otherSide.status;
        }
    }

    protected static class PatternSlotFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemEncodedPattern;
        }
    }
}
