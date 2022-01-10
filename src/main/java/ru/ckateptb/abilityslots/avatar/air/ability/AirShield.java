package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.sequence.AirSlam;
import ru.ckateptb.abilityslots.avatar.air.ability.sequence.AirStream;
import ru.ckateptb.abilityslots.avatar.air.ability.sequence.AirSweep;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

import java.util.Collection;
import java.util.Collections;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirShield",
        displayName = "AirShield",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Creates an air barrier around you that prevents anyone from approaching and destroys many abilities",
        instruction = "Hold Sneak",
        cooldown = 6000
)
@CollisionParticipant(destroyAbilities = {
        AirBlast.class,
        AirSwipe.class,
        AirSweep.class,
        AirStream.class,
        AirBreath.class,
        Tornado.class,
        SonicBlast.class,
        AirSuction.class,
        AirPunch.class,
        AirSlam.class
})
public class AirShield implements Ability {
    @ConfigField
    private static long duration = 7000;
    @ConfigField
    private static double radius = 5;
    @ConfigField
    private static double maxPush = 3;
    @ConfigField
    private static double renderParticleSpeed = 2;
    @ConfigField
    private static int renderParticleStreams = 5;
    @ConfigField
    private static int renderParticleCount = 5;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        if (!user.canUse(livingEntity.getLocation())) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new SwappedSlotsRemovalPolicy<>(user, AirShield.class),
                new OutOfWorldRemovalPolicy(user),
                new SneakingRemovalPolicy(user, true)
        );
        if (duration > 0) {
            this.removalPolicy.addPolicy(new DurationRemovalPolicy(duration));
        }
        return ActivateResult.ACTIVATE;
    }

    private Vector3d getCenter() {
        return new Vector3d(livingEntity.getLocation().add(0, 1.8 / 2, 0));
    }

    @Override
    public Collection<Collider> getColliders() {
        return Collections.singletonList(new Sphere(getCenter(), radius));
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();

        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }

        if (!user.canUse(livingEntity.getLocation())) {
            return UpdateResult.REMOVE;
        }

        Vector3d center = getCenter();

        double height = radius * 2;
        double spacing = height / (renderParticleStreams + 1);

        for (int i = 1; i <= renderParticleStreams; ++i) {
            double y = (i * spacing) - radius;
            double factor = 1.0 - (Math.abs(y) * Math.abs(y)) / (radius * radius);

            // Don't render the end points that are tightly coupled.
            if (factor <= 0.2) continue;

            double theta = time * renderParticleSpeed;

            // Offset each stream so they aren't all lined up.
            double x = radius * factor * Math.cos(theta + i * (Math.PI * 2.0 / renderParticleStreams));
            double z = radius * factor * Math.sin(theta + i * (Math.PI * 2.0 / renderParticleStreams));

            Vector3d offset = new Vector3d(x, y, z);

            Vector3d location = center.add(offset);

            AirElement.display(location.toLocation(livingEntity.getWorld()), renderParticleCount, 0, 0.2f, 0.2f, 0.2f);
        }

        CollisionUtils.handleEntityCollisions(livingEntity, new Sphere(center, radius), (entity) -> {
            if (!user.canUse(entity.getLocation())) {
                return false;
            }
            Vector3d toEntity = new Vector3d(entity.getLocation()).subtract(center);
            Vector3d normal = toEntity.setY(0).normalize();
            double strength = ((radius - toEntity.length()) / radius) * maxPush;
            strength = Math.max(0, Math.min(1, strength));
            entity.setVelocity(new Vector3d(entity.getVelocity()).add(normal.multiply(strength)).toBukkitVector());
            return false;
        }, false);

        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        this.user.setCooldown(this);
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
