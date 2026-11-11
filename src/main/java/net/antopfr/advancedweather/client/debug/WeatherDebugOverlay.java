package net.antopfr.advancedweather.client.debug;

import net.antopfr.advancedweather.api.external.NominatimGeocoding;
import net.antopfr.advancedweather.client.state.ClientAtmosphereState;
import net.antopfr.advancedweather.client.state.ClientWeatherState;
import net.antopfr.advancedweather.config.AWCommonConfig;
import net.antopfr.advancedweather.weather.WeatherEffects;
import net.antopfr.advancedweather.weather.effect.types.rainbows.RainbowRenderer;
import net.antopfr.advancedweather.weather.FeelsLikeCalculation;
import net.antopfr.advancedweather.client.wind.ClientWind;
import net.antopfr.advancedweather.weather.wind.WindScale;
import net.antopfr.advancedweather.weather.WeatherTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "advancedweather", value = Dist.CLIENT)
public class WeatherDebugOverlay {

    private static boolean enabled = false;

    public static void toggle() { enabled = !enabled; }
    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!(mc.level instanceof ClientLevel clientLevel)) return;

        WeatherTypes current  = ClientWeatherState.getCurrentWeather();
        WeatherTypes previous = ClientWeatherState.getPreviousWeather();
        float transition      = ClientWeatherState.getTransitionProgress();

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;

        int x = 4;
        int y = 4;
        int lineH = 10;
        int pad = 4;
        int panelW = 220;

        // ── Calcule dynamiquement le nombre de lignes pour la hauteur ──────────
        Set<WeatherEffects> activeEffects = ClientWeatherState.getActiveEffects();
        float rainbow = RainbowRenderer.intensity(mc.level, 1f);

        int baseLines = 21; // lignes fixes garanties présentes
        int effectLines = activeEffects.isEmpty() ? 1 : 1 + (int) Math.ceil(activeEffects.size() / 2.0);
        int rainbowLines = rainbow > 0f ? 1 : 0;
        int panelH = lineH * (baseLines + effectLines + rainbowLines) + pad * 2 + 4;
        g.fill(x, y, x + panelW, y + panelH, 0xAA000000);
        int borderColor = getPressureCategoryColor(current);
        g.fill(x, y, x + 2, y + panelH, borderColor);

        x += pad + 2;
        y += pad;

        // ── Titre + Dimension ────────────────────────────────────────────────
        String dimLabel = getDimensionLabel(mc.level);
        g.drawString(font, "§bAdvancedWeather §7Debug " + dimLabel, x, y, 0xFFFFFF, false);
        y += lineH + 2;

        // ── Type météo ───────────────────────────────────────────────────────
        g.drawString(font, "§7Weather  " + getWeatherColorCode(current) + current.name(),
                x, y, 0xFFFFFF, false);
        y += lineH;

        // ── Transition ───────────────────────────────────────────────────────
        if (transition < 1.0f) {
            String pct = String.format("%.0f%%", transition * 100);
            g.drawString(font, "§7Transition §e" + previous.name()
                    + " §7→ §f" + current.name()
                    + " §8(" + pct + ")", x, y, 0xFFFFFF, false);
        } else {
            g.drawString(font, "§7Transition §astable", x, y, 0xFFFFFF, false);
        }
        y += lineH;

        // ── Pression ──────────────────────────────────────────────────────────
        float localPressure = ClientAtmosphereState.getLocalPressure();
        float globalPressure = ClientAtmosphereState.getPressure();
        float trend         = ClientAtmosphereState.getTrend() * 20 * 60;
        float forecast30    = ClientAtmosphereState.getForecast30();
        float w             = ClientAtmosphereState.getWindIntensity();
        float wKmh          = ClientWind.regionalKmh();
        String category     = ClientAtmosphereState.getCategory();
        String trendSym     = trend > 0.002f ? "§a↑" : trend < -0.002f ? "§c↓" : "§7→";

        g.drawString(font, "§7Pressure §f" + String.format("%.1f", localPressure) + " hPa"
                + " §8[" + colorCategory(category) + "§8]", x, y, 0xFFFFFF, false);
        y += lineH;

        g.drawString(font, "§7§o(global: " + String.format("%.1f", globalPressure) + " hPa)",
                x, y, 0x999999, false);
        y += lineH;

        g.drawString(font, "§7Trend    " + trendSym + " §f" + String.format("%.2f", trend) + " hPa/min", x, y, 0xFFFFFF, false);
        y += lineH;

        g.drawString(font, "§7+30min   §f" + String.format("%.1f", forecast30) + " hPa", x, y, 0xFFFFFF, false);
        y += lineH;

        g.drawString(font, "§7Next     §e" + ClientAtmosphereState.getPredictedNext()
                + " §8(" + String.format("%.0f%%", ClientAtmosphereState.getConfidenceNext()) + ")", x, y, 0xFFFFFF, false);
        y += lineH;
        g.drawString(font, "§7In 30min §e" + ClientAtmosphereState.getPredictedIn30min()
                + " §8(" + String.format("%.0f%%", ClientAtmosphereState.getConfidenceIn30()) + ")", x, y, 0xFFFFFF, false);
        y += lineH;

        // ── Vent ──────────────────────────────────────────────────────────────
        g.drawString(font, "§7Wind     §d" + String.format("%.3f", w)
                + " §7(§c" + String.format("%.1f", wKmh) + " km/h§7)", x, y, 0xFFFFFF, false);
        y += lineH;
        g.drawString(font, "§7Beaufort §f" + WindScale.beaufort(wKmh).getString(), x, y, 0xFFFFFF, false);
        y += lineH;

        // ── Température & Humidité ───────────────────────────────────────────
        float currentTemp     = ClientAtmosphereState.getLocalTemperature();
        float currentTempFcst = ClientAtmosphereState.getLocalTemperatureForecast();
        float currentHum      = ClientAtmosphereState.getLocalHumidity();
        float currentHumFcst  = ClientAtmosphereState.getLocalHumidityForecast();
        float feelsLike       = FeelsLikeCalculation.calculate(currentTemp, wKmh, currentHum);
        String comfort        = FeelsLikeCalculation.getComfortLabel(feelsLike);

        String tempColor = currentTemp <= -20f ? "§5" : currentTemp <= 0f ? "§1" : currentTemp < 15f ? "§b" : currentTemp < 30f ? "§e" : currentTemp < 55f ? "§c" : "§4";

        g.drawString(font, "§7Temp     " + tempColor + String.format("%.1f°C", currentTemp)
                + " §8(feels " + String.format("%.1f°C", feelsLike) + ")", x, y, 0xFFFFFF, false);
        y += lineH;
        g.drawString(font, "§7§o" + comfort + " §8| +30min: " + String.format("%.1f°C", currentTempFcst),
                x, y, 0x999999, false);
        y += lineH;
        g.drawString(font, "§7Humidity §9" + String.format("%.0f%%", currentHum)
                + " §8(+30min: " + String.format("%.0f%%", currentHumFcst) + ")", x, y, 0xFFFFFF, false);
        y += lineH;

        // ── Mode ──────────────────────────────────────────────────────────────
        g.drawString(font, "§7Mode     " + colorMode(ClientAtmosphereState.getMode()), x, y, 0xFFFFFF, false);
        y += lineH;

        // ── Effets actifs ────────────────────────────────────────────────────
        g.drawString(font, "§7Active Effects:", x, y, 0xFFFFFF, false);
        y += lineH;
        if (activeEffects.isEmpty()) {
            g.drawString(font, "  §8none", x, y, 0xFFFFFF, false);
            y += lineH;
        } else {
            List<String> names = activeEffects.stream()
                    .map(Enum::name)
                    .sorted()
                    .toList();
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                line.append("§a").append(names.get(i).toLowerCase());
                if (i < names.size() - 1) line.append("§7, ");
                if ((i + 1) % 2 == 0 || i == names.size() - 1) {
                    g.drawString(font, "  " + line, x, y, 0xFFFFFF, false);
                    y += lineH;
                    line = new StringBuilder();
                }
            }
        }

        // ── Rainbows à proximité ──────────────────────────────────────────────
        if (rainbow > 0f) {
            g.drawString(font, "§7Rainbow  §dintensity=" + String.format("%.2f", rainbow), x, y, 0xFFFFFF, false);
            y += lineH;
        }

        // ── Localisation ──────────────────────────────────────────────────────
        AWCommonConfig config = AWCommonConfig.get();
        double lat = config.latitude;
        double lon = config.longitude;
        String locationName = NominatimGeocoding.getCachedLocation();

        g.drawString(font, "§7Location  §6" + locationName, x, y, 0xFFFFFF, false);
        y += lineH;
        g.drawString(font, "§7Coords    §f" + String.format("%.4f", lat) + ", " + String.format("%.4f", lon), x, y, 0xFFFFFF, false);
        y += lineH;

        var biome = clientLevel.getBiome(mc.player.blockPosition());
        String biomeName = biome.unwrapKey()
                .map(k -> k.location().getPath())
                .orElse("unknown");
        g.drawString(font, "§7Biome    §f" + biomeName, x, y, 0xFFFFFF, false);
        y += lineH;

        long time = clientLevel.getDayTime() % 24000;
        boolean isNight = time > 13000 && time < 23000;
        String dayNightSym = isNight ? "§9☾" : "§e☀";
        g.drawString(font, "§7DayTime  §f" + time + " §8(" + toTimeString(time) + ") " + dayNightSym, x, y, 0xFFFFFF, false);
        y += lineH;

        g.drawString(font, "§7Player Y §f" + String.format("%.1f", mc.player.getY()), x, y, 0xFFFFFF, false);
    }

    private static String getDimensionLabel(Level level) {
        if (level.dimension().equals(Level.NETHER)) return "§c[Nether]";
        if (level.dimension().equals(Level.END))    return "§5[End]";
        return "§b[Overworld]";
    }

    private static String getWeatherColorCode(WeatherTypes type) {
        return switch (type) {
            case THUNDERSTORM, HAIL, BLIZZARD             -> "§9";
            case HEAVY_RAIN, FREEZING_RAIN                -> "§3";
            case LIGHT_RAIN, DRIZZLE, SNOW                -> "§b";
            case CLEAR, SUNNY                             -> "§e";
            case NETHERSTORM, BRIMSTONE_STORM, HELLFIRE   -> "§c";
            case LAVA_RAIN, ASH_STORM                      -> "§6";
            case VOID_STORM, ENDERSTORM, CHORUS_GALE       -> "§5";
            case END_MIST                                  -> "§d";
            default                                        -> "§f";
        };
    }

    private static int getPressureCategoryColor(WeatherTypes type) {
        return switch (type) {
            case THUNDERSTORM, HAIL, BLIZZARD -> 0xFF5555FF;
            case HEAVY_RAIN, FREEZING_RAIN    -> 0xFF5599FF;
            case LIGHT_RAIN, DRIZZLE, SNOW    -> 0xFF88BBFF;
            case CLEAR, WINDY, SUNNY          -> 0xFFFFAA00;
            case NETHERSTORM, BRIMSTONE_STORM -> 0xFF8B0000;
            case LAVA_RAIN                     -> 0xFFFF4500;
            case HELLFIRE                      -> 0xFF5A0000;
            case VOID_STORM, ENDERSTORM        -> 0xFF2E0854;
            case CHORUS_GALE                   -> 0xFF4B0082;
            case ASH_STORM, END_MIST           -> 0xFF887766;
            default                            -> 0xFF888888;
        };
    }

    private static String colorCategory(String category) {
        return switch (category) {
            case "HIGH"    -> "§aHIGH";
            case "NEUTRAL" -> "§eNEUTRAL";
            case "LOW"     -> "§cLOW";
            case "STORM"   -> "§4STORM";
            default        -> "§7" + category;
        };
    }

    private static String colorMode(String mode) {
        return switch (mode) {
            case "PROCEDURAL" -> "§a§oPROCEDURAL";
            case "REAL"       -> "§9§oREAL";
            case "MANUAL"     -> "§c§oMANUAL";
            default           -> "§7" + mode;
        };
    }

    private static String toTimeString(long dayTime) {
        int hours   = (int)((dayTime + 6000) / 1000 % 24);
        int minutes = (int)((dayTime % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }
}