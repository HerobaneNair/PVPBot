package hero.bane.pvpbot;

import hero.bane.pvpbot.rule.Rule;
import hero.bane.pvpbot.rule.RuleConfigIO;
import hero.bane.pvpbot.rule.RuleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.File;

public final class PVPBotSettings {

    private PVPBotSettings() {
    }

    public static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "pvpbot.json");

    public static void init() {
        RuleRegistry.register(PVPBotSettings.class);
        RuleConfigIO.load(CONFIG_FILE);
    }

    @Rule(desc = "Spawn offline players in online mode if online-mode player with specified name does not exist")
    public static boolean allowSpawningOfflinePlayers = true;

    @Rule(desc = "Allows listing fake players on the multiplayer screen")
    public static boolean allowListingFakePlayers = true;

    @Rule(desc = "Creative No Clip")
    public static boolean creativeNoClip = false;

    @Rule(desc = "Creative flying speed multiplier (Default 1.0)")
    public static double creativeFlySpeed = 1.0;

    @Rule(desc = "Creative air drag (Default 0.09)")
    public static double creativeFlyDrag = 0.09;

    @Rule(desc = "Enables shield stunning, where the entity can be damaged immediately after the shield is disabled")
    public static boolean shieldStunning = false;

    @Rule(desc = "Enables editing player nbt, so you can directly edit values within a player's data")
    public static boolean editablePlayerNbt = false;

    @Rule(desc = "Smooth client animations with low tps settings")
    public static boolean smoothClientAnimations = false;

    @Rule(desc = "Allows intentional game design explosions (from beds and respawn anchors) to not explode with fire")
    public static boolean explosionNoFire = false;

    @Rule(desc = "Players absorb XP instantly, without delay")
    public static boolean xpNoCooldown = false;

    public enum ExplosionNoDmgMode {
        TRUE, FALSE, MOST;

        public boolean enabled() {
            return this != FALSE;
        }
    }

    @Rule(desc = "Explosions won't destroy blocks")
    public static ExplosionNoDmgMode explosionNoBlockDamage = ExplosionNoDmgMode.FALSE;

    public static boolean isCreativeFlying(Entity entity) {
        return creativeNoClip && entity instanceof Player player && player.isCreative() && player.getAbilities().flying;
    }
}
