package ru.ckateptb.abilityslots.avatar.air;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.particle.ParticleEffect;
import ru.ckateptb.abilityslots.user.AbilityUser;

@Getter
@Setter
public class AirElement extends AbstractAbilityCategory {
    private final String name = "Air";
    private String displayName = "§7Air";
    private String prefix = "§7";

    public static void display(Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ) {
        display(location, amount, extra, offsetX, offsetY, offsetZ, true);
    }

    public static void display(Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ, boolean playSound) {
        World world = location.getWorld();
        if (world != null) {
            if (location.getBlock().isLiquid()) {
                ParticleEffect.BUBBLE_COLUMN_UP.display(location, amount, offsetX, offsetY, offsetZ, extra);
                if (playSound) {
                    world.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1, 2);
                }
                return;
            }
            if (playSound) {
                world.playSound(location, Sound.ENTITY_CREEPER_HURT, 1, 2);
            }
        }
        ParticleEffect.SPELL.display(location, amount, offsetX, offsetY, offsetZ);
    }

    public static void display(Location location, int amount, float offsetX, float offsetY, float offsetZ) {
        display(location, amount, 0, offsetX, offsetY, offsetZ);
    }

    public static void handleBlockInteractions(AbilityUser user, Block block) {

    }
}
