package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.DestroyAbilities;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.BurstableAbility;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBlast",
        displayName = "AirBlast",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 1250
)
@DestroyAbilities(destroyAbilities = {
        AirBlast.class
})
public class AirBlast implements BurstableAbility {
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
    private Location original;
    private Location location;
    private Vector3d direction;
    private boolean pushSelf;
    private boolean burst;
    private Collider collider;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        ActivateResult result = ActivateResult.ACTIVATE;
        Optional<? extends AirBlast> optional = getAbilityInstanceService().getAbilityUserInstances(abilityUser, getClass()).stream()
                .filter(ability -> ability.getLocation() == null).findFirst();
        AirBlast airBlast = this;
        if (optional.isPresent()) {
            airBlast = optional.get();
            result = ActivateResult.NOT_ACTIVATE;
        }

        this.setUser(abilityUser);

        if (activationMethod == ActivationMethod.SNEAK && !airBlast.selectOriginal()) {
            return ActivateResult.NOT_ACTIVATE;
        } else if (activationMethod == ActivationMethod.LEFT_CLICK) {
            airBlast.launch();
        }
        return result;
    }

    private void launch() {
        Location eyeLocation = entity.getEyeLocation();
        if (this.original == null) {
            this.original = eyeLocation;
            this.pushSelf = false;
        }
        this.direction = new Vector3d(eyeLocation.getDirection()).normalize();
        this.location = this.original.clone();
        AbilityInformation information = getInformation();
        this.user.setCooldown(information, information.getCooldown());
    }

    private boolean selectOriginal() {
        Location eyeLocation = entity.getEyeLocation();
        Vector3d eyeVector = new Vector3d(eyeLocation);
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        boolean ignoreLiquids = eyeLocation.getBlock().isLiquid();
        this.original = RayTrace.of(eyeVector, direction).range(selectRange).ignoreLiquids(ignoreLiquids).result(entity.getWorld()).position().subtract(direction.multiply(0.5)).toLocation(eyeLocation.getWorld());
        this.pushSelf = true;
        if (this.original.getBlock().isLiquid() || !user.canUse(this.original)) {
            this.original = null;
            return false;
        }
        return true;
    }

    @Override
    public UpdateResult update() {
        if (this.original == null) return UpdateResult.REMOVE;
        if (this.location != null) {
            this.location.add(this.direction.multiply(speed).toBukkitVector());
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
            if(!burst || ThreadLocalRandom.current().nextInt(10) == 0) {
                AirElement.display(location, burst ? 1 : 4, 0.5f, 0.5f, 0.5f);
            }
        } else {
            if (new CompositeRemovalPolicy(
                    new OutOfRangeRemovalPolicy(() -> this.original, () -> this.entity.getLocation(), selectRange + 2),
                    new SwappedSlotsRemovalPolicy<>(user, AirBlast.class)
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
        if (this.collider == null) return Collections.emptyList();
        return Collections.singleton(collider);
    }

    @Override
    // Used to initialize the blast for bursts.
    public void initialize(AbilityUser user, Location location, Vector3d direction) {
        this.setUser(user);
        this.direction = direction;
        this.original = location.clone();
        this.location = location.clone();
        this.pushSelf = false;
        this.burst = true;
    }
}
