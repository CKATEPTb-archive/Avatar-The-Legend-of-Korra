package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.RandomUtils;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.concurrent.ThreadLocalRandom;


@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Tornado",
        displayName = "Tornado",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class Tornado implements Ability {
    @ConfigField
    private static long duration = 4000;
    @ConfigField
    private static double radius = 10;
    @ConfigField
    private static double height = 15;
    @ConfigField
    private static double range = 25;
    @ConfigField
    private static long growthTime = 2500;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private double yOffset = 0;
    private double currentAngle = 0;
    private RemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        removalPolicy = new CompositeRemovalPolicy(
                new SwappedSlotsRemovalPolicy<>(user, Tornado.class),
                new DurationRemovalPolicy(duration),
                new SneakingRemovalPolicy(user, true)
        );
        this.startTime = System.currentTimeMillis();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        World world = livingEntity.getWorld();
        Vector3d base = RayTrace.of(livingEntity).range(range).ignoreLiquids(false).result(world).position();
        Block baseBlock = base.toBlock(world);
        if (baseBlock.getRelative(BlockFace.DOWN).isPassable()) {
            return UpdateResult.CONTINUE;
        }
        if (!user.canUse(baseBlock.getLocation())) {
            return UpdateResult.REMOVE;
        }

        long time = System.currentTimeMillis();
        double factor = Math.min(1, time - startTime / growthTime);
        double height = 2 + factor * (Tornado.height - 2);
        double radius = 2 + factor * (Tornado.radius - 2);

        AABB box = new AABB(new Vector3d(-radius, 0, -radius), new Vector3d(radius, height, radius)).at(base);
        CollisionUtils.handleEntityCollisions(livingEntity, box, entity -> {
            double dy = entity.getLocation().getY() - base.getY();
            double r = 2 + (radius - 2) * dy;
            Vector3d delta = WorldUtils.getEntityCenter(entity).subtract(base);
            double distSq = delta.getX() * delta.getX() + delta.getZ() * delta.getZ();
            if (distSq > r * r) {
                return false;
            }

            Vector3d velocity;
            if (entity.equals(livingEntity)) {
                double velY;
                if (dy >= height * .95) {
                    velY = 0;
                } else if (dy >= height * .85) {
                    velY = 6.0 * (.95 - dy / height);
                } else {
                    velY = 0.6;
                }
                velocity = new Vector3d(livingEntity.getEyeLocation().getDirection().setY(velY).multiply(factor));
            } else {
                Vector3d normal = delta.setY(0).normalize();
                Vector3d ortho = normal.cross(Vector3d.PLUS_J).normalize();
                velocity = ortho.add(normal).normalize().multiply(factor);
            }
            entity.setVelocity(velocity.toBukkitVector());
            return false;
        }, true, true);

        render(base, factor, height, radius);
        return UpdateResult.CONTINUE;
    }

    private void render(Vector3d base, double factor, double height, double radius) {
        double amount = Math.min(30, Math.max(4, factor * 30));
        yOffset += 0.1;
        if (yOffset >= 1) {
            yOffset = 0;
        }
        currentAngle += 4.5;
        if (currentAngle >= 360) {
            currentAngle = 0;
        }
        for (int i = 0; i < 3; i++) {
            double offset = currentAngle + i * 2 * Math.PI / 3.0;
            for (double y = yOffset; y < height; y += (height / amount)) {
                double r = 2 + (radius - 2) * y / height;
                double x = r * Math.cos(y + offset);
                double z = r * Math.sin(y + offset);
                Location loc = base.add(new Vector3d(x, y, z)).toLocation(livingEntity.getWorld());
                AirElement.display(loc, 1, 0, 0, 0, 0, ThreadLocalRandom.current().nextInt(20) == 0);
            }
        }
    }

    @Override
    public void destroy() {
        this.user.setCooldown(getInformation(), getInformation().getCooldown());
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
