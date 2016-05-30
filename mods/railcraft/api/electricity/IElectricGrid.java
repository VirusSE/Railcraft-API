/*
 * ******************************************************************************
 *  Copyright 2011-2015 CovertJaguar
 *
 *  This work (the API) is licensed under the "MIT" License, see LICENSE.md for details.
 * ***************************************************************************
 */

package mods.railcraft.api.electricity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Any Electric Track needs to implement this interface on either the track
 * TileEntity or ITrackInstance object.
 *
 * Other blocks can also implement this on their tile entity to gain access to
 * the grid.
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
//TODO: convert to capability
public interface IElectricGrid {

    double MAX_CHARGE = 10000.0;
    double TRACK_LOSS_PER_TICK = 0.05;
    int SEARCH_INTERVAL = 64;
    Random rand = new Random();

    @Nullable
    ChargeHandler getChargeHandler();

    TileEntity getTile();

    final class ChargeHandler {

        public enum ConnectType {

            TRACK {
                @Override
                public Map<BlockPos, EnumSet<ConnectType>> getPossibleConnectionLocations(IElectricGrid gridObject) {
                    BlockPos pos = gridObject.getTile().getPos();
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    Map<BlockPos, EnumSet<ConnectType>> positions = new HashMap<BlockPos, EnumSet<ConnectType>>();

                    EnumSet<ConnectType> all = EnumSet.allOf(ConnectType.class);
                    EnumSet<ConnectType> notWire = EnumSet.complementOf(EnumSet.of(ConnectType.WIRE));
                    EnumSet<ConnectType> track = EnumSet.of(ConnectType.TRACK);

                    positions.put(new BlockPos(x + 1, y, z), notWire);
                    positions.put(new BlockPos(x - 1, y, z), notWire);

                    positions.put(new BlockPos(x + 1, y + 1, z), track);
                    positions.put(new BlockPos(x + 1, y - 1, z), track);

                    positions.put(new BlockPos(x - 1, y + 1, z), track);
                    positions.put(new BlockPos(x - 1, y - 1, z), track);

                    positions.put(new BlockPos(x, y - 1, z), all);

                    positions.put(new BlockPos(x, y, z + 1), notWire);
                    positions.put(new BlockPos(x, y, z - 1), notWire);

                    positions.put(new BlockPos(x, y + 1, z + 1), track);
                    positions.put(new BlockPos(x, y - 1, z + 1), track);

                    positions.put(new BlockPos(x, y + 1, z - 1), track);
                    positions.put(new BlockPos(x, y - 1, z - 1), track);
                    return positions;
                }

            },
            WIRE {
                @Override
                public Map<BlockPos, EnumSet<ConnectType>> getPossibleConnectionLocations(IElectricGrid gridObject) {
                    BlockPos pos = gridObject.getTile().getPos();
                    int x = pos.getX();
                    int y = pos.getY();
                    int z = pos.getZ();
                    Map<BlockPos, EnumSet<ConnectType>> positions = new HashMap<BlockPos, EnumSet<ConnectType>>();

                    EnumSet<ConnectType> all = EnumSet.allOf(ConnectType.class);
                    EnumSet<ConnectType> notTrack = EnumSet.complementOf(EnumSet.of(ConnectType.TRACK));

                    positions.put(new BlockPos(x + 1, y, z), notTrack);
                    positions.put(new BlockPos(x - 1, y, z), notTrack);
                    positions.put(new BlockPos(x, y + 1, z), all);
                    positions.put(new BlockPos(x, y - 1, z), notTrack);
                    positions.put(new BlockPos(x, y, z + 1), notTrack);
                    positions.put(new BlockPos(x, y, z - 1), notTrack);
                    return positions;
                }

            },
            BLOCK {
                @Override
                public Map<BlockPos, EnumSet<ConnectType>> getPossibleConnectionLocations(IElectricGrid gridObject) {
                    BlockPos pos = gridObject.getTile().getPos();
                    Map<BlockPos, EnumSet<ConnectType>> positions = new HashMap<BlockPos, EnumSet<ConnectType>>();

                    EnumSet<ConnectType> all = EnumSet.allOf(ConnectType.class);

                    for (EnumFacing facing : EnumFacing.VALUES) {
                        positions.put(pos.offset(facing), all);
                    }
                    return positions;
                }

            };

