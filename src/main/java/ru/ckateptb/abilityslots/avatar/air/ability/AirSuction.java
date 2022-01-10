package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.general.AirFlow;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.OutOfRangeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.SwappedSlotsRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSuction",
        displayName = "AirSuction",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "A strong but harmless stream of air that flies from point A to point B, capturing everything in its path. If item B is not specified, it will be specified automatically",
        instruction = "Tap Sneak to indicate the destination (point B, optional). Left Click",
        cooldown = 1250
)
@CollisionParticipant(destroyAbilities = {
        AirSuction.class,
        AirSwipe.class,
        AirBlast.class
})
public class AirSuction implements Ability, AirFlow {
    private AbilityUser user;
    private LivingEntity entity;
    @Setter
    private Location original;
    private Location location;
    private Vector3d direction;
    @Setter
    private boolean pushSelf;
    private AirBlast blast;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        return getActivationResult(abilityUser, activationMethod, getAbilityInstanceService().getAbilityUserInstances(abilityUser, getClass()).stream().filter(ability -> ability.getLocation() == null).findFirst());

    }

    public void launch() {
        Location eyeLocation = entity.getEyeLocation();
        if (this.original == null) {
            this.original = eyeLocation;
            this.pushSelf = false;
        }
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        boolean ignoreLiquids = eyeLocation.getBlock().isLiquid();
        RayTrace.CompositeResult result = RayTrace.of(new Vector3d(eyeLocation), direction)
                .type(RayTrace.Type.COMPOSITE)
                .filter(e -> e instanceof LivingEntity && e != entity)
                .range(AirBlast.getDistance()).ignoreLiquids(ignoreLiquids)
                .result(entity.getWorld());
        LivingEntity livingEntity = (LivingEntity) result.entity();
        if (livingEntity != null) {
            this.location = WorldUtils.getEntityCenter(livingEntity).toLocation(livingEntity.getWorld());
        } else {
            this.location = result
                    .position()
                    .subtract(direction.multiply(0.5))
                    .toLocation(eyeLocation.getWorld());
        }
        this.direction = new Vector3d(original.clone().subtract(location)).normalize();

        try {
            this.blast = AirBlast.class.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return;
        }

        this.blast.initialize(user, this.location, this.direction, this.pushSelf);

        this.user.setCooldown(this);
    }


    public boolean selectOriginal() {
        return AirBlast.selectOriginal(this);
    }

    @Override
    public UpdateResult update() {
        if (this.original == null) return UpdateResult.REMOVE;
        if (this.blast != null) {
            return this.blast.update();
        } else {
            if (new CompositeRemovalPolicy(
                    new OutOfRangeRemovalPolicy(() -> this.original, () -> this.entity.getLocation(), AirBlast.getSelectRange() + 2),
                    new SwappedSlotsRemovalPolicy<>(user, AirSuction.class)
            ).shouldRemove()) {
                return UpdateResult.REMOVE;
            }
            AirElement.display(this.original, 4, 0.5f, 0.5f, 0.5f);
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser abilityUser) {
        this.user = abilityUser;
        this.entity = abilityUser.getEntity();
    }

    @Override
    public Collection<Collider> getColliders() {
        if (blast == null) {
            return Collections.emptyList();
        }
        return blast.getColliders();
    }
}
