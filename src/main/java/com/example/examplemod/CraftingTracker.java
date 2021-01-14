package com.example.examplemod;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class CraftingTracker {
    protected final ICraftingRequester requester;
    protected final IActionSource source;
    // Contains crafting requests that are currently being calculated (calculates things like required materials).
    protected Map<Integer, Future<ICraftingJob>> jobs;
    // After jobs are calculated, they are submitted and a 'link' to the job is generated.
    protected Map<Integer, ICraftingLink> links;

    public CraftingTracker(final ICraftingRequester requester, final IActionSource source) {
        this.requester = requester;
        this.source = source;
        jobs = new HashMap<>();
        links = new HashMap<>();
    }

    public void readFromNBT(final NBTTagCompound tag) {
        if(!tag.hasKey("CraftingTracker"))
            return;

        NBTTagCompound trackerTag = tag.getCompoundTag("CraftingTracker");
        for(String key : trackerTag.getKeySet()) {
            links.put(Integer.parseInt(key),
                    AEApi.instance().storage().loadCraftingLink(trackerTag.getCompoundTag(key), requester));
        }
    }

    public void writeToNBT(final NBTTagCompound tag) {
        NBTTagCompound trackerTag = new NBTTagCompound();
        for(Map.Entry<Integer, ICraftingLink> linkEntry : links.entrySet()) {
            NBTTagCompound linkTag = new NBTTagCompound();
            linkEntry.getValue().writeToNBT(linkTag);

            trackerTag.setTag(String.valueOf(linkEntry.getKey()), linkTag);
        }
        tag.setTag("CraftingTracker", trackerTag);
    }

    // Returns true if a job has successfully been fired off. May need to be called multiple times.
    public boolean handleCrafting(final IAEItemStack requestedItem, final ICraftingGrid craftingGrid, final World world, final IGrid grid) {
        if(requestedItem == null)
            return false;

        // Case 1: Job has already been fired off and the crafting grid is trying to complete the request
        ICraftingLink existingLink = getLink(requestedItem);
        if(existingLink != null) {
            if(existingLink.isDone() || existingLink.isCanceled())
                removeLink(requestedItem);

            return false;
        }

        // Case 2: Job is being calculated but has not been fired off
        final Future<ICraftingJob> craftingJobFuture = getJob(requestedItem);
        if(craftingJobFuture != null) {
            try {
                if(craftingJobFuture.isDone()) {
                    ICraftingJob craftingJob = craftingJobFuture.get();
                    if(craftingJob == null)
                        return false;

                    final ICraftingLink newLink =
                            craftingGrid.submitJob(craftingJob, requester, null, false, source);

                    removeJob(requestedItem);

                    if(newLink == null)
                        return false;

                    addLink(requestedItem, newLink);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        // Case 3: Job has not been calculated (or fired off)
        IAEItemStack jobStack = requestedItem.copy();
        addJob(jobStack, craftingGrid.beginCraftingJob(world, grid, source, jobStack, null));
        return false;
    }

    protected ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.copyOf(links.values());
    }

    public void jobStateChange(final ICraftingLink changedLink) {
        for(Map.Entry<Integer, ICraftingLink> linkEntry : links.entrySet()) {
            if(linkEntry.getValue() == changedLink) {
                links.remove(linkEntry.hashCode());
                return;
            }
        }
    }

    protected Future<ICraftingJob> getJob(IAEItemStack item) {
        return getAEItemStackMapItem(item, jobs);
    }

    protected void addJob(IAEItemStack item, Future<ICraftingJob> job) {
        jobs.put(item.hashCode(), job);
    }

    protected void removeJob(IAEItemStack item) {
        jobs.remove(item.hashCode());
    }

    protected void removeLink(IAEItemStack item) {
        links.remove(item.hashCode());
    }

    protected ICraftingLink getLink(IAEItemStack item) {
        return getAEItemStackMapItem(item, links);
    }

    protected void addLink(IAEItemStack item, ICraftingLink job) {
        links.put(item.hashCode(), job);
    }

    protected <T, U extends Map<Integer, T>> T getAEItemStackMapItem(IAEItemStack stack, U map) {
        if(stack == null || map == null)
            return null;

        int key = stack.hashCode();

        if(!map.containsKey(key))
            return null;

        return map.get(key);
    }

    public void cancelJobs() {
        jobs.values().forEach(job -> job.cancel(true));
        jobs.clear();
    }

    public void cancelLinks() {
        links.values().forEach(ICraftingLink::cancel);
        links.clear();
    }

    public void cancelAll() {
        cancelJobs();
        cancelLinks();
    }
}
