package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.sequence.AirStream;
import ru.ckateptb.abilityslots.avatar.air.ability.sequence.AirSweep;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirShield",
        displayName = "AirShield",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Creates an air barrier around you that prevents anyone from approaching and destroys many abilities",
        instruction = "Hold Sneak",
        cooldown = 6000,
        cost = 10
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
        AirPunch.class
})
public class AirShield extends Ability {
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
    @ConfigField
    private static long energyCostInterval = 1000;

    private RemovalConditional removal;
    private SphereCollider collider;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> user.getCenterLocation().toLocation(world))
                .sneaking(true)
                .duration(duration)
                .costInterval(energyCostInterval)
                .slot()
                .liquid(false)
                .build();
        Location center = user.getCenterLocation().toLocation(world);
        return !center.getBlock().isLiquid() && user.canUse(center) && user.removeEnergy(this) ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        livingEntity.setFireTicks(0);
        ImmutableVector center = user.getCenterLocation();
        double height = radius * 2;
        double spacing = height / (renderParticleStreams + 1);
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= renderParticleStreams; ++i) {
                double y = (i * spacing) - radius;
                double factor = 1.0 - (Math.abs(y) * Math.abs(y)) / (radius * radius);
                // Don't render the end points that are tightly coupled.
                if (factor <= 0.2) continue;
                double theta = time * renderParticleSpeed;
                // Offset each stream so they aren't all lined up.
                double x = radius * factor * Math.cos(theta + i * (Math.PI * 2.0 / renderParticleStreams));
                double z = radius * factor * Math.sin(theta + i * (Math.PI * 2.0 / renderParticleStreams));
                ImmutableVector offset = new ImmutableVector(x, y, z);
                ImmutableVector location = center.add(offset);
                AirElement.display(location.toLocation(world), renderParticleCount, 0.2f, 0.2f, 0.2f, false);
            }
            AirElement.sound(center.toLocation(world));
        });
        this.collider = new SphereCollider(world, center, radius);
        this.collider.handleEntityCollision(livingEntity, false, false, false, entity -> {
            AbilityTarget target = AbilityTarget.of(entity);
            ImmutableVector toEntity = target.getLocation().subtract(center);
            ImmutableVector normal = toEntity.setY(0).normalize();
            double strength = ((radius - toEntity.length()) / radius) * maxPush;
            strength = Math.max(0, Math.min(1, strength));
            target.setVelocity(new ImmutableVector(entity.getVelocity()).add(normal.multiply(strength)), this);
            return CollisionCallbackResult.CONTINUE;
        }, entity -> user.canUse(entity.getLocation()));
        this.collider.handleBlockCollisions(false, false, block -> {
            AirElement.handleBlockInteractions(user, block);
            return CollisionCallbackResult.CONTINUE;
        }, block -> user.canUse(block.getLocation()));
        return UpdateResult.CONTINUE;
    }

    @Override
    public Collection<Collider> getColliders() {
        return collider == null ? Collections.emptyList() : Collections.singletonList(collider);
    }

    @Override
    public void destroy() {
        this.user.setCooldown(this);
    }
}
