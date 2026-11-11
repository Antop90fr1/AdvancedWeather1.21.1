package net.antopfr.advancedweather.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import foundry.veil.api.compat.SodiumCompat;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;

public final class TerrainUniforms {

    private TerrainUniforms() {}

    public static void set(String name, float... value) {
        set(GameRenderer.getRendertypeSolidShader(), name, value);
        set(GameRenderer.getRendertypeCutoutShader(), name, value);
        set(GameRenderer.getRendertypeCutoutMippedShader(), name, value);

        if (SodiumCompat.isLoaded()) {
            IntIterator programs = SodiumCompat.INSTANCE.getLoadedShaders().values().iterator();
            while (programs.hasNext()) set(programs.nextInt(), name, value);
        }
    }

    private static void set(ShaderInstance shader, String name, float[] value) {
        if (shader == null) return;

        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            if (value.length == 3) uniform.set(value[0], value[1], value[2]);
            else uniform.set(value[0], value[1], value[2], value[3]);
        } else {
            set(shader.getId(), name, value);
        }
    }

    private static void set(int program, String name, float[] value) {
        if (program <= 0 || !GL20.glIsProgram(program)) return;
        int location = GL20.glGetUniformLocation(program, name);
        if (location < 0) return;

        if (GL.getCapabilities().OpenGL41) {
            if (value.length == 3) GL41.glProgramUniform3f(program, location, value[0], value[1], value[2]);
            else GL41.glProgramUniform4f(program, location, value[0], value[1], value[2], value[3]);
        } else {
            int bound = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            GL20.glUseProgram(program);
            if (value.length == 3) GL20.glUniform3f(location, value[0], value[1], value[2]);
            else GL20.glUniform4f(location, value[0], value[1], value[2], value[3]);
            GL20.glUseProgram(bound);
        }
    }
}
