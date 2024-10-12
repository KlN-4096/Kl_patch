package com.klmod.patch.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;

public class DeleteMasslessShipsCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        // 获取当前维度的 shipObjectWorld 对象
        IShipObjectWorldServerProvider level = (IShipObjectWorldServerProvider) context.getSource().getLevel();
        ServerShipWorldCore shipObjectWorld = level.getShipObjectWorld();

        if (shipObjectWorld == null) {
            context.getSource().sendFailure(Component.literal("No ship world available in the current dimension."));
            return 1;
        }

        // 筛选出质量为 0 的船只并删除
        long deletedShipsCount = shipObjectWorld.getAllShips().stream()
                .filter(ship -> ship.getInertiaData().getMass() == 0.0)
                .peek(shipObjectWorld::deleteShip)
                .count();

        if (deletedShipsCount > 0) {
            context.getSource().sendSuccess(() -> Component.literal("Successfully deleted " + deletedShipsCount + " massless ships."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No massless ships found to delete."), false);
        }

        // 返回命令执行的结果
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("removeZeroMassShip")
                .requires(source -> source.hasPermission(2))
                .executes(DeleteMasslessShipsCommand::execute);
    }
}