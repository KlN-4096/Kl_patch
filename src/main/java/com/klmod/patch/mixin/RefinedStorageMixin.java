package com.klmod.patch.mixin;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.grid.INetworkAwareGrid;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.grid.CraftingGridBehavior;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin({CraftingGridBehavior.class})
public abstract class RefinedStorageMixin {

    public RefinedStorageMixin() {
    }

    @Inject(
            method = {"onCraftedShift"},
            at = {@At("HEAD")},
            cancellable = true,
            remap = false
    )
    public void onCraftedShift(INetworkAwareGrid grid, Player player, CallbackInfo ci) {

        CraftingContainer matrix = grid.getCraftingMatrix();
        INetwork network = grid.getNetwork();
        List<ItemStack> craftedItemsList = new ArrayList<>();
        ItemStack crafted = Objects.requireNonNull(grid.getCraftingResult()).getItem(0);

        int maxCrafted = Math.min(crafted.getMaxStackSize(), 64); // 这里限制合成最大堆叠数为64，防止过大堆叠


        int amountCrafted = 0;
        boolean useNetwork = network != null && grid.isGridActive();

        IStackList<ItemStack> availableItems = API.instance().createItemStackList();
        if (useNetwork) {
            assert matrix != null;
            forge_1_20_1_47_3_7_mdk$filterDuplicateStacks(network, matrix, availableItems);
        }

        // 记录提取的物品，确保不会重复提取
        IStackList<ItemStack> usedItems = API.instance().createItemStackList();

        ForgeHooks.setCraftingPlayer(player);

        // 在循环中控制每次合成的数量，避免超过最大堆叠
        do {
            grid.onCrafted(player, availableItems, usedItems);
            craftedItemsList.add(crafted.copy());
            amountCrafted += crafted.getCount();


        } while (API.instance().getComparer().isEqual(crafted, grid.getCraftingResult().getItem(0))
                && amountCrafted < maxCrafted
                && amountCrafted + crafted.getCount() <= maxCrafted);

        if (useNetwork) {
            usedItems.getStacks().forEach(stack -> {
                int remainingToExtract = stack.getStack().getCount();

                // 循环提取直到提取完所需数量，或者容器内的物品无法再被提取
                while (remainingToExtract > 0) {
                    int extractAmount = Math.min(remainingToExtract, stack.getStack().getMaxStackSize());

                    // 提取物品
                    ItemStack extracted = network.extractItem(stack.getStack(), extractAmount, Action.PERFORM);

                    // 如果无法提取物品，跳出循环
                    if (extracted.isEmpty()) {
                        break; // 退出循环，防止死循环
                    }

                    remainingToExtract -= extracted.getCount();
                }
            });
        }



        for (ItemStack craftedItem : craftedItemsList) {
            // 尝试将物品插入玩家背包
            ItemStack remainder = ItemHandlerHelper.insertItem(new PlayerMainInvWrapper(player.getInventory()), craftedItem.copy(), false);

            // 如果无法全部放入玩家背包，则插入到网络中
            if (!remainder.isEmpty() && useNetwork) {

                // 确保多余物品只插入一次到网络
                remainder = network.insertItem(remainder, remainder.getCount(), Action.PERFORM);

            }
        }

        // @Volatile: This is some logic copied from ResultSlot#checkTakeAchievements. We call this manually for shift clicking because
        // otherwise it's not being called.
        // For regular crafting, this is already called in ResultCraftingGridSlot#onTake -> checkTakeAchievements(stack)
        crafted.onCraftedBy(player.level(), player, amountCrafted);
        ForgeEventFactory.firePlayerCraftingEvent(player, ItemHandlerHelper.copyStackWithSize(crafted, amountCrafted), grid.getCraftingMatrix());
        ForgeHooks.setCraftingPlayer(null);

        ci.cancel();
    }

    @Unique
    private void forge_1_20_1_47_3_7_mdk$filterDuplicateStacks(INetwork network, CraftingContainer matrix, IStackList<ItemStack> availableItems) {
        for (int i = 0; i < matrix.getContainerSize(); ++i) {
            ItemStack stack = network.getItemStorageCache().getList().get(matrix.getItem(i));

            //Don't add the same item twice into the list. Items may appear twice in a recipe but not in storage.
            if (stack != null && availableItems.get(stack) == null) {
                availableItems.add(stack);
            }
        }
    }


}
