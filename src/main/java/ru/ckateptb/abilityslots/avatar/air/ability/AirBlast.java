package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.callback.CollisionCallbackResult;
import ru.ckateptb.tablecloth.collision.collider.SphereCollider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

import java.util.Collection;
import java.util.Collections;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBlast",
        displayName = "AirBlast",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "A strong but harmless stream of air that flies from point B to point A, capturing everything in its path. If item A is not specified, it will be specified automatically",
        instruction = "Tap Sneak to indicate the source \\(point A, optional\\). Left Click",
        cooldown = 1250,
        cost = 5
)
@CollisionParticipant(destroyAbilities = {
        AirSuction.class,
        AirSwipe.class,
        AirBlast.class
})
public class AirBlast extends Ability {
    @ConfigField
    private static double selectRange = 8;
    @ConfigField
    private static double distance = 20;
    @ConfigField
    private static double speed = 1.2;
    @ConfigField
    private static double pushRadius = 1.5;
    @ConfigField
    private static double pushPowerSelf = 2.1;
    @ConfigField
    private static double pushPowerOther = 2.1;

    private ImmutableVector original;
    private ImmutableVector location;
    private ImmutableVector direction;
    private Collider collider;
    private RemovalConditional removal = new RemovalConditional.Builder().offline().dead().world().build();
    private boolean pushSelf;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        AbilityInstanceService instanceService = getAbilityInstanceService();
        AirBlast ability = instanceService.getAbilityUserInstances(user, AirBlast.class).stream().filter(blast -> blast.getDirection() == null).findFirst().orElse(this);
        if (method == ActivationMethod.SNEAK && !ability.selectOriginal()) {
            return ActivateResult.NOT_ACTIVATE;
        }
        if (method == ActivationMethod.LEFT_CLICK && user.removeEnergy(this)) {
            ability.launch();
        }
        return ability == this ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) {
            return UpdateResult.REMOVE;
        } else if (this.direction != null) {
            this.collider = new SphereCollider(world, this.location, pushRadius);
            this.collider.handleBlockCollisions(false, false, block -> {
                AirElement.handleBlockInteractions(user, block);
                return CollisionCallbackResult.CONTINUE;
            }, block -> user.canUse(block.getLocation()));
            this.collider.handleEntityCollision(livingEntity, false, pushSelf, entity -> {
                AbilityTarget target = AbilityTarget.of(entity);
                double pushPower = pushPowerOther;
                if (entity == livingEntity) {
                    pushPower = pushPowerSelf;
                }
                target.setVelocity(this.direction.multiply(pushPower), this);
                entity.setFireTicks(0);
                return CollisionCallbackResult.CONTINUE;
            });
            AirElement.display(location.toLocation(world), 4, 0.5f, 0.5f, 0.5f);
            this.location = this.location.add(this.direction.multiply(speed));
            return new SphereCollider(world, this.location, 0.1).handleBlockCollisions(false) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        } else if (this.original != null) {
            AirElement.display(this.original.toLocation(world), 4, 0.5f, 0.5f, 0.5f);
            return UpdateResult.CONTINUE;
        }
        return UpdateResult.REMOVE;
    }

    @Override
    public void destroy() {

    }

    public boolean selectOriginal() {
        if (this.direction != null) return false;
        boolean ignoreLiquids = user.getEyeLocation().toBlock(world).isLiquid();
        ImmutableVector original = user.findPosition(selectRange, ignoreLiquids);
        Location location = original.toLocation(world);
        if (location.getBlock().isLiquid() || !user.canUse(location)) {
            return false;
        }
        this.original = original;
        this.pushSelf = true;
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> location)
                .range(() -> location, () -> user.getEntity().getLocation(), selectRange * 1.5)
                .slot()
                .build();
        return true;
    }

    public void launch() {
        if (this.direction != null) return;
        this.direction = user.getDirection();
        if (this.original == null) {
            this.original = user.getEyeLocation();
        }
        this.location = this.original;
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .canUse(() -> this.location.toLocation(world))
                .range(() -> this.original.toLocation(world), () -> this.location.toLocation(world), distance)
                .build();
        user.setCooldown(this);
    }

    @Override
    public Collection<Collider> getColliders() {
        return this.collider == null ? Collections.emptyList() : Collections.singleton(this.collider);
    }
}
