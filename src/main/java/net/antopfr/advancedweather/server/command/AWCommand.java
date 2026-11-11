package net.antopfr.advancedweather.server.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.antopfr.advancedweather.api.AdvancedWeatherAPI;
import net.antopfr.advancedweather.api.external.NominatimGeocoding;
import net.antopfr.advancedweather.api.external.OpenMeteo;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.network.toclient.ToggleDebugPacket;
import net.antopfr.advancedweather.weather.AtmosphericForcing;
import net.antopfr.advancedweather.weather.StormApproach;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.WeatherManager;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.antopfr.advancedweather.weather.effect.EffectManager;
import net.antopfr.advancedweather.weather.effect.types.rainbows.RainbowSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.antopfr.advancedweather.weather.WeatherManager.isCompatibleWithLevel;

public class AWCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("aw")
                .requires(source -> source.hasPermission(2))
                .executes(AWCommand::info)
                .then(history())
                .then(debug())
                .then(force())
                .then(seed())
                .then(set())
                .then(approach())
                .then(setLocation())
                .then(rainbow())
                .then(effect())
                .then(auto())
                .then(refresh());
    }

    // /aw
    private static int info(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        WeatherTypes current = WeatherManager.get(level).getCurrentWeather(level);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.aw.info", current.displayName()), false);
        return 1;
    }

    // /aw history
    private static LiteralArgumentBuilder<CommandSourceStack> history() {
        return Commands.literal("history")
                .executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    List<WeatherTypes> recent = AdvancedWeatherAPI.getRecentHistory(level);
                    if (recent.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.translatable("commands.aw.history.empty"), false);
                        return 1;
                    }
                    MutableComponent joined = Component.empty();
                    for (int i = 0; i < recent.size(); i++) {
                        if (i > 0) joined.append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY));
                        joined.append(recent.get(i).displayName().copy().withStyle(ChatFormatting.WHITE));
                    }
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.aw.history.list", joined), false);
                    return 1;
                });
    }

    // /aw debug
    private static LiteralArgumentBuilder<CommandSourceStack> debug() {
        return Commands.literal("debug")
                .executes(ctx -> {
                    PacketDistributor.sendToPlayer(
                            ctx.getSource().getPlayerOrException(), ToggleDebugPacket.INSTANCE);
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> force() {
        return Commands.literal("force")
                .then(Commands.argument("pressure", FloatArgumentType.floatArg(-100f, 100f))
                        .then(Commands.argument("dewpoint", FloatArgumentType.floatArg(-30f, 30f))
                                .then(Commands.argument("temp", FloatArgumentType.floatArg(-30f, 30f))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                                .executes(ctx -> doForcing(ctx, AtmosphericForcing.Bias.NONE, false))
                                                .then(Commands.argument("bias", StringArgumentType.word())
                                                        .suggests(AWCommand::suggestBias)
                                                        .executes(ctx -> {
                                                            AtmosphericForcing.Bias bias = parseBias(ctx);
                                                            return bias == null ? 0 : doForcing(ctx, bias, false);
                                                        }))))))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            WeatherManager.get(level).getAtmosphere(level).clearForcings();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.aw.force.cleared"), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> seed() {
        return Commands.literal("seed")
                .then(Commands.argument("pressure", FloatArgumentType.floatArg(-100f, 100f))
                        .then(Commands.argument("dewpoint", FloatArgumentType.floatArg(-30f, 30f))
                                .then(Commands.argument("temp", FloatArgumentType.floatArg(-30f, 30f))
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                                .then(Commands.argument("bias", StringArgumentType.word())
                                                        .suggests(AWCommand::suggestBias)
                                                        .executes(ctx -> {
                                                            AtmosphericForcing.Bias bias = parseBias(ctx);
                                                            return bias == null ? 0 : doForcing(ctx, bias, true);
                                                        }))))));
    }

    // /aw set <type>
    private static LiteralArgumentBuilder<CommandSourceStack> set() {
        return Commands.literal("set")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            for (WeatherTypes type : WeatherTypes.values()) {
                                if (isCompatibleWithLevel(type, level))
                                    builder.suggest(type.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String input = StringArgumentType.getString(ctx, "type");
                            WeatherTypes type = WeatherTypes.fromNameSafe(input);
                            if (!input.equalsIgnoreCase(type.name())) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("commands.aw.set.unknown", input));
                                return 0;
                            }
                            ServerLevel level = ctx.getSource().getLevel();
                            if (!isCompatibleWithLevel(type, level)) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("commands.aw.set.unknown", input));
                                return 0;
                            }
                            WeatherManager.get(level).setCurrentWeather(level, type);
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.aw.set.success", type.displayName()), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> approach() {
        return Commands.literal("approach")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            for (WeatherTypes type : WeatherTypes.values()) {
                                if (StormApproach.announces(type) && isCompatibleWithLevel(type, level))
                                    builder.suggest(type.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String input = StringArgumentType.getString(ctx, "type");
                            WeatherTypes type = WeatherTypes.fromNameSafe(input);
                            ServerLevel level = ctx.getSource().getLevel();
                            if (!input.equalsIgnoreCase(type.name()) || !isCompatibleWithLevel(type, level)) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("commands.aw.set.unknown", input));
                                return 0;
                            }
                            if (!StormApproach.announces(type)) {
                                ctx.getSource().sendFailure(
                                        Component.translatable("commands.aw.approach.unannounced", type.displayName()));
                                return 0;
                            }
                            WeatherManager.get(level).approach(level, type);
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.aw.approach.success",
                                            type.displayName(), StormApproach.lead(type) / 20), true);
                            return 1;
                        }));
    }

    // /aw setlocation <latitude> <longitude>
    private static LiteralArgumentBuilder<CommandSourceStack> setLocation() {
        return Commands.literal("setlocation")
                .then(Commands.argument("latitude", DoubleArgumentType.doubleArg(-90, 90))
                        .then(Commands.argument("longitude", DoubleArgumentType.doubleArg(-180, 180))
                                .executes(ctx -> {
                                    double lat = DoubleArgumentType.getDouble(ctx, "latitude");
                                    double lon = DoubleArgumentType.getDouble(ctx, "longitude");
                                    AWCommonConfig config = AWCommonConfig.get();
                                    config.latitude = lat;
                                    config.longitude = lon;
                                    config.save();

                                    NominatimGeocoding.invalidateCache();
                                    NominatimGeocoding.fetchLocationNameAsync(lat, lon);

                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.aw.setlocation", lat + ", " + lon), true);
                                    return 1;
                                })));
    }

    // /aw rainbow spawn|clear
    private static LiteralArgumentBuilder<CommandSourceStack> rainbow() {
        return Commands.literal("rainbow")
                .then(Commands.literal("spawn")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            if (!level.dimension().equals(Level.OVERWORLD)) {
                                ctx.getSource().sendFailure(Component.translatable("commands.aw.rainbow.overworld_only"));
                                return 0;
                            }
                            boolean showing = RainbowSpawner.spawn(level);
                            ctx.getSource().sendSuccess(() -> Component.translatable(showing
                                    ? "commands.aw.rainbow.spawned"
                                    : "commands.aw.rainbow.spawned_low_sun"), false);
                            return 1;
                        }))
                .then(Commands.literal("clear")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            boolean cleared = RainbowSpawner.clear(level);
                            ctx.getSource().sendSuccess(() -> Component.translatable(cleared
                                    ? "commands.aw.rainbow.cleared"
                                    : "commands.aw.rainbow.none"), false);
                            return 1;
                        }));
    }

    // /aw effect add|remove <effect>
    private static LiteralArgumentBuilder<CommandSourceStack> effect() {
        return Commands.literal("effect")
                .then(Commands.literal("add")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests(AWCommand::suggestEffects)
                                .executes(ctx -> applyEffect(ctx, true))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests(AWCommand::suggestEffects)
                                .executes(ctx -> applyEffect(ctx, false))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestEffects(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (WeatherEffects e : WeatherEffects.values()) builder.suggest(e.name().toLowerCase());
        return builder.buildFuture();
    }

    private static int applyEffect(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, boolean add) {
        String input = StringArgumentType.getString(ctx, "effect");
        WeatherEffects effect = WeatherEffects.fromNameSafe(input);
        if (effect == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.aw.effect.unknown", input));
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        if (add) EffectManager.get(level).addEffect(level, effect);
        else EffectManager.get(level).removeEffect(level, effect);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(add ? "commands.aw.effect.added" : "commands.aw.effect.removed",
                        effect.effectName()), true);
        return 1;
    }

    // /aw auto
    private static LiteralArgumentBuilder<CommandSourceStack> auto() {
        return Commands.literal("auto")
                .executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    WeatherManager.get(level).startAutoWeather(level);
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable("commands.aw.auto.started"), true);
                    return 1;
                });
    }

    // /aw refresh  (pull real weather now)
    private static LiteralArgumentBuilder<CommandSourceStack> refresh() {
        return Commands.literal("refresh")
                .executes(ctx -> {
                    AWCommonConfig config = AWCommonConfig.get();
                    if (!config.realWeatherEnabled) {
                        ctx.getSource().sendFailure(Component.translatable("commands.aw.refresh.disabled"));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable("commands.aw.refresh.fetching",
                                    config.latitude + ", " + config.longitude), false);

                    ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);
                    if (overworld == null) return 0;

                    CommandSourceStack source = ctx.getSource();
                    OpenMeteo.fetchAsync(config.latitude, config.longitude)
                            .thenAccept(type -> overworld.getServer().execute(() -> {
                                if (type == null) {
                                    source.sendFailure(Component.translatable("commands.aw.refresh.failedfetch"));
                                    return;
                                }
                                WeatherManager.get(overworld).applyRealWeather(overworld, type);
                                source.sendSuccess(() -> Component.translatable("commands.aw.refresh.applied",
                                        Component.literal(type.name() + " (").append(type.displayName()).append(")")), false);
                            }));
                    return 1;
                });
    }

    private static CompletableFuture<Suggestions> suggestBias(
            CommandContext<CommandSourceStack> ctx,
            SuggestionsBuilder builder) {
        for (AtmosphericForcing.Bias b : AtmosphericForcing.Bias.values())
            builder.suggest(b.name().toLowerCase());
        return builder.buildFuture();
    }

    private static AtmosphericForcing.Bias parseBias(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "bias");
        try {
            return AtmosphericForcing.Bias.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("commands.aw.force.unknown_bias", raw));
            return null;
        }
    }

    private static int doForcing(CommandContext<CommandSourceStack> ctx,
                                 AtmosphericForcing.Bias bias, boolean asSeed) {
        ServerLevel level = ctx.getSource().getLevel();
        float p = FloatArgumentType.getFloat(ctx, "pressure");
        float dp = FloatArgumentType.getFloat(ctx, "dewpoint");
        float t = FloatArgumentType.getFloat(ctx, "temp");
        int sec = IntegerArgumentType.getInteger(ctx, "seconds");

        if (asSeed) {
            WeatherManager.get(level).seed(level, bias, p, dp, t, sec * 20);
        } else {
            WeatherManager.get(level).getAtmosphere(level)
                    .applyForcing(p, dp, t, sec * 20, bias);
        }

        ctx.getSource().sendSuccess(() -> Component.translatable(
                asSeed ? "commands.aw.force.seeded" : "commands.aw.force.applied",
                String.format("%.0f", p), String.format("%.0f", dp), String.format("%.0f", t),
                sec, bias.name().toLowerCase()), true);
        return 1;
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(register());
    }
}
