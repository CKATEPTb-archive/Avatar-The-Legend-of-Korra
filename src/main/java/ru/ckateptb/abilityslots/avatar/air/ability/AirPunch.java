package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.LivingEntity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.AbilityCollisionResult;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirPunch",
        displayName = "AirPunch",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "High density air currents to deal minor damage. Multiple hits can be made before the ability runs out of cooldown.",
        instruction = "Left Click",
        cooldown = 3500,
        cost = 5
)
@CollisionParticipant
public class AirPunch extends Ability {
    @ConfigField
    private static long threshold = 2500;
    @ConfigField
    private static int shots = 3;
    @ConfigField
    private static double range = 30;
    @ConfigField
    private static double speed = 1.5;
    @ConfigField
    private static double damage = 2;

    private final List<PunchStream> streams = new ArrayList<>();
    private int currentShots;
    private long lastShotTime;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        for (AirPunch punch : user.getAbilityInstances(AirPunch.class)) {
            punch.createShot();
            return ActivateResult.NOT_ACTIVATE;
        }
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .build();
        this.currentShots = shots;
        this.createShot();
        return user.removeEnergy(this) ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        streams.removeIf(punch -> punch.update() == UpdateResult.REMOVE);
        return streams.isEmpty() && (currentShots == 0 || System.currentTimeMillis() > lastShotTime + threshold) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
    }

    private void createShot() {
        if (currentShots-- > 0) {
            lastShotTime = System.currentTimeMillis();
            ImmutableVector eyeLocation = user.getEyeLocation();
            ImmutableVector direction = user.getDirection();
            streams.add(new PunchStream(this, eyeLocation, direction));
        }
    }

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream().map(PunchStream::getCollider).collect(Collectors.toList());
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(stream -> destroyedCollider == stream.getCollider());
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class PunchStream {
        private final AirPunch ability;
        private ImmutableVector location;
        private final ImmutableVector direction;
        private final RemovalConditional removal;
        @Getter
        private Collider collider;

        PunchStream(AirPunch ability, ImmutableVector original, ImmutableVector direction) {
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
            this.collider = new SphereCollider(world, location, 0.5);
            if (this.collider.handleEntityCollision(livingEntity, true, entity -> {
                if (user.canUse(entity.getLocation())) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    AbilityTarget.of(livingEntity).damage(damage, ability);
                }
                return CollisionCallbackResult.END;
            })) return UpdateResult.REMOVE;
            return new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        }
    }
}
