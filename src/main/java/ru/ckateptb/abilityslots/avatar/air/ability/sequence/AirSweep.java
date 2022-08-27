package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.AbilityCollisionResult;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBurst;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSuction;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSwipe;
import ru.ckateptb.abilityslots.common.math.CubicHermiteSpline;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSweep",
        displayName = "AirSweep",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Sweep the air in front of you hitting multiple enemies, causing moderate damage and a large knockback. The radius and direction of AirSweep is controlled by moving your mouse in a sweeping motion. For example, if you want to AirSweep upward, then move your mouse upward right after you left click AirBurst",
        instruction = "AirSwipe \\(Left Click\\) > AirBurst \\(Hold Shift\\) > AirBurst \\(Left Click\\) > Move the mouse \\(optional\\)",
        cooldown = 6000,
        cost = 15,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirSwipe.class, action = SequenceAction.LEFT_CLICK),
        @AbilityAction(ability = AirBurst.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirBurst.class, action = SequenceAction.LEFT_CLICK)
})
@CollisionParticipant(destroyAbilities = {
        AirBlast.class,
        AirSweep.class,
        AirSuction.class,
        AirSwipe.class,
        Twister.class
})
public class AirSweep extends Ability {
    @ConfigField
    private static double range = 14;
    @ConfigField
    private static double speed = 1.4;
    @ConfigField
    private static double damage = 3;
    @ConfigField
    private static double knockback = 3.5;
    @ConfigField
    private static int sampleTime = 400;
    @ConfigField
    private static int streamCount = 30;
    @ConfigField(comment = "This sets the method in which the sweep interpolates the samples.\nThis can be set to either 'linear' or 'spline'.")
    private static String interpolationMethod = "spline";
    @ConfigField
    private static int launchDuration = 500;
    @ConfigField
    private static boolean lockedLaunchOrigin = true;
    @ConfigField
    private static boolean startFromLegs = true;

    private long startTime;
    private CubicHermiteSpline spline;
    private final List<SweepStream> streams = new ArrayList<>();
    private final List<Entity> affected = new ArrayList<>();
    private int launchCount;
    private boolean linear;
    private ImmutableVector origin;
    private double potential;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        user.destroyInstances(AirBurst.class);
        this.startTime = System.currentTimeMillis();
        this.spline = new CubicHermiteSpline(0.1);
        this.linear = "linear".equalsIgnoreCase(interpolationMethod);
        this.origin = null;
        this.world = livingEntity.getWorld();
        if (!user.removeEnergy(this)) return ActivateResult.NOT_ACTIVATE;
        user.setCooldown(this);
        return getOriginLocation().toBlock(world).isLiquid() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();
        if (time < startTime + sampleTime) {
            // Sample target positions and add them to the spline.
            // These positions get interpolated later.
            ImmutableVector position = getOriginLocation().add(user.getDirection().multiply(range));
            spline.addKnot(position);

            return UpdateResult.CONTINUE;
        }

        if (this.origin == null || !lockedLaunchOrigin) {
            this.origin = getOriginLocation();
        }

        // Clear out any intermediate knots so it becomes a line.
        if (linear && spline.getKnots().size() > 2) {
            List<ImmutableVector> knots = spline.getKnots();
            ImmutableVector begin = knots.get(0);
            ImmutableVector end = knots.get(knots.size() - 1);

            knots.clear();
            spline.addKnot(begin);
            spline.addKnot(end);
        }

        // Launch a few streams per tick to give it a delay.
        if (launchCount < streamCount) {
            potential += streamCount / (launchDuration / 50.0);

            int count = Math.min(streamCount - launchCount, (int) potential - launchCount);

            for (int i = 0; i < count; ++i) {
                // Interpolate based on the initial samples gathered.
                ImmutableVector target = spline.interpolate(launchCount / (double) streamCount);
                ImmutableVector direction = target.subtract(this.origin).normalize();

                streams.add(new SweepStream(this, this.origin, direction));
                ++launchCount;
            }
        }

        streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);

        return (streams.isEmpty() && launchCount >= streamCount) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    private ImmutableVector getOriginLocation() {
        return new ImmutableVector(startFromLegs ? livingEntity.getLocation() : livingEntity.getEyeLocation());
    }

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream()
                .map(SweepStream::getCollider)
                .collect(Collectors.toList());
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(stream -> destroyedCollider == stream.getCollider());
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class SweepStream {
        private final AirSweep ability;
        private ImmutableVector location;
        private final ImmutableVector direction;
        private final RemovalConditional removal;
        @Getter
        private Collider collider;

        SweepStream(AirSweep ability, ImmutableVector original, ImmutableVector direction) {
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
            this.collider = new SphereCollider(world, location, 1);
            if (this.collider.handleEntityCollision(livingEntity, true, entity -> {
                if (user.canUse(entity.getLocation()) && !affected.contains(entity)) {
                    AbilityTargetLiving target = AbilityTarget.of((LivingEntity) entity);
                    target.damage(damage, ability);
                    target.setVelocity(direction.multiply(knockback), ability);

                    affected.add(entity);
                }
                return CollisionCallbackResult.CONTINUE;
            })) return UpdateResult.REMOVE;
            return new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        }
    }
}
