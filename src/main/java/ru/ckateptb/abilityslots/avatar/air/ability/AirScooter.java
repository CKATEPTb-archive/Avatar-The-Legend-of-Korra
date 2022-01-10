package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirScooter",
        displayName = "AirScooter",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.DAMAGE},
        category = "air",
        description = "Creates a balloon under your feet for movement",
        instruction = "Jump and Left Click at sprinting time",
        cooldown = 3500
)
public class AirScooter implements Ability {
    @ConfigField
    private static double speed = 0.7;
    @ConfigField
    private static long duration = 0;
    @ConfigField
    private static boolean withoutSprinting = false;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private CompositeRemovalPolicy removalPolicy;
    private HeightSmoother heightSmoother;
    public boolean canRender = true;
    private double verticalPosition = 0;
    private int stuckCount = 0;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        if (getAbilityInstanceService().destroyInstanceType(user, AirScooter.class) || method == ActivationMethod.DAMAGE) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.heightSmoother = new HeightSmoother();
        Location location = livingEntity.getLocation();
        double dist = WorldUtils.getDistanceAboveGround(livingEntity, false);
        if (dist < 0.5 || dist > 5) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsOfflineRemovalPolicy(user),
                new IsDeadRemovalPolicy(user),
                new SneakingRemovalPolicy(user, false)
        );
        if (duration > 0) {
            this.removalPolicy.addPolicy(new DurationRemovalPolicy(duration));
        }
        return !location.getBlock().isLiquid() && (withoutSprinting || (livingEntity instanceof Player player && player.isSprinting())) ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        if (!user.canUse(livingEntity.getLocation())) {
            return UpdateResult.REMOVE;
        }

        stuckCount = new Vector3d(livingEntity.getVelocity()).lengthSq() < 0.1 ? stuckCount + 1 : 0;
        if (stuckCount > 10 || !move()) {
            return UpdateResult.REMOVE;
        }

        if (canRender) {
            render();
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    public void render() {
        double rotationFrequency = 3;

        verticalPosition += (2 * Math.PI) / (20 / rotationFrequency);

        int horizontalParticles = 10;
        double radius = 0.6;
        for (int i = 0; i < horizontalParticles; ++i) {
            double angle = ((Math.PI * 2) / horizontalParticles) * i;

            double x = radius * Math.cos(angle) * Math.sin(verticalPosition);
            double y = radius * Math.cos(verticalPosition);
            double z = radius * Math.sin(angle) * Math.sin(verticalPosition);
            AirElement.display(livingEntity.getLocation().add(x, y, z), 2, 0.0f, 0.0f, 0.0f, 0.0f, ThreadLocalRandom.current().nextInt(20) == 0);
        }
    }

    private boolean move() {
        if (isColliding()) {
            return false;
        }
        double height = WorldUtils.getDistanceAboveGround(livingEntity, false);
        double smoothedHeight = heightSmoother.add(height);
        if (livingEntity.getLocation().getBlock().isLiquid()) {
            height = 0.5;
        } else if (smoothedHeight > 3.25) {
            return false;
        }
        double delta = getPrediction() - height;
        double force = Math.max(-0.5, Math.min(0.5, 0.3 * delta));
        Vector3d velocity = new Vector3d(livingEntity.getEyeLocation().getDirection()).setY(0).normalize().multiply(AirScooter.speed).setY(force);
        livingEntity.setVelocity(velocity.toBukkitVector());
        livingEntity.setFallDistance(0);
        return true;
    }

    private boolean isColliding() {
        Location eyeLocation = livingEntity.getEyeLocation();
        double speed = livingEntity.getVelocity().setY(0).length();
        Vector3d direction = new Vector3d(eyeLocation.getDirection()).setY(0).normalize(Vector3d.ZERO);
        Vector3d front = new Vector3d(eyeLocation).subtract(new Vector3d(0, 0.5, 0))
                .add(direction.multiply(Math.max(speed, AirScooter.speed)));
        Block block = front.toBlock(livingEntity.getWorld());
        return !block.isLiquid() && !block.isPassable();
    }

    private double getPrediction() {
        double playerSpeed = livingEntity.getVelocity().setY(0).length();
        double speed = Math.max(AirScooter.speed, playerSpeed) * 3;
        Vector3d offset = new Vector3d(livingEntity.getEyeLocation().getDirection()).setY(0).normalize().multiply(speed);
        Vector3d location = new Vector3d(livingEntity.getLocation()).add(offset);
        AABB userBounds = AABB.from(livingEntity).at(location);
        if (!WorldUtils.getNearbyBlocks(livingEntity.getWorld(), userBounds, block -> true, 1).isEmpty()) {
            return 2.25;
        }
        return 1.25;
    }

    private static class HeightSmoother {
        private final double[] values;
        private int index;

        private HeightSmoother() {
            index = 0;
            values = new double[10];
        }

        private double add(double value) {
            values[index] = value;
            index = (index + 1) % values.length;
            return get();
        }

        private double get() {
            return Arrays.stream(values).sum() / values.length;
        }
    }
}
