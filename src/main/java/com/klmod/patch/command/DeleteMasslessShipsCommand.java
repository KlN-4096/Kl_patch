package com.klmod.patch.command;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;

import java.util.List;
import java.util.function.Predicate;

public class DeleteMasslessShipsCommand {

    public static int execute(CommandContext<CommandSourceStack> context, String type, double yValue) {
        Predicate<ServerShip> condition;
        String description;

        switch (type) {
            case "zeromass":
                condition = ship -> ship.getInertiaData().getMass() == 0.0;
                description = "massless ships";
                break;
            case "on":
                condition = ship -> ship.getTransform().getPositionInWorld().y() == yValue;
                description = "ships on Y level " + yValue;
                break;
            case "below":
                condition = ship -> ship.getTransform().getPositionInWorld().y() < yValue;
                description = "ships below Y level " + yValue;
                break;
            default:
                context.getSource().sendFailure(Component.literal("Invalid type specified. Use one of: zeromass, on, below.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))));
                return 1;
        }

        return executeWithCondition(context, condition, description);
    }

    private static int executeWithCondition(CommandContext<CommandSourceStack> context, Predicate<ServerShip> condition, String description) {
        // 获取当前维度的 shipObjectWorld 对象
        IShipObjectWorldServerProvider level = (IShipObjectWorldServerProvider) context.getSource().getLevel();
        ServerShipWorldCore shipObjectWorld = level.getShipObjectWorld();

        if (shipObjectWorld == null) {
            context.getSource().sendFailure(Component.literal("No ship world available in the current dimension.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))));
            return 1;
        }

        // 先收集符合条件的船只以避免 ConcurrentModificationException
        List<ServerShip> shipsToDelete = shipObjectWorld.getAllShips().stream()
                .filter(ship -> ship instanceof ServerShip)
                .filter(condition)
                .toList();

        // 删除船只
        shipsToDelete.forEach(shipObjectWorld::deleteShip);

        long deletedShipsCount = shipsToDelete.size();

        if (deletedShipsCount > 0) {
            context.getSource().sendSuccess(() -> Component.literal("✔ Successfully deleted " + deletedShipsCount + " " + description + ".").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("⚠ No " + description + " found to delete.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))), false);
        }

        // 返回命令执行的结果
        return 0;
    }

    public static int listAllShips(CommandContext<CommandSourceStack> context) {
        // 获取当前维度的 shipObjectWorld 对象
        IShipObjectWorldServerProvider level = (IShipObjectWorldServerProvider) context.getSource().getLevel();
        ServerShipWorldCore shipObjectWorld = level.getShipObjectWorld();

        if (shipObjectWorld == null) {
            context.getSource().sendFailure(Component.literal("No ship world available in the current dimension.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))));
            return 1;
        }

        // 收集所有船只
        List<ServerShip> ships = shipObjectWorld.getAllShips().stream()
                .filter(ship -> ship instanceof ServerShip)
                .toList();

        if (ships.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("⚠ No ships available in the current dimension.").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))), false);
        } else {
            StringBuilder shipList = new StringBuilder("✨ Ships in the current dimension:\n");
            ships.forEach(ship -> shipList.append(String.format("▶ %-20s | Position: x=%-8.2f y=%-8.2f z=%-8.2f\n",
                    (ship.getSlug() != null ? ship.getSlug() : ship.getId()),
                    ship.getTransform().getPositionInWorld().x(),
                    ship.getTransform().getPositionInWorld().y(),
                    ship.getTransform().getPositionInWorld().z())));
            context.getSource().sendSuccess(() -> Component.literal(shipList.toString()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))), false);
        }

        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("ships")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("remove")
                        .then(Commands.literal("on")
                                .then(Commands.argument("yValue", DoubleArgumentType.doubleArg())
                                        .executes(context -> execute(context, "on", DoubleArgumentType.getDouble(context, "yValue")))))
                        .then(Commands.literal("below")
                                .then(Commands.argument("yValue", DoubleArgumentType.doubleArg())
                                        .executes(context -> execute(context, "below", DoubleArgumentType.getDouble(context, "yValue")))))
                        .then(Commands.literal("zeromass")
                                .executes(context -> execute(context, "zeromass", 0))))
                .then(Commands.literal("listall")
                        .executes(DeleteMasslessShipsCommand::listAllShips));
    }
}