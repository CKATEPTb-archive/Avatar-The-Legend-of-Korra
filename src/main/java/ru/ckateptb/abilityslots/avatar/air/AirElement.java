package ru.ckateptb.abilityslots.avatar.air;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;

import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class AirElement extends AbstractAbilityCategory {
    @ConfigField
    @Getter
    private static long revertTime = 60000;
    private final String name = "Air";
    private String displayName = "§7Air";
    private String prefix = "§7";

    public static void display(Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ) {
        display(location, amount, extra, offsetX, offsetY, offsetZ, ThreadLocalRandom.current().nextInt(2) == 0);
    }

    public static void display(Location location, int amount, float offsetX, float offsetY, float offsetZ, boolean playSound) {
        display(location, amount, 0, offsetX, offsetY, offsetZ, playSound);
    }

    public static void display(Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ, boolean playSound) {
        World world = location.getWorld();
        if (world != null) {
            if (location.getBlock().isLiquid()) {
                Particle.BUBBLE_COLUMN_UP.display(location, amount, offsetX, offsetY, offsetZ, extra);
                if (playSound) {
                    world.playSound(location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1, 2);
                }
                return;
            }
            if (playSound) {
                world.playSound(location, Sound.ENTITY_CREEPER_HURT, 1, 2);
            }
        }
        Particle.SPELL.display(location, amount, offsetX, offsetY, offsetZ, extra);
    }

    public static void sound(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.playSound(location, Sound.ENTITY_CREEPER_HURT, 1, 2);
        }
    }

    public static void display(Location location, int amount, float offsetX, float offsetY, float offsetZ) {
        display(location, amount, 0, offsetX, offsetY, offsetZ);
    }

    public static void handleBlockInteractions(AbilityUser user, Block block) {
        Location location = block.getLocation();
        if (!user.canUse(location)) return;
        Material type = block.getType();
        if (type == Material.LAVA) {
            new TemporaryBlock(location, Material.OBSIDIAN.createBlockData(), revertTime);
        }
        if (type == Material.FIRE || type == Material.SOUL_FIRE) {
            new TemporaryBlock(location, Material.AIR.createBlockData(), revertTime);
        }
    }

}
