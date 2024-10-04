package com.klmod.mixin;


import net.mcreator.kineticpixel.block.FirearmworktableBlock;
import net.mcreator.kineticpixel.block.entity.FirearmworktableTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FirearmworktableBlock.class})
public abstract class FirearmworktableBlockMixin {

    public FirearmworktableBlockMixin() {
    }

    @Inject(
            method = {"m_6810_"},
            at = {@At("HEAD")},
            cancellable = true,
            remap = false
    )
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci) {
        // 检查方块是否被替换为不同的方块
        if (state.getBlock() != newState.getBlock()) {
            // 获取方块实体
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FirearmworktableTileEntity be) {
                // 遍历所有槽位
                for (int i = 0; i < be.getContainerSize(); i++) {
                    // 如果不是蓝图槽位（32）或枪械槽位（33）
                    if (i != 32 && i != 33) {
                        // 清除该槽位的物品
                        be.setItem(i, ItemStack.EMPTY);
                    }
                }
                // 掉落方块实体中的物品（此时只有槽位 32 和 33 有物品）
                Containers.dropContents(world, pos, be);
                // 更新邻近方块状态
                world.updateNeighbourForOutputSignal(pos, (FirearmworktableBlock)(Object)this);
            }
            // 取消后续的原方法执行（防止重复调用）
            ci.cancel();
        }
    }

}
