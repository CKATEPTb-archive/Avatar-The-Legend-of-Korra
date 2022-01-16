package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.AbilityCollisionResult;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSwipe",
        displayName = "AirSwipe",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.SNEAK},
        category = "air",
        description = "Creates an arc of air that damages your targets and knocks them back a decent distance",
        instruction = "Left Click \\(or Hold Sneak to reinforce and then Release Sneak\\)",
        cooldown = 3500,
        cost = 5
)
@CollisionParticipant(destroyAbilities = {
        AirBlast.class,
        AirSwipe.class,
        AirSuction.class,
        AirPunch.class,
        AirSpout.class
})
public class AirSwipe extends Ability {
    @ConfigField
    private static double damage = 2;
    @ConfigField
    private static double range = 14;
    @ConfigField
    private static double speed = 1.25;
    @ConfigField
    private static int arc = 32;
    @ConfigField
    private static int arcStep = 4;
    @ConfigField
    private static long maxChargeTime = 2500;
    @ConfigField
    private static double chargeFactor = 3.0;
    @ConfigField
    private static double entityCollisionRadius = 1;
    @ConfigField
    private static double abilityCollisionRadius = 1;

    private final List<SwipeStream> streams = new ArrayList<>();
    private final List<Entity> affectedEntities = new ArrayList<>();

    private ImmutableVector original;
    private RemovalConditional removal;
    private boolean charging;
    private long startTime;
    private double factor = 1.0;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        Location eyeLocation = livingEntity.getEyeLocation();
        this.original = user.getEyeLocation();
        this.startTime = System.currentTimeMillis();
        this.charging = true;
        if (eyeLocation.getBlock().isLiquid() || !user.canUse(eyeLocation)) {
            return ActivateResult.NOT_ACTIVATE;
        }

        removal = new RemovalConditional.Builder()
                .dead()
                .offline()
                .world()
                .build();

        for (AirSwipe swipe : getAbilityInstanceService().getAbilityUserInstances(user, AirSwipe.class)) {
            if (swipe.charging) {
                swipe.launch();
                return ActivateResult.NOT_ACTIVATE;
            }
        }

        if (method == ActivationMethod.LEFT_CLICK) {
            launch();
        }

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        long time = System.currentTimeMillis();
        affectedEntities.clear();
        if (charging) {
            if (new RemovalConditional.Builder().slot().build().shouldRemove(user, this)) {
                return UpdateResult.REMOVE;
            }
            if (user.isSneaking() && time >= startTime + maxChargeTime) {
                ImmutableVector direction = user.getDirection();
                ImmutableVector location = user.getEyeLocation().add(direction);
                ImmutableVector side = direction.crossProduct(ImmutableVector.PLUS_J).normalize(ImmutableVector.PLUS_I);
                location = location.add(side.multiply(0.5));
                AirElement.display(location.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
            } else if (!user.isSneaking()) {
                factor = Math.max(1.0, Math.min(1.0, (time - startTime) / (double) maxChargeTime) * chargeFactor);
                launch();
            }
        } else {
            streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
        }
        return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
    }

    private void launch() {
        if (!user.removeEnergy(this)) return;
        charging = false;
        user.setCooldown(this);

        original = user.getEyeLocation();

        ImmutableVector up = ImmutableVector.PLUS_J;
        ImmutableVector lookingDir = user.getDirection();
        ImmutableVector right = lookingDir.crossProduct(up).normalize(ImmutableVector.PLUS_I);
        ImmutableVector rotateAxis = right.crossProduct(lookingDir);

        double halfArc = arc / 2.0;

        for (double deg = -halfArc; deg <= halfArc; deg += arcStep) {
            double rads = Math.toRadians(deg);

            ImmutableVector direction = lookingDir;
            direction = direction.rotate(rotateAxis, rads);

            streams.add(new SwipeStream(this, original, direction));
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream()
                .map(SwipeStream::getCollider)
                .collect(Collectors.toList());
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(stream -> destroyedCollider == stream.getCollider());
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class SwipeStream {
        private final AirSwipe ability;
        private ImmutableVector location;
        private final ImmutableVector direction;
        private final RemovalConditional removal;
        @Getter
        private Collider collider;

        SwipeStream(AirSwipe ability, ImmutableVector original, ImmutableVector direction) {
            this.ability = ability;
            this.location = original;
            this.direction = direction;
            this.removal = new RemovalConditional.Builder()
                    .canUse(() -> location.toLocation(world))
                    .range(() -> original.toLocation(world), () -> location.toLocation(world), range)
                    .build();
        }

        UpdateResult update() {
            location = location.add(direction.multiply(speed));
            if (removal.shouldRemove(user, ability)) return UpdateResult.REMOVE;
            AirElement.display(location.toLocation(world), 1, 0.0f, 0.0f, 0.0f, 0.0f);
            this.collider = new SphereCollider(world, location, abilityCollisionRadius);
            if (new SphereCollider(world, location, entityCollisionRadius).handleEntityCollision(livingEntity, false, entity -> {
                if (user.canUse(entity.getLocation())) {
                    AbilityTarget.of(entity).setVelocity(direction.multiply(factor), ability);
                    if (entity instanceof LivingEntity livingEntity && !affectedEntities.contains(livingEntity)) {
                        AbilityTarget.of((LivingEntity) entity).damage(damage * factor, ability);
                        affectedEntities.add(entity);
                    }
                }
                return CollisionCallbackResult.CONTINUE;
            })) return UpdateResult.REMOVE;
            new SphereCollider(world, location, 0.5).handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            return new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        }
    }
}
