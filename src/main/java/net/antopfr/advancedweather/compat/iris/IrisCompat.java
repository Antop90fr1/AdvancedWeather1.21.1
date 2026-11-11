package net.antopfr.advancedweather.compat.iris;

import net.antopfr.advancedweather.util.ModLoaded;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class IrisCompat {

    private IrisCompat() {}

    private static final MethodHandle IN_USE = find();

    public static boolean shaderPackInUse() {
        if (IN_USE == null) return false;
        try {
            return (boolean) IN_USE.invoke();
        } catch (Throwable e) {
            return false;
        }
    }

    private static MethodHandle find() {
        if (!ModLoaded.isIrisLoaded()) return null;
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return MethodHandles.publicLookup()
                    .findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class))
                    .bindTo(instance);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
