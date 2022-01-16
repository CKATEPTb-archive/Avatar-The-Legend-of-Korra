package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.util.MaterialUtils;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.DiskCollider;
import ru.ckateptb.tablecloth.collision.collider.OrientedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Tornado",
        displayName = "Tornado",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "An extremely powerful ability that creates a controlled tornado that is capable of lifting all targets in its path into the air",
        instruction = "Hold Sneak",
        cooldown = 3500,
        cost = 30
)
@CollisionParticipant
public class Tornado extends Ability {
    @ConfigField
    private static long duration = 8000;
    @ConfigField
    private static double radius = 7;
    @ConfigField
    private static double height = 15;
    @ConfigField
    private static double range = 5;
    @ConfigField
    private static double moveSpeed = 0.3;
    @ConfigField
    private static long growthTime = 2500;
    @ConfigField
    private static boolean elevateOther = true;
    @ConfigField
    private static boolean elevateToCenter = true;
    @ConfigField
    private static int particlesPerStream = 30;
    @ConfigField(comment = "Set 0 for realistic tornado")
    private static int streams = 0;
    @ConfigField
    private static double renderSpeed = 4;
    @ConfigField
    private static long energyCostInterval = 1000;

    private long startTime;
    private double yOffset = 0;
    private double currentAngle = 0;
    private final List<Collider> colliders = new ArrayList<>();
    private ImmutableVector location;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .duration(duration)
                .costInterval(energyCostInterval)
                .slot()
                .sneaking(true)
                .build();
        this.startTime = System.currentTimeMillis();
        return getDirection() == null ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;

        long time = System.currentTimeMillis();

        double factor = Math.min(1, time - startTime / growthTime);
        double height = 2.0 + factor * (Tornado.height - 2.0);
        double radius = 2.0 + factor * (Tornado.radius - 2.0);

        ImmutableVector direction = getDirection();

        if (direction != null) {
            location = location.add(direction.multiply(moveSpeed));
        }
        Location baseLocation = location.toLocation(world);

        if (!user.canUse(baseLocation)) return UpdateResult.REMOVE;

        colliders.clear();

        if (MaterialUtils.isTransparent(baseLocation.getBlock().getRelative(BlockFace.DOWN))) {
            return UpdateResult.CONTINUE;
        }

        for (int i = 0; i < height - 1; ++i) {
            double r = 2.0 + (radius - 2.0) * (i / height);
            ImmutableVector location = this.location.add(0, i, 0);
            AxisAlignedBoundingBoxCollider aabb = new AxisAlignedBoundingBoxCollider(world, new ImmutableVector(-r, 0, -r), new ImmutableVector(r, 1, r));

            colliders.add(new DiskCollider(world, new OrientedBoundingBoxCollider(aabb), new SphereCollider(world, location, r)).at(location));
        }

        for (Collider collider : colliders) {
            collider.handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            collider.handleEntityCollision(livingEntity, false, true, entity -> {
                AbilityTarget target = AbilityTarget.of(entity);
                double dy = entity.getLocation().getY() - location.getY();
                double r = 2 + (radius - 2) * dy;
                ImmutableVector delta = target.getCenterLocation().subtract(location);
                double distSq = delta.getX() * delta.getX() + delta.getZ() * delta.getZ();
                if (distSq > r * r) {
                    return CollisionCallbackResult.CONTINUE;
                }

                ImmutableVector velocity;
                if (elevateOther || entity.equals(livingEntity)) {
                    double velY;
                    if (dy >= height * .95) {
                        velY = 0;
                    } else if (dy >= height * .85) {
                        velY = 6.0 * (.95 - dy / height);
                    } else {
                        velY = 0.6;
                    }
                    if (elevateToCenter) {
                        velocity = location.add(0, height, 0).subtract(entity.getLocation().toVector());
                    } else {
                        velocity = direction == null ? ImmutableVector.ZERO : direction;
                    }
                    velocity = velocity.setY(velY).multiply(factor);
                } else {
                    ImmutableVector normal = delta.setY(0).normalize();
                    ImmutableVector ortho = normal.crossProduct(ImmutableVector.PLUS_J).normalize();
                    velocity = ortho.add(normal).normalize().multiply(factor);
                }
                target.setVelocity(velocity.multiply(Math.min(moveSpeed, 1)), this);
                return CollisionCallbackResult.CONTINUE;
            });
        }
        if (streams > 0) {
            render(location, factor, height, radius);
        } else {
            realisticRender(location, factor, height, radius);
        }
        return UpdateResult.CONTINUE;
    }

    private ImmutableVector getDirection() {
        ImmutableVector target = user.findPosition(range, false);

        Block ground = target.getFirstRelativeBlock(world, BlockFace.DOWN, (range * range) + height + 1);

        if (!ground.isLiquid() && ground.isPassable()) {
            return null;
        }

        ImmutableVector groundVector = target.setY(ground.getY());

        if (location == null) {
            location = groundVector;
        }

        ImmutableVector direction = groundVector.subtract(location);

        if (direction.lengthSquared() > 0) {
            direction = direction.normalize();
        }

        return direction;
    }

    private void realisticRender(ImmutableVector base, double factor, double height, double radius) {
        double amount = Math.min(30, Math.max(4, factor * particlesPerStream));
        yOffset += 0.1;
        if (yOffset >= 1) {
            yOffset = 0;
        }
        currentAngle += 18;
        if (currentAngle >= 360) {
            currentAngle = 0;
        }
        for (int i = 0; i < 3; i++) {
            double offset = currentAngle + i * 2 * Math.PI / 3.0;
            for (double y = yOffset; y < height; y += (height / amount)) {
                double r = radius * y / height;
                double x = r * Math.cos(y + offset);
                double z = r * Math.sin(y + offset);
                AirElement.display(base.add(x, y, z).toLocation(world), 1, 0, 0, 0, false);
            }
        }
        AirElement.sound(base.toLocation(world));
    }

    private void render(ImmutableVector base, double factor, double height, double radius) {
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

                AirElement.display(base.add(x, y, z).toLocation(world), 1, 0, 0, 0, false);
            }
        }
        AirElement.sound(base.toLocation(world));
    }

    @Override
    public void destroy() {
        this.user.setCooldown(this);
    }

    @Override
    public Collection<Collider> getColliders() {
        return this.colliders;
    }
}
