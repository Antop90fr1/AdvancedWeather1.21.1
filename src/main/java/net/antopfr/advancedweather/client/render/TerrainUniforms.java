package net.antopfr.advancedweather.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import foundry.veil.api.compat.SodiumCompat;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.renderer.GameRenderer;
import net.antopfr.advancedweather.util.AWLogger;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;

import java.util.HashSet;
import java.util.Set;

public final class TerrainUniforms {

    private TerrainUniforms() {}

    private static final Set<String> broken = new HashSet<>();

    public static void retry() {
        broken.clear();
    }

    public static void set(String name, float... value) {
        if (broken.contains(name)) return;
        try {
            set(GameRenderer.getRendertypeSolidShader(), name, value);
            set(GameRenderer.getRendertypeCutoutShader(), name, value);
            set(GameRenderer.getRendertypeCutoutMippedShader(), name, value);

            if (SodiumCompat.isLoaded()) {
                IntIterator programs = SodiumCompat.INSTANCE.getLoadedShaders().values().iterator();
                while (programs.hasNext()) set(programs.nextInt(), name, value);
            }
        } catch (Throwable t) {
            fail(name, t);
        }
    }

    public static void setInt(String name, int value) {
        if (broken.contains(name)) return;
        try {
            setInt(GameRenderer.getRendertypeSolidShader(), name, value);
            setInt(GameRenderer.getRendertypeCutoutShader(), name, value);
            setInt(GameRenderer.getRendertypeCutoutMippedShader(), name, value);

            if (SodiumCompat.isLoaded()) {
                IntIterator programs = SodiumCompat.INSTANCE.getLoadedShaders().values().iterator();
                while (programs.hasNext()) setInt(programs.nextInt(), name, value);
            }
        } catch (Throwable t) {
            fail(name, t);
        }
    }

    private static void fail(String name, Throwable t) {
        broken.add(name);
        AWLogger.AW.error("[AdvancedWeather] Terrain uniform {} switched off, the rest keeps going", name, t);
    }

    private static void setInt(ShaderInstance shader, String name, int value) {
        if (shader != null) setInt(shader.getId(), name, value);
    }

    private static void setInt(int program, String name, int value) {
        if (program <= 0 || !GL20.glIsProgram(program)) return;
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) return;

        if (GL.getCapabilities().OpenGL41) {
            GL41.glProgramUniform1i(program, location, value);
        } else {
            int bound = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            GL20.glUseProgram(program);
            GL20.glUniform1i(location, value);
            GL20.glUseProgram(bound);
        }
    }

    private static void set(ShaderInstance shader, String name, float[] value) {
        if (shader == null) return;

        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            switch (value.length) {
                case 1 -> uniform.set(value[0]);
                case 2 -> uniform.set(value[0], value[1]);
                case 3 -> uniform.set(value[0], value[1], value[2]);
                default -> uniform.set(value[0], value[1], value[2], value[3]);
            }
        } else {
            set(shader.getId(), name, value);
        }
    }

    private static void set(int program, String name, float[] value) {
        if (program <= 0 || !GL20.glIsProgram(program)) return;
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) return;

        if (GL.getCapabilities().OpenGL41) {
            switch (value.length) {
                case 1 -> GL41.glProgramUniform1f(program, location, value[0]);
                case 2 -> GL41.glProgramUniform2f(program, location, value[0], value[1]);
                case 3 -> GL41.glProgramUniform3f(program, location, value[0], value[1], value[2]);
                default -> GL41.glProgramUniform4f(program, location, value[0], value[1], value[2], value[3]);
            }
        } else {
            int bound = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            GL20.glUseProgram(program);
            switch (value.length) {
                case 1 -> GL20.glUniform1f(location, value[0]);
                case 2 -> GL20.glUniform2f(location, value[0], value[1]);
                case 3 -> GL20.glUniform3f(location, value[0], value[1], value[2]);
                default -> GL20.glUniform4f(location, value[0], value[1], value[2], value[3]);
            }
            GL20.glUseProgram(bound);
        }
    }
}
