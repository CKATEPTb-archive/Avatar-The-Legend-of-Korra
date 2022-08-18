package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.AbilityCollisionResult;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.util.VectorUtils;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.RayCollider;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBurst",
        displayName = "AirBurst",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.FALL, ActivationMethod.SNEAK},
        category = "air",
        description = "Harness all the chaos in the air around you, allowing you to channel its energy into a multitude of AirBlast. You and only you decide how to use it, around you, along a cone or when falling",
        instruction = """
                Several ways to use:
                1. Select the ability when falling from a height
                2. Hold Sneak to accumulate air
                2.1. Left Click to create a cone
                2.2. Release Sneak to blast the accumulated air""",
        cooldown = 3000,
        cost = 30
)
@CollisionParticipant
public class AirBurst extends Ability {
    private enum Mode {CONE, SPHERE, FALL}

    @ConfigField
    private static double speed = 1.2;
    @ConfigField
    private static double power = 1.2;
    @ConfigField
    private static double pushRadius = 1.5;
    @ConfigField(name = "sphere.chargeTime")
    private static int sphereChargeTime = 1750;
    @ConfigField(name = "sphere.cooldownMultiplier")
    private static double sphereCooldownMultiplier = 1;
    @ConfigField(name = "sphere.range")
    private static double sphereRange = 12;
    @ConfigField(name = "cone.chargeTime")
    private static int coneChargeTime = 875;
    @ConfigField(name = "cone.cooldownMultiplier")
    private static double coneCooldownMultiplier = 0.5;
    @ConfigField(name = "cone.range")
    private static double coneRange = 16;
    @ConfigField(name = "fall.cooldownMultiplier")
    private static double fallCooldownMultiplier = 2;
    @ConfigField(name = "fall.range")
    private static double fallRange = 8;
    @ConfigField(name = "fall.threshold")
    private static int fallThreshold = 10;

    private final Collection<BurstStream> streams = new ArrayList<>();
    private final Map<Entity, BurstStream> affected = new HashMap<>();
    private boolean released;
    private long startTime;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        if (method == ActivationMethod.LEFT_CLICK) {
            user.getAbilityInstances(AirBurst.class).stream()
                    .findFirst()
                    .ifPresent(airBurst -> airBurst.release(Mode.CONE));
            return ActivateResult.NOT_ACTIVATE;
        }
        this.removal = new RemovalConditional.Builder().offline().dead().world().slot().build();
        this.released = false;
        if (method == ActivationMethod.FALL) {
            if (livingEntity.getFallDistance() < fallThreshold || user.isSneaking()) {
                return ActivateResult.NOT_ACTIVATE;
            }
            release(Mode.FALL);
        }
        this.startTime = System.currentTimeMillis();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        if (!released) {
            boolean coneCharged = isConeCharged();
            boolean sphereCharged = isSphereCharged();
            if (coneCharged) {
                ImmutableVector direction = user.getDirection();
                ImmutableVector location = user.getEyeLocation().add(direction);
                ImmutableVector side = direction.crossProduct(ImmutableVector.PLUS_J).normalize(ImmutableVector.PLUS_I);
                ImmutableVector left = location.subtract(side.multiply(0.5));
                AirElement.display(left.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
                if (sphereCharged) {
                    ImmutableVector right = location.add(side.multiply(0.5));
                    AirElement.display(right.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
                }
                if (!user.isSneaking()) {
                    release(sphereCharged ? Mode.SPHERE : Mode.CONE);
                }
            } else {
                if (!user.isSneaking()) {
                    return UpdateResult.REMOVE;
                }
            }
            return UpdateResult.CONTINUE;
        }
        streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
        return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    private void release(Mode mode) {
        if (released || !user.removeEnergy(this)) return;
        this.released = true;
        Collection<RayCollider> rays = new ArrayList<>();
        AbilityInformation information = getInformation();
        long cooldown = information.getCooldown();
        if (mode == Mode.CONE) {
            if (!isConeCharged()) return;
            rays.addAll(VectorUtils.cone(user, coneRange));
            user.setCooldown(information, (long) (cooldown * coneCooldownMultiplier));
        } else if (mode == Mode.SPHERE) {
            if (!isSphereCharged()) return;
            rays.addAll(VectorUtils.sphere(user, sphereRange));
            user.setCooldown(information, (long) (cooldown * sphereCooldownMultiplier));
        } else {
            rays.addAll(VectorUtils.fall(user, fallRange));
            user.setCooldown(information, (long) (cooldown * fallCooldownMultiplier));
        }
        rays.forEach(rayCollider -> streams.add(new BurstStream(this, rayCollider)));
        removal = new RemovalConditional.Builder().offline().dead().world().build();
    }

    public boolean isSphereCharged() {
        return System.currentTimeMillis() >= startTime + sphereChargeTime;
    }

    public boolean isConeCharged() {
        return System.currentTimeMillis() >= startTime + coneChargeTime;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream().map(BurstStream::getCollider).toList();
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(stream -> destroyedCollider == stream.getCollider());
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class BurstStream {
        private final AirBurst ability;
        private final ImmutableVector original;
        private final ImmutableVector direction;
        private final RemovalConditional removal;
        private final double maxRange;
        @Getter
        private Collider collider;
        private ImmutableVector location;
        private long nextRenderTime;

        BurstStream(AirBurst ability, RayCollider rayCollider) {
            this.ability = ability;
            this.location = rayCollider.getPosition();
            this.original = location;
            this.direction = rayCollider.getDirection().normalize();
            this.maxRange = rayCollider.getMaxDistance();
            this.removal = new RemovalConditional.Builder()
                    .canUse(() -> location.toLocation(world))
                    .range(() -> original.toLocation(world), () -> location.toLocation(world), maxRange)
                    .build();
        }

        UpdateResult update() {
            if (removal.shouldRemove(user, ability)) return UpdateResult.REMOVE;
            this.collider = new SphereCollider(world, this.location, pushRadius);
            this.collider.handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            this.collider.handleEntityCollision(livingEntity, false, entity -> {
                if(affected.putIfAbsent(entity, this) == this) {
                    AbilityTarget target = AbilityTarget.of(entity);
                    ImmutableVector yMultiplier = new ImmutableVector(0, 0.1 * (maxRange - entity.getLocation().distance(original.toLocation(world))), 0);
                    target.setVelocity(direction.add(yMultiplier).multiply(power), ability);
                    entity.setFireTicks(0);
                }
                return CollisionCallbackResult.CONTINUE;
            });

            long time = System.currentTimeMillis();
            ThreadLocalRandom current = ThreadLocalRandom.current();
            if (current.nextBoolean()) {
                Location location = this.location.toLocation(world);
                if (time >= nextRenderTime) {
                    AirElement.display(location, 1, 0.2f, 0.2f, 0.2f, false);
                    nextRenderTime = time + 75;
                }
                if (current.nextInt(12) == 0) {
                    AirElement.sound(location);
                }
            }
            this.location = this.location.add(this.direction.multiply(speed));
            return new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        }
    }
}
