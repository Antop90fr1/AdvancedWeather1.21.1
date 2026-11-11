package net.antopfr.advancedweather.client.command;

import net.antopfr.advancedweather.client.ClientLocalHistory;
import net.antopfr.advancedweather.client.debug.MapLocationSelectorScreen;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public class AWClientCommand {

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("aw")
                        // /aw map -> open the location picker (edits the client's config copy)
                        .then(Commands.literal("map")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.execute(() -> mc.setScreen(new MapLocationSelectorScreen()));
                                    return 1;
                                }))
                        // /aw reset -> clear the client's cached weather state and local history
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    ClientWeatherState.reset();
                                    ClientLocalHistory.clear();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.aw.reset.success"), false);
                                    return 1;
                                }))
        );
    }
}
