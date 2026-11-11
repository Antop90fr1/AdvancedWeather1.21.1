package net.antopfr.advancedweather.compat.sable;

import com.mojang.blaze3d.shaders.Uniform;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.opengl.GL20;

public final class ShipCover {

    private ShipCover() {}

    private static final String UNIFORM = "AwFrostShip";

    public static void drawingShip(ClientSubLevel ship, Vector3dc origin, ShaderInstance shader,
                                   double camX, double camY, double camZ) {
        Pose3dc pose = ship.renderPose();
        Quaterniondc turn = pose.orientation();
        Vector3d pivot = turn.transform(new Vector3d(pose.rotationPoint()).sub(origin));
        Vector3d offset = new Vector3d(pose.position()).sub(pivot).sub(camX, camY, camZ);
        turn.transformInverse(offset);
        set(shader, (float) offset.x, (float) offset.y, (float) offset.z, 1f);
    }

    public static void backToWorld(ShaderInstance shader) {
        set(shader, 0f, 0f, 0f, 0f);
    }

    private static void set(ShaderInstance shader, float x, float y, float z, float w) {
        Uniform uniform = shader.getUniform(UNIFORM);
        if (uniform != null) {
            uniform.set(x, y, z, w);
            uniform.upload();
            return;
        }
        int location = GL20.glGetUniformLocation(shader.getId(), UNIFORM);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }
}
