package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;
import ru.ckateptb.tablecloth.util.WorldUtils;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBreath",
        displayName = "AirBreath",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class AirBreath implements Ability {
    @ConfigField
    private static long duration = 7000;
    @ConfigField
    private static double damage = 1;
    @ConfigField
    private static int particles = 4;
    @ConfigField
    private static double range = 10;
    @ConfigField
    private static double power = 0.5;
    @ConfigField
    private static double knockback = 0.8;
    @ConfigField
    private static int angle = 30;

    private AbilityUser user;
    private LivingEntity livingEntity;
    private Vector3d direction;
    private Sphere collider;
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.removalPolicy = new CompositeRemovalPolicy(
                new SneakingRemovalPolicy(user, true),
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user),
                new DurationRemovalPolicy(duration),
                new SwappedSlotsRemovalPolicy<>(user, AirBreath.class)
        );
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        Location eyeLocation = livingEntity.getEyeLocation();
        this.direction = new Vector3d(eyeLocation.getDirection().normalize());
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        double step = 1;
        double size = 0;

        for (double i = 0; i < range; i += step) {
            eyeLocation = eyeLocation.add(direction.multiply(step).toBukkitVector());
            size += 0.005;

            if (!isLocationSafe(eyeLocation)) {
                if (livingEntity.getLocation().getPitch() > angle) {
                    livingEntity.setVelocity(direction.multiply(-power).toBukkitVector());
                }
                return UpdateResult.CONTINUE;
            }
            Location finalLoc = eyeLocation.clone();
            collider = new Sphere(new Vector3d(eyeLocation.toVector()), 2);
            for (Block handleBlock : WorldUtils.getNearbyBlocks(finalLoc, 2)) {
                AirElement.handleBlockInteractions(user, handleBlock);
            }
            CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                if (!user.canUse(finalLoc)) return false;
                if (damage > 0 && entity instanceof LivingEntity)
                    ((LivingEntity) entity).damage(damage, livingEntity);
                entity.setVelocity(direction.multiply(knockback).toBukkitVector());
                return true;
            }, false);
            AirElement.display(eyeLocation, particles, (float) size, (float) Math.random(), (float) Math.random(), (float) Math.random());
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());
    }

    private boolean isLocationSafe(Location loc) {
        Block block = loc.getBlock();
        return !user.canUse(loc) || block.isPassable();
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
