package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.LivingEntity;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
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
        name = "AirBreath",
        displayName = "AirBreath",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Releases air from your lungs with such force that you can lift yourself up or push your enemies away",
        instruction = "Hold Sneak",
        cooldown = 3500,
        cost = 10
)
@CollisionParticipant
public class AirBreath extends Ability {
    @ConfigField
    private static long duration = 7000;
    @ConfigField
    private static double damage = 0;
    @ConfigField
    private static int particles = 1;
    @ConfigField
    private static double range = 10;
    @ConfigField
    private static double power = 0.5;
    @ConfigField
    private static double knockback = 0.8;
    @ConfigField
    private static int angle = 30;
    @ConfigField
    private static long energyCostInterval = 1000;

    private ImmutableVector direction;
    private final List<Collider> colliders = new ArrayList<>();
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .slot()
                .duration(duration)
                .sneaking(true)
                .costInterval(energyCostInterval)
                .build();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        ImmutableVector eyeLocation = user.getEyeLocation();
        this.direction = user.getDirection();
        double step = 1;
        double size = 0;
        colliders.clear();
        for (double i = 0; i < range; i += step) {
            eyeLocation = eyeLocation.add(direction.multiply(step));
            size += 0.005;
            if (new SphereCollider(world, eyeLocation, 0.1).handleBlockCollisions(true)) {
                if (livingEntity.getLocation().getPitch() > angle) {
                    user.setVelocity(direction.multiply(-power), this);
                }
                return UpdateResult.CONTINUE;
            }
            Collider collider = new SphereCollider(world, eyeLocation, 2);
            colliders.add(collider);
            collider.handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            collider.handleEntityCollision(livingEntity, false, entity -> {
                if (user.canUse(entity.getLocation())) {
                    if (damage > 0 && entity instanceof LivingEntity target) {
                        AbilityTarget.of(target).damage(damage, this);
                    }
                    AbilityTarget.of(entity).setVelocity(direction.multiply(knockback), this);
                }
                return CollisionCallbackResult.CONTINUE;
            });
            AirElement.display(eyeLocation.toLocation(world), particles, (float) size, (float) Math.random(), (float) Math.random(), (float) Math.random(), false);
        }
        AirElement.sound(eyeLocation.toLocation(world));
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
    }

    @Override
    public List<Collider> getColliders() {
        return colliders;
    }
}
