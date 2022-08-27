package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.Collection;
import java.util.Collections;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.util.VectorUtils;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "SonicBlast",
        displayName = "SonicBlast",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Compresses air, making a sound so loud that targets affected by it lose focus and take damage",
        instruction = "Hold Sneak for charge up then Release Sneak",
        cooldown = 3500,
        cost = 10
)
@CollisionParticipant
public class SonicBlast extends Ability {
    @ConfigField
    private static double damage = 4;
    @ConfigField
    private static double range = 20;
    @ConfigField
    private static double radius = 2;
    @ConfigField
    private static double speed = 1;
    @ConfigField
    private static long chargeTime = 500;
    @ConfigField
    private static long duration = 2000;
    @ConfigField
    private static int power = 2;

    private long startTime;
    private boolean charged;
    private ImmutableVector location;
    private ImmutableVector origin;
    private ImmutableVector direction;
    private Collider collider;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.startTime = System.currentTimeMillis();
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .slot()
                .canUse(() -> livingEntity.getLocation())
                .build();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        if (direction == null && user.isSneaking()) {
            if (System.currentTimeMillis() > startTime + chargeTime) {
                charged = true;
                ImmutableVector direction = user.getDirection();
                ImmutableVector location = user.getEyeLocation().add(direction);
                ImmutableVector side = direction.crossProduct(ImmutableVector.PLUS_J).normalize(ImmutableVector.PLUS_I);
                ImmutableVector left = location.subtract(side.multiply(0.5));
                ImmutableVector right = location.add(side.multiply(0.5));
                AirElement.display(left.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
                AirElement.display(right.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
            }
        } else {
            if (!charged) return UpdateResult.REMOVE;
            if (direction == null) {
                if (!user.removeEnergy(this)) return UpdateResult.REMOVE;
                direction = user.getDirection();
                location = user.getEyeLocation();
                origin = location;
                world.playSound(location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
                user.setCooldown(this);
                removal = new RemovalConditional.Builder()
                        .offline()
                        .dead()
                        .world()
                        .range(() -> origin.toLocation(world), () -> location.toLocation(world), range)
                        .canUse(() -> location.toLocation(world))
                        .build();
            }
            if (new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false)) return UpdateResult.REMOVE;
            ImmutableVector side = ImmutableVector.PLUS_J.crossProduct(this.direction).normalize();
            VectorUtils.circle(side.multiply(radius), this.direction, 40).forEach(v ->
                    AirElement.display(location.add(v).toLocation(world), 1, 0, 0, 0, false)
            );
            AirElement.sound(location.toLocation(world));
            this.collider = new SphereCollider(world, location, radius);
            this.collider.handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            if (this.collider.handleEntityCollision(livingEntity, entity -> {
                LivingEntity target = (LivingEntity) entity;
                AbilityTargetLiving abilityTarget = AbilityTarget.of(target);
                abilityTarget.damage(damage, this);
                handlePotionEffect(abilityTarget, PotionEffectType.CONFUSION, power, (int) duration / 50);
                handlePotionEffect(abilityTarget, PotionEffectType.BLINDNESS, power, (int) duration / 50);
                return CollisionCallbackResult.END;
            })) return UpdateResult.REMOVE;
            location = location.add(direction.multiply(speed));
        }
        return UpdateResult.CONTINUE;
    }

    private void handlePotionEffect(AbilityTargetLiving target, PotionEffectType type, int amplifier, int duration) {
        PotionEffect effect = target.getPotionEffect(this, type);
        if (effect == null || effect.getDuration() < duration || effect.getAmplifier() < amplifier) {
            target.addPotionEffect(this, new PotionEffect(type, duration, amplifier - 1, true, false));
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public Collection<Collider> getColliders() {
        return this.collider == null ? Collections.emptyList() : Collections.singleton(this.collider);
    }
}
