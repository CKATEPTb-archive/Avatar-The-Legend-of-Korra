package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.util.VectorUtils;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.collision.collider.Disc;
import ru.ckateptb.tablecloth.collision.collider.OBB;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Tornado",
        displayName = "Tornado",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class Tornado implements Ability {
    @ConfigField
    private static long duration = 8000;
    @ConfigField
    private static double radius = 10;
    @ConfigField
    private static double height = 15;
    @ConfigField
    private static double range = 25;
    @ConfigField
    private static double moveSpeed = 1;
    @ConfigField
    private static long growthTime = 2500;
    @ConfigField
    private static boolean elevateOther = true;
    @ConfigField
    private static int particlesPerStream = 10;
    @ConfigField(comment = "Set 0 for realistic tornado")
    private static int streams = 0;
    @ConfigField
    private static double renderSpeed = 4;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private double yOffset = 0;
    private double currentAngle = 0;
    private List<Collider> colliders = new ArrayList<>();
    private Vector3d location;
    private RemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        removalPolicy = new CompositeRemovalPolicy(
                new SwappedSlotsRemovalPolicy<>(user, Tornado.class),
                new DurationRemovalPolicy(duration),
                new SneakingRemovalPolicy(user, true)
        );
        this.startTime = System.currentTimeMillis();
        return ActivateResult.ACTIVATE;
    }

    private Vector3d getDirection() {
        World world = livingEntity.getWorld();
        Vector3d target = RayTrace.of(livingEntity).range(range).ignoreLiquids(false).result(world).position();
        Vector3d top = RayTrace.of(target, Vector3d.MINUS_J).range(range * 2).ignoreLiquids(false).result(world).position();

        Block block = top.toBlock(world);
        if(block.isEmpty() && block.getRelative(BlockFace.DOWN).isEmpty()) {
            return null;
        }

        if (location == null) {
            location = top;
        }

        Vector3d direction = top.subtract(location);

        if (VectorUtils.getNormSq(direction) > 0) {
            direction = direction.normalize();
        }

        return direction;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }

        long time = System.currentTimeMillis();

        double factor = Math.min(1, time - startTime / growthTime);
        double height = 2.0 + factor * (Tornado.height - 2.0);
        double radius = 2.0 + factor * (Tornado.radius - 2.0);

        World world = livingEntity.getWorld();

        Vector3d direction = getDirection();
        if(direction == null) {
            return UpdateResult.REMOVE;
        }

        location = location.add(direction.multiply(moveSpeed));
        Location baseLocation = location.toLocation(world);

        if (!user.canUse(baseLocation)) {
            return UpdateResult.REMOVE;
        }

        colliders.clear();

        if (AABB.from(baseLocation.getBlock().getRelative(BlockFace.DOWN)).max == null) {
            return UpdateResult.CONTINUE;
        }

        for (int i = 0; i < height - 1; ++i) {
            Location location = baseLocation.clone().add(0, i, 0);
            Vector3d vector3d = new Vector3d(location);
            double r = 2.0 + (radius - 2.0) * (i / height);
            AABB aabb = new AABB(new Vector3d(-r, 0, -r), new Vector3d(r, 1, r)).at(vector3d);

            colliders.add(new Disc(new OBB(aabb), new Sphere(vector3d, r)));
        }

        for (Collider collider : colliders) {
            CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                double dy = entity.getLocation().getY() - location.getY();
                double r = 2 + (radius - 2) * dy;
                Vector3d delta = WorldUtils.getEntityCenter(entity).subtract(location);
                double distSq = delta.getX() * delta.getX() + delta.getZ() * delta.getZ();
                if (distSq > r * r) {
                    return false;
                }

                Vector3d velocity;
                if (elevateOther || entity.equals(livingEntity)) {
                    double velY;
                    if (dy >= height * .95) {
                        velY = 0;
                    } else if (dy >= height * .85) {
                        velY = 6.0 * (.95 - dy / height);
                    } else {
                        velY = 0.6;
                    }
                    velocity = new Vector3d(livingEntity.getEyeLocation().getDirection().setY(velY).multiply(factor));
                } else {
                    Vector3d normal = delta.setY(0).normalize();
                    Vector3d ortho = normal.cross(Vector3d.PLUS_J).normalize();
                    velocity = ortho.add(normal).normalize().multiply(factor);
                }
                entity.setVelocity(velocity.multiply(moveSpeed).toBukkitVector());
                return false;
            }, false, true);
        }
        if (streams > 0) {
            render(baseLocation, factor, height, radius);
        } else {
            render(location, factor, height, radius);
        }
        return UpdateResult.CONTINUE;
    }

    private void render(Vector3d base, double factor, double height, double radius) {
        double amount = Math.min(30, Math.max(4, factor * particlesPerStream));
        yOffset += 0.1;
        if (yOffset >= 1) {
            yOffset = 0;
        }
        currentAngle += 4.5;
        if (currentAngle >= 360) {
            currentAngle = 0;
        }
        for (int i = 0; i < 3; i++) {
            double offset = currentAngle + i * 2 * Math.PI / 3.0;
            for (double y = yOffset; y < height; y += (height / amount)) {
                double r = 2 + (radius - 2) * y / height;
                double x = r * Math.cos(y + offset);
                double z = r * Math.sin(y + offset);
                Location loc = base.add(new Vector3d(x, y, z)).toLocation(livingEntity.getWorld());
                AirElement.display(loc, 1, 0, 0, 0, 0, ThreadLocalRandom.current().nextInt(20) == 0);
            }
        }
    }

    private void render(Location base, double factor, double height, double radius) {
        long time = System.currentTimeMillis();
        int particleCount = (int) Math.ceil(factor * particlesPerStream);

        double cycleMS = (1000.0 * height / renderSpeed);

        for (int j = 0; j < streams; ++j) {
            double thetaOffset = j * ((Math.PI * 2) / streams);

            for (int i = 0; i < particleCount; ++i) {
                double thisTime = time + (i / (double) particleCount) * cycleMS;
                double f = (thisTime - startTime) / cycleMS % 1.0;

                double y = f * height;
                double theta = y + thetaOffset;

                double x = (2.0 + ((radius - 2.0) * f)) * Math.cos(theta);
                double z = (2.0 + ((radius - 2.0) * f)) * Math.sin(theta);

                Location current = base.add(x, y, z);
                AirElement.display(current, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void destroy() {
        this.user.setCooldown(getInformation(), getInformation().getCooldown());
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    @Override
    public Collection<Collider> getColliders() {
        return this.colliders;
    }
}
