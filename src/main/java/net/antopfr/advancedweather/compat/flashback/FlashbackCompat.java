package net.antopfr.advancedweather.compat.flashback;

import net.antopfr.advancedweather.util.AWLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class FlashbackCompat {

    private FlashbackCompat() {}

    private static boolean looked;
    private static MethodHandle editorShown;
    private static MethodHandle exporting;

    public static boolean editorPreview() {
        if (!looked) find();
        if (editorShown == null) return false;
        try {
            return (boolean) editorShown.invokeExact() && !(boolean) exporting.invokeExact();
        } catch (Throwable t) {
            editorShown = null;
            AWLogger.AW.warn("[AdvancedWeather] Flashback editor check failed, overlays stay on: {}", t.toString());
            return false;
        }
    }

    private static void find() {
        looked = true;
        try {
            ClassLoader loader = FlashbackCompat.class.getClassLoader();
            Class<?> ui = Class.forName("com.moulberry.flashback.editor.ui.ReplayUI", false, loader);
            Class<?> flashback = Class.forName("com.moulberry.flashback.Flashback", false, loader);
            MethodType flag = MethodType.methodType(boolean.class);
            editorShown = MethodHandles.publicLookup().findStatic(ui, "isActive", flag);
            exporting = MethodHandles.publicLookup().findStatic(flashback, "isExporting", flag);
            AWLogger.AW.info("[AdvancedWeather] Flashback detected, weather overlays hidden in its editor preview");
        } catch (ReflectiveOperationException | LinkageError e) {
            editorShown = null;
        }
    }
}
