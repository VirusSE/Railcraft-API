/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2016

 This work (the API) is licensed under the "MIT" License,
 see LICENSE.md for details.
 -----------------------------------------------------------------------------*/

package mods.railcraft.api.tracks;

import mods.railcraft.api.core.IVariantEnum;
import mods.railcraft.api.core.RailcraftConstantsAPI;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * TrackKits are Items that can be applied to existing tracks to transform them into a more advanced track with special
 * properties. This class defines the behaviors of those advanced tracks.
 *
 * Each track equipped with a TrackKit in the world has a ITrackInstance that corresponds with
 * it.
 *
 * Take note of the difference (similar to block classes and tile entities
 * classes).
 *
 * TrackKits must be registered with the TrackRegistry in the Pre-Init phase of a Railcraft Module.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 * @see TrackRegistry
 * @see ITrackKitInstance
 * @see mods.railcraft.api.core.RailcraftModule
 */
public final class TrackKit implements IVariantEnum {
    public static final String NBT_TAG = "kit";
    public static Block blockTrackOutfitted;
    public static Item itemKit;
    @Nonnull
    private final ResourceLocation registryName;
    @Nonnull
    private final Class<? extends ITrackKitInstance> instanceClass;
    private final Predicate<TrackType> trackTypeFilter = (t) -> true;
    private final boolean allowedOnSlopes;
    private final boolean requiresTicks;
    private final boolean visible;
    private final int states;
    private final int maxSupportDistance;

    public TrackKit(@Nonnull ResourceLocation registryName,
                    @Nonnull Class<? extends ITrackKitInstance> instanceClass,
                    boolean allowedOnSlopes, boolean requiresTicks,
                    boolean visible, int states, int maxSupportDistance) {
        this.registryName = registryName;
        this.instanceClass = instanceClass;
        this.allowedOnSlopes = allowedOnSlopes;
        this.requiresTicks = requiresTicks;
        this.visible = visible;
        this.states = states;
        this.maxSupportDistance = maxSupportDistance;
    }

    public static class TrackKitBuilder {
        @Nonnull
        private final ResourceLocation registryName;
        @Nonnull
        private final Class<? extends ITrackKitInstance> instanceClass;
        private Predicate<TrackType> trackTypeFilter = (t) -> true;
        private boolean allowedOnSlopes = true;
        private boolean requiresTicks;
        private boolean visible = true;
        private int states = 1;
        private int maxSupportDistance;

        /**
         * Defines a new track kit spec.
         *
         * @param registryName  A unique internal string identifier (ex.
         *                      "railcraft:one_way")
         * @param instanceClass The ITrackInstance class that corresponds to this
         *                      TrackSpec
         */
        public TrackKitBuilder(@Nonnull ResourceLocation registryName, @Nonnull Class<? extends ITrackKitInstance> instanceClass) {
            this.registryName = registryName;
            this.instanceClass = instanceClass;
        }

        public TrackKit build() {
            return new TrackKit(registryName, instanceClass, allowedOnSlopes, requiresTicks,
                    visible, states, maxSupportDistance);
        }

        public TrackKitBuilder setStates(int states) {
            this.states = states;
            return this;
        }

        public TrackKitBuilder setAllowedOnSlopes(boolean allowedOnSlopes) {
            this.allowedOnSlopes = allowedOnSlopes;
            return this;
        }

        public TrackKitBuilder setMaxSupportDistance(int maxSupportDistance) {
            this.maxSupportDistance = maxSupportDistance;
            return this;
        }

        public TrackKitBuilder setTrackTypeFilter(Predicate<TrackType> filter) {
            this.trackTypeFilter = filter;
            return this;
        }

        public TrackKitBuilder setRequiresTicks(boolean requiresTicks) {
            this.requiresTicks = requiresTicks;
            return this;
        }

        public TrackKitBuilder setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }
    }

    @Override
    @Nonnull
    public String getName() {
        return getRegistryName().toString().replaceAll("[.:]", "_");
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public String getResourcePathSuffix() {
        return getName();
    }

    /**
     * This function will only work after the Init Phase.
     *
     * @return an ItemStack that can be used to place the track.
     */
    @Nullable
    public ItemStack getTrackKitItem() {
        return getTrackKitItem(1);
    }

    /**
     * This function will only work after the Init Phase.
     *
     * @return an ItemStack that can be used to place the track.
     */
    @Nullable
    public ItemStack getTrackKitItem(int qty) {
        if (itemKit != null) {
            ItemStack stack = new ItemStack(itemKit, qty, ordinal());
            NBTTagCompound nbt = stack.getSubCompound(RailcraftConstantsAPI.MOD_ID, true);
            nbt.setString(NBT_TAG, getName());
            return stack;
        }
        return null;
    }

    /**
     * This function will only work after the Init Phase.
     *
     * @return an ItemStack that can be used to place the track.
     */
    @Nullable
    public ItemStack getOutfittedTrack(TrackType trackType) {
        return getOutfittedTrack(trackType, 1);
    }

    /**
     * This function will only work after the Init Phase.
     *
     * @return an ItemStack that can be used to place the track.
     */
    @Nullable
    public ItemStack getOutfittedTrack(TrackType trackType, int qty) {
        if (blockTrackOutfitted != null) {
            ItemStack stack = new ItemStack(blockTrackOutfitted, qty);
            NBTTagCompound nbt = stack.getSubCompound(RailcraftConstantsAPI.MOD_ID, true);
            nbt.setString(TrackType.NBT_TAG, trackType.getName());
            nbt.setString(NBT_TAG, getName());
            return stack;
        }
        return null;
    }

    @Nonnull
    public ITrackKitInstance createInstance() {
        try {
            ITrackKitInstance trackInstance = instanceClass.newInstance();
            if (trackInstance == null) throw new NullPointerException("No track constructor found");
            return trackInstance;
        } catch (Exception ex) {
            throw new RuntimeException("Improper Track Instance Constructor", ex);
        }
    }

    public int getStates() {
        return states;
    }

    public boolean isAllowedOnSlopes() {
        return allowedOnSlopes;
    }

    public int getMaxSupportDistance() {
        return maxSupportDistance;
    }

    public boolean requiresTicks() {
        return requiresTicks;
    }

    public boolean isAllowedTrackType(TrackType trackType) {
        return trackTypeFilter.test(trackType);
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public int ordinal() {
        return TrackRegistry.TRACK_KIT.getId(this);
    }

    @Override
    public String toString() {
        return "TrackKit{" + getName() + "}";
    }
}