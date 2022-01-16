package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import ru.ckateptb.abilityslots.avatar.air.ability.AirSuction;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.spring.SpringContext;
import ru.ckateptb.tablecloth.temporary.TemporaryService;
import ru.ckateptb.tablecloth.temporary.flight.TemporaryFlight;

import java.util.*;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirStream",
        displayName = "AirStream",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Creates a controlled flow of air that you can control and capture all targets in its path",
        instruction = "AirShield \\(Hold Sneak\\) > AirSuction \\(Left Click\\) > AirBlast \\(Left Click\\)",
        cooldown = 7000,
        cost = 20,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirShield.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirSuction.class, action = SequenceAction.LEFT_CLICK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.LEFT_CLICK)
})
@CollisionParticipant
public class AirStream extends Ability {
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
    @ConfigField
    private static long energyCostInterval = 1000;

    private ImmutableVector location;
    private ImmutableVector origin;
    private final List<TemporaryFlight> flights = new ArrayList<>();
    private RemovalConditional removal;
    private SphereCollider collider;
    private final List<TailData> tail = new ArrayList<>();
    private int tailIndex;
    private final Map<Entity, Long> affected = new HashMap<>();
    private final List<Entity> immune = new ArrayList<>();

    @Override
    public ActivateResult activate(ActivationMethod method) {
        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        abilityInstanceService.destroyInstanceType(user, AirBlast.class);
        abilityInstanceService.destroyInstanceType(user, AirSuction.class);

        this.location = user.getEyeLocation();
        this.origin = this.location;

        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .costInterval(energyCostInterval)
                .sneaking(true)
                .build();

        this.collider = new SphereCollider(world, location, radius);

        return origin.toBlock(world).isLiquid() ? ActivateResult.NOT_ACTIVATE : ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        long time = System.currentTimeMillis();

        ImmutableVector previous = location;
        ImmutableVector direction = getDirection();
        location = location.add(direction.multiply(speed));
        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        for (AirBlast blast : abilityInstanceService.getAbilityUserInstances(user, AirBlast.class)) {
            ImmutableVector blastLocation = blast.getLocation();
            if (blastLocation.distance(origin) < range) {
                this.location = blastLocation;
            }
        }
        collider = new SphereCollider(world, location, radius);
        if (location.distance(origin) > range || new SphereCollider(world, location, 0.1).handleBlockCollisions(false)) {
            location = previous;
        }
        if (location.getY() - origin.getY() > height) {
            location = location.setY(previous.getY());
        }

        handleTail(direction);

        collider.handleBlockCollisions(false, false, block -> {
            AirElement.handleBlockInteractions(user, block);
            return CollisionCallbackResult.CONTINUE;
        }, block -> user.canUse(block.getLocation()));

        collider.handleEntityCollision(livingEntity, false, (entity) -> {
            if (!affected.containsKey(entity) && !immune.contains(entity)) {
                affected.put(entity, System.currentTimeMillis() + entityDuration);
                if (entity instanceof Player) {
                    flights.add(new TemporaryFlight((LivingEntity) entity, entityDuration, true, false, true));
                }
            }
            return CollisionCallbackResult.CONTINUE;
        });

        for (Iterator<Map.Entry<Entity, Long>> iterator = affected.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Entity, Long> entry = iterator.next();
            Entity entity = entry.getKey();
            AbilityTarget target = AbilityTarget.of(entity);
            ImmutableVector targetLocation = target.getLocation();
            long end = entry.getValue();
            if (targetLocation.distance(this.location) > 5 || time > end) {
                if (entity instanceof Player) {
                    flights.removeIf((flight) -> {
                        if(flight.getLivingEntity().equals(entity)) {
                            SpringContext.getInstance().getBean(TemporaryService.class).revert(flight);
                            return true;
                        }
                        return false;
                    });
                }
                if(time > end) {
                    immune.add(entity);
                }
                iterator.remove();
                continue;
            }
            entity.setFallDistance(0);
            ImmutableVector force = this.location.subtract(targetLocation);
            if (force.lengthSquared() == 0) {
                continue;
            }

            force = force.normalize().multiply(speed);
            target.setVelocity(force, this);
        }

        return UpdateResult.CONTINUE;
    }

    private void handleTail(ImmutableVector direction) {
        TailData newData = new TailData(location, direction);

        if (tail.size() <= tailIndex) {
            tail.add(newData);
        } else {
            tail.set(tailIndex, newData);
        }

        tailIndex = ++tailIndex % tailCount;

        for (TailData data : tail) {
            ImmutableVector side = ImmutableVector.PLUS_J.crossProduct(data.direction).normalize(ImmutableVector.PLUS_I);

            if (side.lengthSquared() > 0) {
                side = side.normalize();
            } else {
                side = ImmutableVector.PLUS_I;
            }

            for (double theta = 0; theta < Math.PI * 2; theta += Math.toRadians(45)) {
                ImmutableVector offset = side.rotate(data.direction, theta).normalize().multiply(radius);
                AirElement.display(data.location.add(offset).toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
            }
            AirElement.sound(data.location.toLocation(world));
        }
    }

    private ImmutableVector getDirection() {
        ImmutableVector target = user.findPosition(range, false);
        ImmutableVector direction = target.subtract(location);

        if (direction.lengthSquared() > 0) {
            direction = direction.normalize();
        }

        return direction;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
        for (TemporaryFlight flight : flights) {
            SpringContext.getInstance().getBean(TemporaryService.class).revert(flight);
        }
    }

    @Override
    public Collection<Collider> getColliders() {
        return this.collider == null ? Collections.emptyList() : Collections.singletonList(collider);
    }

    private static class TailData {
        public ImmutableVector location;
        public ImmutableVector direction;

        TailData(ImmutableVector location, ImmutableVector direction) {
            this.location = location;
            this.direction = direction;
        }
    }
}
