package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import ru.ckateptb.abilityslots.ability.Ability;
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
import ru.ckateptb.abilityslots.avatar.air.ability.AirShield;
import ru.ckateptb.abilityslots.avatar.air.ability.Tornado;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Twister",
        displayName = "Twister",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Create a cyclone of air that travels along the ground grabbing nearby entities.",
        instruction = "AirShield \\(Tap Shift\\) > Tornado \\(Hold Shift\\) > AirBlast \\(Left Click\\)",
        cooldown = 4000,
        cost = 20,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK_RELEASE),
        @AbilityAction(ability = Tornado.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK)
})
@CollisionParticipant
public class Twister extends Ability {
    @ConfigField
    private static long duration = 8000;
    @ConfigField
    private static double radius = 3.5;
    @ConfigField
    private static double height = 8;
    @ConfigField
    private static double range = 25;
    @ConfigField
    private static double speed = 0.35;
    @ConfigField
    private static double proximity = 2.0;
    @ConfigField
    private static double renderSpeed = 2.5;
    @ConfigField
    private static int streams = 6;
    @ConfigField
    private static int particlesPerStream = 7;

    private long startTime;
    private ImmutableVector base;
    private ImmutableVector direction;
    private ImmutableVector origin;
    private double currentHeight;
    private RemovalConditional removal;
    private final List<Collider> colliders = new ArrayList<>();
    private final Set<Entity> affected = new HashSet<>();

    @Override
    public ActivateResult activate(ActivationMethod method) {
        user.destroyInstances(AirBlast.class);
        this.startTime = System.currentTimeMillis();
        this.direction = user.getDirection().setY(0).normalize();
        this.base = user.getLocation().add(direction.multiply(2));
        this.currentHeight = height;
        Block ground = this.base.add(0, currentHeight / 2, 0).getFirstRelativeBlock(world, BlockFace.DOWN, currentHeight);
        if (!isAcceptableBase(ground) || !user.removeEnergy(this)) {
            return ActivateResult.NOT_ACTIVATE;
        }
        this.base = base.setY(ground.getY());
        this.origin = base;
        this.user.setCooldown(this);
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> base.toLocation(world))
                .custom((user, ability) -> !isAcceptableBase(base.toBlock(world)))
                .build();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        Block top = this.base.add(0, 1, 0).getFirstRelativeBlock(world, BlockFace.UP, currentHeight);
        currentHeight = top.getY() - (base.getY() + 1);
        if (currentHeight < height / 2) return UpdateResult.REMOVE;
        colliders.clear();
        for (int i = 0; i < currentHeight - 1; ++i) {
            ImmutableVector location = base.add(0, i, 0);
            double r = proximity + radius * (i / currentHeight);
            AxisAlignedBoundingBoxCollider aabb = new AxisAlignedBoundingBoxCollider(world, new ImmutableVector(-r, 0, -r), new ImmutableVector(r, 1, r));
            colliders.add(new DiskCollider(world, new OrientedBoundingBoxCollider(aabb), new SphereCollider(world, location, r)).at(location));
        }
        for (Collider collider : colliders) {
            collider.handleEntityCollision(livingEntity, false, entity -> {
                if (user.canUse(entity.getLocation())) {
                    affected.add(entity);
                }
                return CollisionCallbackResult.CONTINUE;
            });
        }
        for (Entity entity : affected) {
            AbilityTarget target = AbilityTarget.of(entity);
            target.setVelocity(base.add(0, currentHeight, 0).subtract(target.getLocation()).normalize().multiply(speed), this);
        }
        render();
        if(base.distance(origin) < range) {
            this.base = base.add(direction.multiply(speed));
            Block ground = this.base.add(0, currentHeight / 2, 0).getFirstRelativeBlock(world, BlockFace.DOWN, currentHeight);
            this.base = base.setY(ground.getY());
        } else if (System.currentTimeMillis() > startTime + duration) {
                return UpdateResult.REMOVE;
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public List<Collider> getColliders() {
        return colliders;
    }

    private void render() {
        long time = System.currentTimeMillis();
        double cycleMS = (1000.0 * currentHeight / renderSpeed);

        for (int j = 0; j < streams; ++j) {
            double thetaOffset = j * ((Math.PI * 2) / streams);

            for (int i = 0; i < particlesPerStream; ++i) {
                double thisTime = time + (i / (double) particlesPerStream) * cycleMS;
                double f = (thisTime - startTime) / cycleMS % 1.0;

                double y = f * currentHeight;
                double theta = y + thetaOffset;

                double x = radius * f * Math.cos(theta);
                double z = radius * f * Math.sin(theta);

                if (y > currentHeight) {
                    continue;
                }

                AirElement.display(base.toLocation(world).add(x, y, z), 1, 0.0f, 0.0f, 0.0f, false);
            }
        }
        AirElement.sound(base.toLocation(world));
    }

    private boolean isAcceptableBase(Block block) {
        return !(MaterialUtils.isTransparent(block) && MaterialUtils.isTransparent(block.getRelative(BlockFace.DOWN)));
    }
}
