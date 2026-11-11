package net.antopfr.advancedweather.server.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = "advancedweather")
public class WeatherShelterHandler {

    private static final int NONE = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;
        if (mob instanceof Enemy) return;

        if (!(mob.getNavigation() instanceof GroundPathNavigation)) return;

        int idle = idlePriority(mob);
        if (idle == NONE) return;
        mob.goalSelector.addGoal(idle - 1, new SeekShelterGoal(mob));
    }

    private static int idlePriority(PathfinderMob mob) {
        int stroll = NONE;
        int lastMove = NONE;
        for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof SeekShelterGoal) return NONE;
            if (goal.getGoal() instanceof RandomStrollGoal) {
                stroll = stroll == NONE ? goal.getPriority() : Math.min(stroll, goal.getPriority());
            }
            if (goal.getFlags().contains(Goal.Flag.MOVE)) {
                lastMove = Math.max(lastMove, goal.getPriority());
            }
        }
        return stroll != NONE ? stroll : lastMove;
    }
}
