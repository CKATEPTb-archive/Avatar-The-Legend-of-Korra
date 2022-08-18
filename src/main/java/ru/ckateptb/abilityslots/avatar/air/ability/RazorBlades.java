package ru.ckateptb.abilityslots.avatar.air.ability;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.LivingEntity;

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
import ru.ckateptb.tablecloth.collision.collider.AxisAlignedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.DiskCollider;
import ru.ckateptb.tablecloth.collision.collider.OrientedBoundingBoxCollider;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "RazorBlades",
        displayName = "RazorBlades",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Маги воздуха могут создававать лезвия из воздуха",
        instruction = "Кликнуть ЛКМ",
        cooldown = 7000,
        cost = 5
)
@CollisionParticipant
public class RazorBlades extends Ability {
	@ConfigField
    private static double damage = 4;
    @ConfigField
    private static double range = 20;
    @ConfigField
    private static double maxRadius = 3;
    @ConfigField
    private static double minRadius = 0.3;
    @ConfigField
    private static double speed = 1.3;

    private double angle;
    private double currentRadius;
    private ImmutableVector original;
    private ImmutableVector location;
    private ImmutableVector direction;
    private RemovalConditional removal;
    private Collider collider;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.currentRadius = maxRadius;
        this.original = user.getEyeLocation();
        this.direction = user.getDirection();
        this.location = original.add(direction.multiply(currentRadius));
        if (!user.canUse(this.location.toLocation(world)) || !user.removeEnergy(this))
            return ActivateResult.NOT_ACTIVATE;
        this.angle = Math.toRadians(livingEntity.getEyeLocation().getYaw());
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .range(() -> this.original.toLocation(world), () -> this.location.toLocation(world), range)
                .custom((user, ability) -> currentRadius < minRadius)
                .build();
        user.setCooldown(this);
        return ActivateResult.ACTIVATE;
    }

    public DiskCollider getCollider() {
        AxisAlignedBoundingBoxCollider bounds = new AxisAlignedBoundingBoxCollider(world, new ImmutableVector(-0.15, -currentRadius, -currentRadius), new ImmutableVector(0.15, currentRadius, currentRadius));
        OrientedBoundingBoxCollider obb = new OrientedBoundingBoxCollider(bounds, ImmutableVector.PLUS_J, angle);
        return new DiskCollider(world, obb, new SphereCollider(world, currentRadius)).at(location);
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        this.collider = getCollider();
        if (this.collider.handleEntityCollision(livingEntity, entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                AbilityTargetLiving target = AbilityTarget.of(livingEntity);
                target.damage(damage, this);
            }
            return CollisionCallbackResult.END;
        })) return UpdateResult.REMOVE;
        while (this.getCollider().handleBlockCollisions(true)) {
            currentRadius -= 0.1;
            if (currentRadius < minRadius) break;
        }
        CompletableFuture.runAsync(() -> {
            ImmutableVector rotateAxis = ImmutableVector.PLUS_J.setX(1).crossProduct(this.direction);
            VectorUtils.circle(this.direction.multiply(currentRadius), rotateAxis, 40).forEach(v ->
                    AirElement.display(location.add(v).toLocation(world), 1, 0, 0, 0, false)
            );
            rotateAxis = ImmutableVector.PLUS_J.setX(-1).crossProduct(this.direction);
            VectorUtils.circle(this.direction.multiply(currentRadius), rotateAxis, 40).forEach(v ->
            		AirElement.display(location.add(v).toLocation(world), 1, 0, 0, 0, false)
            );
            
            
            
            AirElement.sound(location.toLocation(world));
        });
        this.location = location.add(direction.multiply(speed));
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public Collection<Collider> getColliders() {
        return collider == null ? Collections.emptyList() : Collections.singleton(collider);
    }
}
