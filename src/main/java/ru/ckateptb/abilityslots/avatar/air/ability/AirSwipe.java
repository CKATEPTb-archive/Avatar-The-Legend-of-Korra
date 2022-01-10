package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
        name = "AirSwipe",
        displayName = "AirSwipe",
        activationMethods = {ActivationMethod.LEFT_CLICK, ActivationMethod.SNEAK},
        category = "air",
        description = "Creates an arc of air that damages your targets and knocks them back a decent distance",
        instruction = "Left Click (or Hold Sneak to reinforce and then Release Sneak)",
        cooldown = 3500
)
@CollisionParticipant(destroyAbilities = {
        AirBlast.class,
        AirSwipe.class,
        AirSuction.class,
        AirPunch.class,
        AirSpout.class
})
public class AirSwipe implements Ability {
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

    private AbilityUser user;
    private LivingEntity livingEntity;

    private World world;
    private Vector3d origin;
    private final List<SwipeStream> streams = new ArrayList<>();
    private CompositeRemovalPolicy removalPolicy;
    private boolean charging;
    private long startTime;
    private double factor = 1.0;
    private final List<Entity> affectedEntities = new ArrayList<>();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.world = livingEntity.getWorld();
        this.origin = new Vector3d(livingEntity.getEyeLocation());
        this.startTime = System.currentTimeMillis();
        this.charging = true;

        if (livingEntity.getEyeLocation().getBlock().isLiquid()) {
            return ActivateResult.NOT_ACTIVATE;
        }

        removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user),
                new OutOfWorldRemovalPolicy(user)
        );

        for (AirSwipe swipe : getAbilityInstanceService().getAbilityUserInstances(user, AirSwipe.class)) {
            if (swipe.charging) {
                swipe.launch();
                return ActivateResult.NOT_ACTIVATE;
            }
        }

        if (!user.canUse(origin.toLocation(world))) {
            return ActivateResult.NOT_ACTIVATE;
        }

        if (method == ActivationMethod.LEFT_CLICK) {
            launch();
        }

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }

        long time = System.currentTimeMillis();

        affectedEntities.clear();

        if (charging) {
            // Make sure the user keeps this ability selected while charging.
            if(new SwappedSlotsRemovalPolicy<>(user, getClass()).shouldRemove()) {
                return UpdateResult.REMOVE;
            }

            if (livingEntity instanceof Player player && player.isSneaking() && time >= startTime + maxChargeTime) {
                Vector3d direction = new Vector3d(livingEntity.getEyeLocation().getDirection());
                Vector3d location = new Vector3d(livingEntity.getEyeLocation().add(direction.toBukkitVector()));

                Vector3d side = direction.cross(Vector3d.PLUS_J).normalize(Vector3d.PLUS_I);
                location = location.add(side.multiply(0.5));

                // Display air particles to the right of the player.
                AirElement.display(location.toLocation(world), 1, 0.0f, 0.0f, 0.0f, 0.0f);
            } else if (livingEntity instanceof Player player && !player.isSneaking()) {
                factor = Math.max(1.0, Math.min(1.0, (time - startTime) / (double) maxChargeTime) * chargeFactor);
                launch();
            }
        } else {
            streams.removeIf(stream -> !stream.update());
        }

        return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
    }

    private void launch() {
        charging = false;
        user.setCooldown(this);

        Location eyeLocation = livingEntity.getEyeLocation();

        origin = new Vector3d(eyeLocation);

        Vector3d up = Vector3d.PLUS_J;
        Vector3d lookingDir = new Vector3d(eyeLocation.getDirection());
        Vector3d right = lookingDir.cross(up).normalize(Vector3d.PLUS_I);
        Vector3d rotateAxis = right.cross(lookingDir);

        double halfArc = arc / 2.0;

        for (double deg = -halfArc; deg <= halfArc; deg += arcStep) {
            double rads = Math.toRadians(deg);

            Vector3d direction = lookingDir;
            direction = VectorUtils.rotate(direction, rotateAxis, rads);

            streams.add(new SwipeStream(origin, direction));
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
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
        private Vector3d location;
        private final Vector3d direction;
        @Getter
        private Collider collider;

        SwipeStream(Vector3d origin, Vector3d direction) {
            this.location = origin;
            this.direction = direction;
        }

        // Return false to destroy this stream
        boolean update() {
            location = location.add(direction.multiply(speed));

            if (!user.canUse(location.toLocation(world))) {
                return false;
            }

            if (location.distance(origin) >= range) {
                return false;
            }

            AirElement.display(location.toLocation(world), 1,0.0f, 0.0f, 0.0f, 0.0f);

            this.collider = new Sphere(location, abilityCollisionRadius);

            Collider collider = new Sphere(location, entityCollisionRadius);
            if (collide(collider)) {
                return false;
            }

            for (Block handleBlock : WorldUtils.getNearbyBlocks(location.toLocation(world), 2.0)) {
                AirElement.handleBlockInteractions(user, handleBlock);
            }
            return CollisionUtils.handleBlockCollisions(livingEntity, new Sphere(location, 0.5), o -> false, block -> !block.isPassable() || block.isLiquid(), false).size() == 0;
        }

        // Returns true if it collides with an entity
        private boolean collide(Collider collider) {
            return CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                if (!user.canUse(entity.getLocation())) {
                    return false;
                }

                entity.setVelocity(direction.multiply(factor).toBukkitVector());

                if (entity instanceof LivingEntity && !affectedEntities.contains(entity)) {
                    ((LivingEntity) entity).damage(damage * factor, livingEntity);
                    affectedEntities.add(entity);
                    return false;
                }

                return false;
            }, false);
        }
    }
}
