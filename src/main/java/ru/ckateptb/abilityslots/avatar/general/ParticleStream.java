package ru.ckateptb.abilityslots.avatar.general;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

public abstract class ParticleStream {
    protected Location origin;
    protected Location location;
    protected Vector3d direction;
    protected double range;
    protected double speed;
    protected double entityCollisionRadius;
    protected double abilityCollisionRadius;
    protected double blockCollisionRadius;
    protected double damage;
    protected Collider collider;
    private AbilityUser user;
    private LivingEntity entity;
    private World world;

    public ParticleStream(AbilityUser user, Location origin, Vector3d direction,
                          double range, double speed, double entityCollisionRadius, double abilityCollisionRadius,
                          double damage) {
        this(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, 0.01, damage);
    }

    public ParticleStream(AbilityUser user, Location origin, Vector3d direction,
                          double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double blockCollisionRadius,
                          double damage) {
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

        this.collider = new Sphere(new Vector3d(location), abilityCollisionRadius);
    }

    // Return false to destroy this stream
    public boolean update() {
        Location previous = location.clone();
        location.add(direction.multiply(speed).toBukkitVector());

        if (!user.canUse(location)) {
            return false;
        }

        render();

        this.collider = new Sphere(new Vector3d(location), abilityCollisionRadius);

        Sphere entityCollider = new Sphere(new Vector3d(location), entityCollisionRadius);
        boolean hit = CollisionUtils.handleEntityCollisions(this.entity, entityCollider, this::onEntityHit, true);

        if (hit || location.distance(origin) > range) {
            return false;
        }

        if (previous.equals(origin)) return true;

        Sphere blockCollider = new Sphere(new Vector3d(location), blockCollisionRadius);
        return CollisionUtils.handleBlockCollisions(this.entity, blockCollider, o -> false, block -> block.getType().isSolid(), true).size() == 0;
    }

    public abstract void render();

    public boolean onEntityHit(Entity entity) {
        if (!user.canUse(entity.getLocation())) {
            return false;
        }
        ((LivingEntity) entity).damage(damage, this.entity);
        return true;
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
