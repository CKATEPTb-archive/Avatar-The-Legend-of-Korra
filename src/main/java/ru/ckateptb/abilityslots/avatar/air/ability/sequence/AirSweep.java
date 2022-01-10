package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.*;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.*;
import ru.ckateptb.abilityslots.common.math.CubicHermiteSpline;
import ru.ckateptb.abilityslots.common.particlestream.ParticleStream;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSweep",
        displayName = "AirSweep",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Sweep the air in front of you hitting multiple enemies, causing moderate damage and a large knockback. The radius and direction of AirSweep is controlled by moving your mouse in a sweeping motion. For example, if you want to AirSweep upward, then move your mouse upward right after you left click AirBurst",
        instruction = "AirSwipe (Left Click) > AirBurst (Hold Shift) > AirBurst (Left Click) > Move the mouse (optional)",
        cooldown = 6000,
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
        AirSweep.class,
        Twister.class
})
public class AirSweep implements Ability {
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

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private CubicHermiteSpline spline;
    private List<ParticleStream> streams;
    private final List<Entity> affected = new ArrayList<>();
    private int launchCount;
    private boolean linear;
    private Vector3d origin;
    private double potential;
    private World world;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        getAbilityInstanceService().destroyInstanceType(user, AirBurst.class);
        this.startTime = System.currentTimeMillis();
        this.spline = new CubicHermiteSpline(0.1);
        this.streams = new ArrayList<>();
        this.linear = "linear".equalsIgnoreCase(interpolationMethod);
        this.origin = null;
        this.world = livingEntity.getWorld();

        user.setCooldown(this);

        return getOriginLocation().toBlock(world).isLiquid() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();

        if (time < startTime + sampleTime) {
            // Sample target positions and add them to the spline.
            // These positions get interpolated later.
            Vector3d position = getOriginLocation().add(new Vector3d(livingEntity.getEyeLocation().getDirection().multiply(range)));
            spline.addKnot(position);

            return UpdateResult.CONTINUE;
        }

        if (this.origin == null || !lockedLaunchOrigin) {
            this.origin = getOriginLocation();
        }

        // Clear out any intermediate knots so it becomes a line.
        if (linear && spline.getKnots().size() > 2) {
            List<Vector3d> knots = spline.getKnots();
            Vector3d begin = knots.get(0);
            Vector3d end = knots.get(knots.size() - 1);

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
                Vector3d target = spline.interpolate(launchCount / (double) streamCount);
                Vector3d direction = target.subtract(this.origin).normalize();

                streams.add(new SweepStream(user, this.origin, direction, range, speed, 1, 1, damage));
                ++launchCount;
            }
        }

        streams.removeIf(stream -> !stream.update());

        return (streams.isEmpty() && launchCount >= streamCount) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    private Vector3d getOriginLocation() {
        return new Vector3d(startFromLegs ? livingEntity.getLocation() : livingEntity.getEyeLocation());
    }

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream()
                .map(ParticleStream::getCollider)
                .collect(Collectors.toList());
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(stream -> destroyedCollider == stream.getCollider());
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class SweepStream extends ParticleStream {
        public SweepStream(AbilityUser user, Vector3d origin, Vector3d direction, double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double damage) {
            super(user, origin.toLocation(world), direction, range, speed, entityCollisionRadius, abilityCollisionRadius, 0.1, damage);
        }

        @Override
        public void render() {
            AirElement.display(location, 1, 0, 0, 0);
        }

        @Override
        public boolean onEntityHit(Entity entity) {
            if (!user.canUse(entity.getLocation())) {
                return false;
            }

            if (affected.contains(entity)) {
                return true;
            }

            ((LivingEntity) entity).damage(damage, livingEntity);
            entity.setVelocity(direction.multiply(knockback).toBukkitVector());

            affected.add(entity);

            return true;
        }
    }
}
