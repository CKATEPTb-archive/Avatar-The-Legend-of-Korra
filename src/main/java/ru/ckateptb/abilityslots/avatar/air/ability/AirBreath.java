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
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.particlestream.ParticleStream;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBreath",
        displayName = "AirBreath",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.DAMAGE},
        category = "air",
        description = "Releases air from your lungs with such force that you can lift yourself up or push your enemies away",
        instruction = "Hold Sneak",
        cooldown = 3500
)
@CollisionParticipant
public class AirBreath implements Ability {
    @ConfigField
    private static long duration = 7000;
    @ConfigField
    private static double damage = 0;
    @ConfigField
    private static int particles = 1;
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
    private final List<Collider> colliders = new ArrayList<>();
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        if (getAbilityInstanceService().destroyInstanceType(user, AirScooter.class) || method == ActivationMethod.DAMAGE) {
            return ActivateResult.NOT_ACTIVATE;
        }
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
        colliders.clear();
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
            Sphere collider = new Sphere(new Vector3d(eyeLocation.toVector()), 2);
            colliders.add(collider);
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
    public Collection<Collider> getColliders() {
        return colliders;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
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