            @Nonnull
            public abstract Map<BlockPos, EnumSet<ConnectType>> getPossibleConnectionLocations(IElectricGrid gridObject);

        }

        @Nonnull
        private final IElectricGrid gridObject;
        @Nonnull
        private final ConnectType type;
        @Nonnull
        private final Set<ChargeHandler> neighbors = new HashSet<ChargeHandler>();
        private double charge, draw, lastTickDraw;
        private final double lossPerTick;
        private int clock = rand.nextInt();

        public ChargeHandler(@Nonnull IElectricGrid gridObject, @Nonnull ConnectType type) {
            this(gridObject, type, type == ConnectType.TRACK ? TRACK_LOSS_PER_TICK : 0.0);
        }

        public ChargeHandler(@Nonnull IElectricGrid gridObject, @Nonnull ConnectType type, double lossPerTick) {
            this.gridObject = gridObject;
            this.type = type;
            this.lossPerTick = lossPerTick;
        }

        public Map<BlockPos, EnumSet<ConnectType>> getPossibleConnectionLocations() {
            return type.getPossibleConnectionLocations(gridObject);
        }

        public double getCharge() {
            return charge;
        }

        public double getCapacity() {
            return MAX_CHARGE;
        }

        public double getLosses() {
            return lossPerTick;
        }

        public double getDraw() {
            return draw;
        }

        public ConnectType getType() {
            return type;
        }

        /**
         * Averages the charge between two ChargeHandlers.
         */
        public void balance(ChargeHandler other) {
            double total = charge + other.charge;
            double half = total / 2.0;
            charge = half;
            other.charge = half;
        }

        public void setCharge(double charge) {
            this.charge = charge;
        }

        public void addCharge(double charge) {
            this.charge += charge;
        }

        /**
         * Remove up to the requested amount of charge and returns the amount
         * removed.
         *
         * @return charge removed
         */
        public double removeCharge(double request) {
            if (charge >= request) {
                charge -= request;
                lastTickDraw += request;
                return request;
            }
            double ret = charge;
            charge = 0.0;
            lastTickDraw += ret;
            return ret;
        }

        private void removeLosses() {
            if (lossPerTick > 0.0)
                if (charge >= lossPerTick)
                    charge -= lossPerTick;
                else
                    charge = 0.0;
        }

        /**
         * Must be called once per tick by the owning object. Server side only.
         */
        public void tick() {
            clock++;
            removeLosses();

            draw = (draw * 49.0 + lastTickDraw) / 50.0;
            lastTickDraw = 0.0;

            if (charge <= 0.0)
                return;

            if (clock % SEARCH_INTERVAL == 0) {
                neighbors.clear();
                Set<IElectricGrid> connections = GridTools.getMutuallyConnectedObjects(gridObject);
                for (IElectricGrid t : connections) {
                    neighbors.add(t.getChargeHandler());
                }
            }

            Iterator<ChargeHandler> it = neighbors.iterator();
            while (it.hasNext()) {
                ChargeHandler ch = it.next();
                if (ch == null || ch.gridObject.getTile().isInvalid())
                    it.remove();
            }
            for (ChargeHandler t : neighbors) {
                balance(t);
            }
        }

        /**
         * Must be called by the owning object's save function.
         */
        public void writeToNBT(NBTTagCompound nbt) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setDouble("charge", charge);
            nbt.setTag("chargeHandler", tag);
        }

        /**
         * Must be called by the owning object's load function.
         */
        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagCompound tag = nbt.getCompoundTag("chargeHandler");
            charge = tag.getDouble("charge");
        }

    }

}
