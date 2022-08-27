package ru.ckateptb.abilityslots.avatar.fire;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.avatar.fire.ability.passive.BlueFire;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.abilityslots.common.util.MaterialUtils;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.particle.Particle;
import ru.ckateptb.tablecloth.temporary.block.TemporaryBlock;

@Getter
@Setter
public class FireElement extends AbstractAbilityCategory {
    @Getter
    @ConfigField
    private static long maxFireDuration = 10000;
    @Getter
    @ConfigField
    private static long minFireDuration = 5000;

    private final String name = "Fire";
    private String displayName = "§4Fire";
    private String prefix = "§4";

    public static void display(AbilityUser user, Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ) {
        display(user, location, amount, extra, offsetX, offsetY, offsetZ, ThreadLocalRandom.current().nextInt(2) == 0);
    }

    public static void display(AbilityUser user, Location location, int amount, float offsetX, float offsetY, float offsetZ, boolean playSound) {
        display(user, location, amount, 0, offsetX, offsetY, offsetZ, playSound);
    }

    public static void display(AbilityUser user, Location location, int amount, double extra, float offsetX, float offsetY, float offsetZ, boolean playSound) {
        World world = location.getWorld();
        if (playSound) {
            world.playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1, 1);
        }
        (isBlueFireBender(user) ? Particle.SOUL_FIRE_FLAME : Particle.FLAME).display(location, amount, offsetX, offsetY, offsetZ, extra);
    }

    public static void igniteBlocks(AbilityUser user, Location center, double radius) {
        igniteBlocks(user, center.getWorld(), new ImmutableVector(center), radius);
    }

    public static void igniteBlocks(AbilityUser user, World world, ImmutableVector center, double radius) {
        new SphereCollider(world, center, radius).handleBlockCollisions(block -> {
            igniteBlock(user, block.getRelative(BlockFace.UP));
            return CollisionCallbackResult.CONTINUE;
        });
    }

    public static boolean igniteBlock(AbilityUser user, Block block) {
        return igniteBlock(user, block, ThreadLocalRandom.current().nextLong(minFireDuration, maxFireDuration));
    }

    public static boolean igniteBlock(AbilityUser user, Block block, long duration) {
        BlockState state = block.getState(false);
        boolean light = false;
        if (state instanceof Furnace furnace) {
            if (furnace.getBurnTime() < 800) {
                furnace.setBurnTime((short) 800);
                light = true;
            }
        }
        if (light || MaterialUtils.isCampfire(block)) {
            Lightable data = (Lightable) block.getBlockData();
            if (!data.isLit()) {
                data.setLit(true);
                block.setBlockData(data);
            }
        }
        if (block.isPassable() && !block.isLiquid()) {
            AxisAlignedBoundingBoxCollider alignedBoundingBoxCollider = new AxisAlignedBoundingBoxCollider(block.getRelative(BlockFace.DOWN));
            if (alignedBoundingBoxCollider.getHalfExtents().getY() == 0.5) {
                Material fire = isBlueFireBender(user) ? Material.SOUL_FIRE : Material.FIRE;
                new TemporaryBlock(block.getLocation(), fire.createBlockData(), duration);
                return true;
            }
        }
        return false;
    }

    public static boolean isBlueFireBender(AbilityUser user) {
        return !user.getActiveAbilities(BlueFire.class).isEmpty();
    }
}
