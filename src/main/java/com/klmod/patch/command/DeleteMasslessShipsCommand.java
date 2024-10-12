package com.klmod.patch.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.function.Predicate;

public class DeleteMasslessShipsCommand {

    public static int execute(CommandContext<CommandSourceStack> context, String type) {
        Predicate<ServerShip> condition;
        String description;

        switch (type) {
            case "zeromass":
                condition = ship -> ((ServerShip) ship).getInertiaData().getMass() == 0.0;
                description = "massless ships";
                break;
            case "lowy":
                condition = ship -> ((ServerShip) ship).getTransform().getPositionInWorld().y() < -200;
                description = "ships below Y level -120";
                break;
            case "highy":
                condition = ship -> ((ServerShip) ship).getTransform().getPositionInWorld().y() > 1000;
                description = "ships above Y level 500";
                break;
            default:
                context.getSource().sendFailure(Component.literal("Invalid type specified. Use one of: zeromass, lowy, highy."));
                return 1;
        }

        return executeWithCondition(context, condition, description);
    }

    private static int executeWithCondition(CommandContext<CommandSourceStack> context, Predicate<ServerShip> condition, String description) {
        // 获取当前维度的 shipObjectWorld 对象
        IShipObjectWorldServerProvider level = (IShipObjectWorldServerProvider) context.getSource().getLevel();
        ServerShipWorldCore shipObjectWorld = level.getShipObjectWorld();

        if (shipObjectWorld == null) {
            context.getSource().sendFailure(Component.literal("No ship world available in the current dimension."));
            return 1;
        }

        // 先收集符合条件的船只以避免 ConcurrentModificationException
        List<ServerShip> shipsToDelete = shipObjectWorld.getAllShips().stream()
                .filter(ship -> ship instanceof ServerShip)
                .map(ship -> (ServerShip) ship)
                .filter(condition)
                .toList();

        // 删除船只
        shipsToDelete.forEach(shipObjectWorld::deleteShip);

        long deletedShipsCount = shipsToDelete.size();

        if (deletedShipsCount > 0) {
            context.getSource().sendSuccess(() -> Component.literal("Successfully deleted " + deletedShipsCount + " " + description + "."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("No " + description + " found to delete."), false);
        }

        // 返回命令执行的结果
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("removeships")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("zeromass");
                            builder.suggest("lowy");
                            builder.suggest("highy");
                            return builder.buildFuture();
                        })
                        .executes(context -> execute(context, StringArgumentType.getString(context, "type"))));
    }
}