/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2019

 This work (the API) is licensed under the "MIT" License,
 see LICENSE.md for details.
 -----------------------------------------------------------------------------*/

package mods.railcraft.api.carts;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;

/**
 * Some helper functions to make interacting with carts simpler.
 * <p/>
 * This interface is implemented by CartBase.
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public interface IMinecart {

    /**
     * Returns true if the Minecart matches the item provided. Generally just
     * stack.isItemEqual(cart.getCartItem()), but some carts may need more
     * control (the Tank Cart for example).
     *
     * @param stack the Filter
     * @param cart  the Cart
     * @return true if the item matches the cart
     */
    boolean doesCartMatchFilter(ItemStack stack, EntityMinecart cart);
}
