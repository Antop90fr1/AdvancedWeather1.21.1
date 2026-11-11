package net.antopfr.advancedweather.content.item.kite;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public class KiteArmPose {

    public static final EnumProxy<HumanoidModel.ArmPose> POSE_PARAMETERS =
            new EnumProxy<>(HumanoidModel.ArmPose.class,
                    false,
                    (IArmPoseTransformer) KiteArmPose::apply);

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        KiteHold.pose(model, entity, arm);
    }

    public static HumanoidModel.ArmPose get() {
        return POSE_PARAMETERS.getValue();
    }
}
