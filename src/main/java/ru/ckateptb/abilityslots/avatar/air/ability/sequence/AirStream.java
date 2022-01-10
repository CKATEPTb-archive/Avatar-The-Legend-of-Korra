package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirShield;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSuction;
import ru.ckateptb.abilityslots.common.util.VectorUtils;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.OutOfRangeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.SneakingRemovalPolicy;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.spring.SpringContext;
import ru.ckateptb.tablecloth.temporary.TemporaryService;
import ru.ckateptb.tablecloth.temporary.flight.TemporaryFlight;
import ru.ckateptb.tablecloth.util.CollisionUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirStream",
        displayName = "AirStream",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Creates a controlled flow of air that you can control and capture all targets in its path",
        instruction = "AirShield (Hold Sneak) > AirSuction (Left Click) > AirBlast (Left Click)",
        cooldown = 7000,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirSuction.class, action = SequenceAction.LEFT_CLICK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK)
})
public class AirStream implements Ability {
    @ConfigField
    private static double speed = 0.5;
    @ConfigField
    private static double range = 40;
    @ConfigField
    private static double radius = 1;
    @ConfigField
    private static long entityDuration = 4000;
    @ConfigField
    private static double height = 14;
    @ConfigField
    private static int tailCount = 10;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private Vector3d location;
    private Vector3d origin;
    private final List<TemporaryFlight> flights = new ArrayList<>();
    private CompositeRemovalPolicy removalPolicy;
    private Sphere collider;
    private final List<TailData> tail = new ArrayList<>();
    private int tailIndex;
    private final Map<Entity, Long> affected = new HashMap<>();
    private final List<Entity> immune = new ArrayList<>();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        abilityInstanceService.destroyInstanceType(user, AirBlast.class);
        abilityInstanceService.destroyInstanceType(user, AirSuction.class);

        this.location = new Vector3d(livingEntity.getEyeLocation());
        World world = livingEntity.getWorld();
        this.origin = this.location;

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new OutOfRangeRemovalPolicy(() -> this.origin.toLocation(world), () -> this.location.toLocation(world), range),
                new SneakingRemovalPolicy(user, true)
        );

        this.collider = new Sphere(location, radius);

        return origin.toBlock(world).isLiquid() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long time = System.currentTimeMillis();

        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }

        Vector3d previous = location;
        Vector3d direction = getDirection();
        location = location.add(direction.multiply(speed));
        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        for (AirBlast blast : abilityInstanceService.getAbilityUserInstances(user, AirBlast.class)) {
            Location blastLocation = blast.getLocation();
            if (blastLocation.distance(origin.toLocation(blastLocation.getWorld())) < range)
                location = new Vector3d(blastLocation);
        }
        collider = new Sphere(location, radius);
        if (!user.canUse(location.toLocation(livingEntity.getWorld()))) {
            return UpdateResult.REMOVE;
        }

        if (CollisionUtils.handleBlockCollisions(livingEntity, new Sphere(location, 0.1), o -> false, block -> !block.isPassable() || block.isLiquid(), false).size() > 0) {
            location = previous;
        }

        if (location.getY() - origin.getY() > height) {
            location = location.setY(previous.getY());
//            return UpdateResult.REMOVE;
        }

        handleTail(direction);

        CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
            if (!affected.containsKey(entity) && !immune.contains(entity)) {
                affected.put(entity, System.currentTimeMillis() + entityDuration);

                if (entity instanceof Player) {
                    flights.add(new TemporaryFlight((LivingEntity) entity, 20000, true, false, true));
                }
            }

            return false;
        }, false);

        for (Iterator<Map.Entry<Entity, Long>> iterator = affected.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Entity, Long> entry = iterator.next();
            Entity entity = entry.getKey();
            long end = entry.getValue();

            if (time > end) {
                if (entity instanceof Player) {
                    flights.removeIf((flight) -> flight.getLivingEntity().equals(entity));
//                    new TemporaryFlight(entity, 20000, true, true, false);
                }
                immune.add(entity);
                iterator.remove();
                continue;
            }

            entity.setFallDistance(0);
            Vector3d force = location.subtract(new Vector3d(entity.getLocation()));
            if (force.lengthSq() == 0) {
                continue;
            }

            force = force.normalize().multiply(speed);
            entity.setVelocity(force.toBukkitVector());
        }

        return UpdateResult.CONTINUE;
    }

    private void handleTail(Vector3d direction) {
        TailData newData = new TailData(location, direction);

        if (tail.size() <= tailIndex) {
            tail.add(newData);
        } else {
            tail.set(tailIndex, newData);
        }

        tailIndex = ++tailIndex % tailCount;

        for (TailData data : tail) {
            Vector3d side = Vector3d.PLUS_J.cross(data.direction).normalize(Vector3d.PLUS_I);

            if (side.lengthSq() > 0) {
                side = side.normalize();
            } else {
                side = Vector3d.PLUS_I;
            }

            for (double theta = 0; theta < Math.PI * 2; theta += Math.toRadians(45)) {
                Vector3d offset = VectorUtils.rotate(side, data.direction, theta).normalize().multiply(radius);

                AirElement.display(data.location.add(offset).toLocation(livingEntity.getWorld()), 1, 0.0f, 0.0f, 0.0f, 0.0f, ThreadLocalRandom.current().nextInt(10) == 0);
            }
        }
    }

    private Vector3d getDirection() {
        Vector3d target = RayTrace.of(livingEntity).range(range).ignoreLiquids(false).result(livingEntity.getWorld()).position();
        Vector3d direction = target.subtract(location);

        if (direction.lengthSq() > 0) {
            direction = direction.normalize();
        }

        return direction;
    }

    @Override
    public void destroy() {
        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());

        for (TemporaryFlight flight : flights) {
            SpringContext.getInstance().getBean(TemporaryService.class).revert(flight);
//            new TemporaryFlight(flight.getLivingEntity(), 20000, true, true, false);
        }

    }

    @Override
    public Collection<Collider> getColliders() {
        if (this.collider == null) return Collections.emptyList();
        return Collections.singletonList(collider);
    }

    private static class TailData {
        public Vector3d location;
        public Vector3d direction;

        TailData(Vector3d location, Vector3d direction) {
            this.location = location;
            this.direction = direction;
        }
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
