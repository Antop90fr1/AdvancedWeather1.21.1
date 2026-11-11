package net.antopfr.advancedweather.content.item;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.antopfr.advancedweather.content.item.almanac.WeatherAlmanacItem;
import net.antopfr.advancedweather.content.item.crystals.ClearCrystalItem;
import net.antopfr.advancedweather.content.item.crystals.FireCrystalItem;
import net.antopfr.advancedweather.content.item.crystals.FrostCrystalItem;
import net.antopfr.advancedweather.content.item.crystals.RainCrystalItem;
import net.antopfr.advancedweather.content.item.kite.KiteItem;
import net.antopfr.advancedweather.util.AWRegistrate;
import net.antopfr.advancedweather.weather.AtmosphericForcing;
import net.minecraft.world.item.Item;

@SuppressWarnings("unused")
public class AWItems {

    public static final ItemEntry<CalibrationToolItem> CALIBRATION_TOOL = AWRegistrate.get()
            .item("calibration_tool", CalibrationToolItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .register();

    public static final ItemEntry<HammerItem> HAMMER = AWRegistrate.get()
            .item("hammer", HammerItem::new)
            .properties(p -> p.stacksTo(1).durability(128))
            .register();

    public static final ItemEntry<BarometerItem> PORTABLE_BAROMETER = AWRegistrate.get()
            .item("portable_barometer", BarometerItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<AnemometerItem> PORTABLE_ANEMOMETER = AWRegistrate.get()
            .item("portable_anemometer", AnemometerItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<ThermometerItem> PORTABLE_THERMOMETER = AWRegistrate.get()
            .item("portable_thermometer", ThermometerItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<HygrometerItem> PORTABLE_HYGROMETER = AWRegistrate.get()
            .item("portable_hygrometer", HygrometerItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<KiteItem> KITE_ITEM = AWRegistrate.get()
            .item("kite", KiteItem::new)
            .initialProperties(() -> new Item.Properties().stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<WeatherReportItem> WEATHER_REPORT = AWRegistrate.get()
            .item("weather_report", WeatherReportItem::new)
            .properties(p -> p.stacksTo(16))
            .register();

    public static final ItemEntry<WeatherAlmanacItem> WEATHER_ALMANAC = AWRegistrate.get()
            .item("weather_almanac", WeatherAlmanacItem::new)
            .properties(p -> p.stacksTo(1))
            .model((ctx, prov) -> {})
            .register();

    public static final ItemEntry<InfoItem> DIAL = AWRegistrate.get()
            .item("dial", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.dial"))
            .properties(p -> p.stacksTo(16))
            .register();

    public static final ItemEntry<InfoItem> RAW_CINNABAR = AWRegistrate.get()
            .item("raw_cinnabar", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.raw_cinnabar"))
            .register();

    public static final ItemEntry<InfoItem> CINNABAR_DUST = AWRegistrate.get()
            .item("cinnabar_dust", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.cinnabar_dust"))
            .register();

    public static final ItemEntry<InfoItem> WASHED_CINNABAR_DUST = AWRegistrate.get()
            .item("washed_cinnabar_dust", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.washed_cinnabar_dust"))
            .register();

    public static final ItemEntry<InfoItem> ALUMINA = AWRegistrate.get()
            .item("alumina", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.alumina"))
            .register();

    public static final ItemEntry<InfoItem> ALUMINA_CARBON_MIX = AWRegistrate.get()
            .item("alumina_carbon_mix", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.alumina_carbon_mix"))
            .register();

    public static final ItemEntry<InfoItem> ALUMINUM_INGOT = AWRegistrate.get()
            .item("aluminum_ingot", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.aluminum_ingot"))
            .register();

    public static final ItemEntry<Item> ALUMINUM_NUGGET = AWRegistrate.get()
            .item("aluminum_nugget", Item::new)
            .register();

    public static final ItemEntry<InfoItem> ALUMINUM_SHEET = AWRegistrate.get()
            .item("aluminum_sheet", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.aluminum_sheet"))
            .register();

    public static final ItemEntry<InfoItem> EMPTY_VIAL = AWRegistrate.get()
            .item("empty_vial", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.empty_vial"))
            .register();

    public static final ItemEntry<InfoItem> MERCURY_VIAL = AWRegistrate.get()
            .item("mercury_vial", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.mercury_vial"))
            .properties(p -> p.stacksTo(16))
            .register();

    public static final ItemEntry<InfoItem> SPRING = AWRegistrate.get()
            .item("spring", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.spring"))
            .register();

    public static final ItemEntry<InfoItem> CALIBRATED_SPRING = AWRegistrate.get()
            .item("calibrated_spring", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.calibrated_spring"))
            .register();

    public static final ItemEntry<InfoItem> CUP_ROTOR = AWRegistrate.get()
            .item("cup_rotor", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.cup_rotor"))
            .register();

    public static final ItemEntry<InfoItem> CALIBRATED_CUP_ROTOR = AWRegistrate.get()
            .item("calibrated_cup_rotor", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.calibrated_cup_rotor"))
            .register();

    public static final ItemEntry<InfoItem> HORSEHAIR = AWRegistrate.get()
            .item("horsehair", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.horsehair"))
            .register();

    public static final ItemEntry<InfoItem> SENSITIVE_FIBER = AWRegistrate.get()
            .item("sensitive_fiber", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.sensitive_fiber"))
            .register();

    public static final ItemEntry<InfoItem> CALIBRATED_SENSITIVE_FIBER = AWRegistrate.get()
            .item("calibrated_sensitive_fiber", p -> new InfoItem(p,
                    "advancedweather.item_tooltip.calibrated_sensitive_fiber"))
            .register();

    public static final ItemEntry<Item> WEATHER_DISPLAY = AWRegistrate.get()
            .item("weather_display", Item::new)
            .register();

    public static final ItemEntry<Item> WIRELESS_MODULE = AWRegistrate.get()
            .item("wireless_module", Item::new)
            .register();


    // CRYSTALS
    public static final ItemEntry<Item> DORMANT_CRYSTAL = AWRegistrate.get()
            .item("dormant_crystal", Item::new)
            .properties(Item.Properties::fireResistant)
            .register();

    public static final ItemEntry<FireCrystalItem> FIRE_CRYSTAL = AWRegistrate.get()
            .item("fire_crystal", FireCrystalItem::new)
            .properties(Item.Properties::fireResistant)
            .register();

    public static final ItemEntry<FrostCrystalItem> FROST_CRYSTAL = AWRegistrate.get()
            .item("frost_crystal", FrostCrystalItem::new)
            .register();

    public static final ItemEntry<RainCrystalItem> RAIN_CRYSTAL = AWRegistrate.get()
            .item("rain_crystal", RainCrystalItem::new)
            .properties(Item.Properties::fireResistant)
            .register();

    public static final ItemEntry<ClearCrystalItem> CLEAR_CRYSTAL = AWRegistrate.get()
            .item("clear_crystal", ClearCrystalItem::new)
            .register();

    // ROCKETS
    public static final ItemEntry<SeedingRocketItem> SEEDING_ROCKET_WARM = AWRegistrate.get()
            .item("seeding_rocket_warm", p -> new SeedingRocketItem(p,
                    AtmosphericForcing.Bias.HEATING, +6f, +2f, +12f, 6000))
            .register();

    public static final ItemEntry<SeedingRocketItem> SEEDING_ROCKET_FROST = AWRegistrate.get()
            .item("seeding_rocket_frost", p -> new SeedingRocketItem(p,
                    AtmosphericForcing.Bias.COOLING, -6f, -2f, -12f, 6000))
            .register();

    public static final ItemEntry<SeedingRocketItem> SEEDING_ROCKET_RAIN = AWRegistrate.get()
            .item("seeding_rocket_rain", p -> new SeedingRocketItem(p,
                    AtmosphericForcing.Bias.SEEDING, -18f, +8f, 0f, 2400))
            .register();

    public static final ItemEntry<SeedingRocketItem> SEEDING_ROCKET_CLEAR = AWRegistrate.get()
            .item("seeding_rocket_clear", p -> new SeedingRocketItem(p,
                    AtmosphericForcing.Bias.DISSIPATING, +18f, -8f, 0f, 2400))
            .register();

    public static void register() {}
}
