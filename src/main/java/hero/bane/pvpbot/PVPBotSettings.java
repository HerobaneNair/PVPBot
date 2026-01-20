package hero.bane.pvpbot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class PVPBotSettings
{
    private PVPBotSettings() {}

    public static boolean allowSpawningOfflinePlayers = true;
    public static boolean allowListingFakePlayers = true;

    public static boolean creativeNoClip = false;
    public static double creativeFlySpeed = 1.0;
    public static double creativeFlyDrag = 0.09;
    public static boolean isCreativeFlying(Entity entity)
    {
        return creativeNoClip
                && entity instanceof Player player
                && player.isCreative()
                && player.getAbilities().flying;
    }

    public static boolean shieldStunning = false;
    public static boolean editablePlayerNbt = false;

    public static boolean explosionNoFire = false;
    public enum ExplosionNoDmgMode
    {
        TRUE,
        FALSE,
        MOST;
        public boolean enabled()
        {
            return this != FALSE;
        }
    }
    public static ExplosionNoDmgMode explosionNoBlockDamage = ExplosionNoDmgMode.FALSE;
}
