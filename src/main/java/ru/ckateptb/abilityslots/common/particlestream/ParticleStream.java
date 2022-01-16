package ru.ckateptb.abilityslots.common.particlestream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.math.ImmutableVector;

public abstract class ParticleStream {
    protected Location origin;
    protected Location location;
    protected ImmutableVector direction;
    protected double range;
    protected double speed;
    protected double entityCollisionRadius;
    protected double abilityCollisionRadius;
    protected double blockCollisionRadius;
    protected double damage;
    protected boolean ignoreLiquids;
    protected Collider collider;
    private final AbilityUser user;
    private final LivingEntity entity;
    private final World world;

    public ParticleStream(AbilityUser user, Location origin, ImmutableVector direction,
                          double range, double speed, double entityCollisionRadius, double abilityCollisionRadius,
                          double damage, boolean ignoreLiquids) {
        this(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, 0.1, damage, ignoreLiquids);
    }

    public ParticleStream(AbilityUser user, Location origin, ImmutableVector direction,
                          double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double blockCollisionRadius,
                          double damage) {
        this(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, blockCollisionRadius, damage, true);
    }

    public ParticleStream(AbilityUser user, Location origin, ImmutableVector direction,
                          double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double blockCollisionRadius,
                          double damage, boolean ignoreLiquids) {
        this.user = user;
        this.entity = user.getEntity();
        this.world = entity.getWorld();
        this.origin = origin;
        this.location = origin.clone();
        this.direction = direction;
        this.range = range;
        this.speed = speed;
        this.entityCollisionRadius = entityCollisionRadius;
        this.abilityCollisionRadius = abilityCollisionRadius;
        this.blockCollisionRadius = blockCollisionRadius;
        this.damage = damage;
        this.ignoreLiquids = ignoreLiquids;

        this.collider = new SphereCollider(world, new ImmutableVector(location), abilityCollisionRadius);
    }

    // Return false to destroy this stream
    public boolean update() {
        Location previous = location.clone();
        location.add(direction.multiply(speed));

        if (!user.canUse(location)) {
            return false;
        }

        SphereCollider blockCollider = new SphereCollider(world, new ImmutableVector(location), blockCollisionRadius);
        if (blockCollider.handleBlockCollisions(true, false, block -> CollisionCallbackResult.END, block -> true)) {
            return false;
        }

        render();

        this.collider = new SphereCollider(world, new ImmutableVector(location), abilityCollisionRadius);

        SphereCollider entityCollider = new SphereCollider(world, new ImmutableVector(location), entityCollisionRadius);
        boolean hit = entityCollider.handleEntityCollision(this.entity, this::onEntityHit);

        return !hit && !(location.distance(origin) > range);
    }

    public abstract void render();

    public CollisionCallbackResult onEntityHit(Entity entity) {
        if (!user.canUse(entity.getLocation())) {
            return CollisionCallbackResult.CONTINUE;
        }
        ((LivingEntity) entity).damage(damage, this.entity);
        return CollisionCallbackResult.END;
    }

    public Location getLocation() {
        return location;
    }

    public Location getOrigin() {
        return origin;
    }

    public Collider getCollider() {
        return collider;
    }

    public World getWorld() {
        return world;
    }
}
