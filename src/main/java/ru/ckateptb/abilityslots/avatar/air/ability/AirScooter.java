package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.Arrays;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirScooter",
        displayName = "AirScooter",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Creates a balloon under your feet for movement",
        instruction = "Jump and Left Click at sprinting time",
        cooldown = 3500,
        cost = 10
)
public class AirScooter extends Ability {
    @ConfigField
    private static double speed = 0.7;
    @ConfigField
    private static long duration = 0;
    @ConfigField
    private static boolean withoutSprinting = false;
    @ConfigField
    private static long energyCostInterval = 1000;

    private RemovalConditional removal;
    private HeightSmoother heightSmoother;
    public boolean canRender = true;
    private double verticalPosition = 0;
    private int stuckCount = 0;
    private Listener damageHandler;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        if (user.destroyInstances(AirScooter.class)) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.heightSmoother = new HeightSmoother();
        Location location = livingEntity.getLocation();
        double dist = user.getDistanceAboveGround();
        if (dist < 0.5 || dist > 5) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .sneaking(false)
                .canUse(() -> livingEntity.getLocation())
                .duration(duration)
                .costInterval(energyCostInterval)
                .build();
        if (!location.getBlock().isLiquid() && (withoutSprinting || user.isSprinting())) {
            this.damageHandler = new DamageHandler();
            Bukkit.getPluginManager().registerEvents(damageHandler, AbilitySlots.getInstance());
            return ActivateResult.ACTIVATE;
        }
        return ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        this.stuckCount = new ImmutableVector(livingEntity.getVelocity()).lengthSquared() < 0.1 ? stuckCount + 1 : 0;
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
        EntityDamageEvent.getHandlerList().unregister(damageHandler);
        user.setCooldown(this);
    }

    public void render() {
        double rotationFrequency = 3;

        verticalPosition += (2 * Math.PI) / (20 / rotationFrequency);

        int horizontalParticles = 10;
        double radius = 0.6;
        ImmutableVector location = user.getLocation();
        for (int i = 0; i < horizontalParticles; ++i) {
            double angle = ((Math.PI * 2) / horizontalParticles) * i;
            double x = radius * Math.cos(angle) * Math.sin(verticalPosition);
            double y = radius * Math.cos(verticalPosition);
            double z = radius * Math.sin(angle) * Math.sin(verticalPosition);
            AirElement.display(location.add(x, y, z).toLocation(world), 2, 0.0f, 0.0f, 0.0f, false);
        }
        AirElement.sound(location.toLocation(world));
    }

    private boolean move() {
        if (isColliding()) {
            return false;
        }
        double height = user.getDistanceAboveGround();
        double smoothedHeight = heightSmoother.add(height);
        if (livingEntity.getLocation().getBlock().isLiquid()) {
            height = 0.5;
        } else if (smoothedHeight > 3.25) {
            return false;
        }
        double delta = getPrediction() - height;
        double force = Math.max(-0.5, Math.min(0.5, 0.3 * delta));
        ImmutableVector velocity = user.getDirection().setY(0).normalize().multiply(speed).setY(force);
        AbilityTarget.of(livingEntity).setVelocity(velocity, this);
        livingEntity.setFallDistance(0);
        return true;
    }

    private boolean isColliding() {
        double speed = livingEntity.getVelocity().setY(0).length();
        ImmutableVector direction = user.getDirection().setY(0).normalize(ImmutableVector.ZERO);
        ImmutableVector front = user.getEyeLocation().subtract(0, 0.5, 0)
                .add(direction.multiply(Math.max(speed, AirScooter.speed)));
        Block block = front.toBlock(world);
        return !block.isLiquid() && !block.isPassable();
    }

    private double getPrediction() {
        double playerSpeed = livingEntity.getVelocity().setY(0).length();
        double speed = Math.max(AirScooter.speed, playerSpeed) * 3;
        ImmutableVector offset = user.getDirection().setY(0).normalize().multiply(speed);
        ImmutableVector location = user.getLocation().add(offset);
        AxisAlignedBoundingBoxCollider alignedBoundingBoxCollider = new AxisAlignedBoundingBoxCollider(livingEntity).at(location);
        if (alignedBoundingBoxCollider.handleBlockCollisions(false)) {
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


    public class DamageHandler implements Listener {
        @EventHandler
        public void on(EntityDamageEvent event) {
            if (event.getEntity() instanceof LivingEntity entity && entity == livingEntity) {
                user.destroyInstances(AirScooter.class);
            }
        }
    }
}
