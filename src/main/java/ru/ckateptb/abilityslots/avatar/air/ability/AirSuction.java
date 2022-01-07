package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.DestroyAbilities;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;
import ru.ckateptb.tablecloth.util.WorldUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSuction",
        displayName = "AirSuction",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 1250
)
@DestroyAbilities(destroyAbilities = {
        AirSuction.class
})
public class AirSuction implements Ability {
    @ConfigField
    private static double selectRange = 8;
    @ConfigField
    private static double distance = 20;
    @ConfigField
    private static double speed = 1.2;
    @ConfigField
    private static double pushRadius = 0.7;
    @ConfigField
    private static double pushPowerSelf = 2.1;
    @ConfigField
    private static double pushPowerOther = 2.1;

    private AbilityUser user;
    private LivingEntity entity;
    private Location destination;
    private Location original;
    private Location location;
    private Vector3d direction;
    private boolean pushSelf;
    private Collider collider;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        ActivateResult result = ActivateResult.ACTIVATE;
        Optional<? extends AirSuction> optional = getAbilityInstanceService().getAbilityUserInstances(abilityUser, getClass()).stream()
                .filter(ability -> ability.getLocation() == null).findFirst();
        AirSuction airSuction = this;
        if (optional.isPresent()) {
            airSuction = optional.get();
            result = ActivateResult.NOT_ACTIVATE;
        }

        this.setUser(abilityUser);

        if (activationMethod == ActivationMethod.SNEAK && !airSuction.selectOriginal()) {
            return ActivateResult.NOT_ACTIVATE;
        } else if (activationMethod == ActivationMethod.LEFT_CLICK) {
            airSuction.launch();
        }
        return result;
    }

    private void launch() {
        Location eyeLocation = entity.getEyeLocation();
        if (this.destination == null) {
            this.destination = eyeLocation;
            this.pushSelf = false;
        }
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        boolean ignoreLiquids = eyeLocation.getBlock().isLiquid();
        RayTrace.CompositeResult result = RayTrace.of(new Vector3d(eyeLocation), direction)
                .type(RayTrace.Type.COMPOSITE)
                .filter(e -> e instanceof LivingEntity && e != entity)
                .range(distance).ignoreLiquids(ignoreLiquids)
                .result(entity.getWorld());
        LivingEntity livingEntity = (LivingEntity) result.entity();
        if(livingEntity != null) {
            this.original = WorldUtils.getEntityCenter(livingEntity).toLocation(livingEntity.getWorld());
        } else {
            this.original = result
                    .position()
                    .subtract(direction.multiply(0.5))
                    .toLocation(eyeLocation.getWorld());
        }
        this.direction = new Vector3d(destination.clone().subtract(original)).normalize();
        this.location = original.clone();
        AbilityInformation information = getInformation();
        this.user.setCooldown(information, information.getCooldown());
    }


    private boolean selectOriginal() {
        Location eyeLocation = entity.getEyeLocation();
        Vector3d eyeVector = new Vector3d(eyeLocation);
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        boolean ignoreLiquids = eyeLocation.getBlock().isLiquid();
        this.destination = RayTrace.of(eyeVector, direction).range(selectRange).ignoreLiquids(ignoreLiquids).result(entity.getWorld()).position().subtract(direction.multiply(0.5)).toLocation(eyeLocation.getWorld());
        this.pushSelf = true;
        if (this.destination.getBlock().isLiquid() || !user.canUse(this.destination)) {
            this.destination = null;
            return false;
        }
        return true;
    }

    @Override
    public UpdateResult update() {
        if (this.destination == null) return UpdateResult.REMOVE;
        if (this.location != null) {
            Block block = location.getBlock();
            if (block.isLiquid() || !block.isPassable() || new CompositeRemovalPolicy(
                    new IsDeadRemovalPolicy(user),
                    new ProtectRemovalPolicy(user, () -> location),
                    new OutOfWorldRemovalPolicy(user),
                    new OutOfRangeRemovalPolicy(() -> this.original, () -> location, distance)
            ).shouldRemove()) {
                return UpdateResult.REMOVE;
            }
            this.collider = new Sphere(new Vector3d(location), pushRadius);
            CollisionUtils.handleEntityCollisions(entity, collider, (entity) -> {
                double pushPower = pushPowerOther;
                if (entity == this.entity) {
                    pushPower = pushPowerSelf;
                }
                entity.setVelocity(this.direction.multiply(pushPower).toBukkitVector());
                return true;
            }, false, pushSelf);
            AirElement.display(location, 4, 0.5f, 0.5f, 0.5f);
            this.location.add(this.direction.multiply(speed).toBukkitVector());
        } else {
            if (new CompositeRemovalPolicy(
                    new OutOfRangeRemovalPolicy(() -> this.destination, () -> this.entity.getLocation(), selectRange + 2),
                    new SwappedSlotsRemovalPolicy<>(user, AirSuction.class)
            ).shouldRemove()) {
                return UpdateResult.REMOVE;
            }
            AirElement.display(this.destination, 4, 0.5f, 0.5f, 0.5f);
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
        if (this.collider == null) return Collections.emptyList();
        return Collections.singleton(collider);
    }
}
