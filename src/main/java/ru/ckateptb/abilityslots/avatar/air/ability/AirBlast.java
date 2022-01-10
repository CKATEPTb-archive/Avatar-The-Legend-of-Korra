package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.general.AirFlow;
import ru.ckateptb.abilityslots.common.burst.BurstableAbility;
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
        description = "A strong but harmless stream of air that flies from point B to point A, capturing everything in its path. If item A is not specified, it will be specified automatically",
        instruction = "Tap Sneak to indicate the source (point A, optional). Left Click",
        cooldown = 1250
)
@CollisionParticipant(destroyAbilities = {
        AirSuction.class,
        AirSwipe.class,
        AirBlast.class
})
public class AirBlast implements BurstableAbility, AirFlow {
    @ConfigField
    @Getter
    private static double selectRange = 8;
    @ConfigField
    @Getter
    private static double distance = 20;
    @ConfigField
    private static double speed = 1.2;
    @ConfigField
    private static double pushRadius = 1.5;
    @ConfigField
    private static double pushPowerSelf = 2.1;
    @ConfigField
    private static double pushPowerOther = 2.1;

    private AbilityUser user;
    private LivingEntity livingEntity;
    @Setter
    private Location original;
    private Location location;
    private Vector3d direction;
    @Setter
    private boolean pushSelf;
    private boolean burst;
    private Collider collider;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        return getActivationResult(abilityUser, activationMethod, getAbilityInstanceService().getAbilityUserInstances(abilityUser, getClass()).stream().filter(ability -> ability.getLocation() == null).findFirst());
    }

    public void launch() {
        Location eyeLocation = livingEntity.getEyeLocation();
        if (this.original == null) {
            this.original = eyeLocation;
            this.pushSelf = false;
        }
        this.direction = new Vector3d(eyeLocation.getDirection()).normalize();
        this.location = this.original.clone();
        AbilityInformation information = getInformation();
        this.user.setCooldown(information, information.getCooldown());
    }

    public boolean selectOriginal() {
        return selectOriginal(this);
    }

    public static boolean selectOriginal(AirFlow flow) {
        AbilityUser user = flow.getUser();
        LivingEntity entity = user.getEntity();
        Location eyeLocation = entity.getEyeLocation();
        Vector3d eyeVector = new Vector3d(eyeLocation);
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        boolean ignoreLiquids = eyeLocation.getBlock().isLiquid();
        Location original = RayTrace.of(eyeVector, direction).range(selectRange).ignoreLiquids(ignoreLiquids).result(entity.getWorld()).position().subtract(direction.multiply(0.5)).toLocation(eyeLocation.getWorld());
        flow.setOriginal(original);
        flow.setPushSelf(true);
        if (original.getBlock().isLiquid() || !user.canUse(original)) {
            flow.setOriginal(null);
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
            AirElement.handleBlockInteractions(user, block);
            if (block.isLiquid() || !block.isPassable() || new CompositeRemovalPolicy(
                    new IsDeadRemovalPolicy(user),
                    new ProtectRemovalPolicy(user, () -> location),
                    new OutOfWorldRemovalPolicy(user),
                    new OutOfRangeRemovalPolicy(() -> this.original, () -> location, distance)
            ).shouldRemove()) {
                return UpdateResult.REMOVE;
            }
            this.collider = new Sphere(new Vector3d(location), pushRadius);
            CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                double pushPower = pushPowerOther;
                if (entity == this.livingEntity) {
                    pushPower = pushPowerSelf;
                }
                entity.setVelocity(this.direction.multiply(pushPower).toBukkitVector());
                entity.setFireTicks(0);
                return true;
            }, false, pushSelf);
            if (!burst || ThreadLocalRandom.current().nextInt(10) == 0) {
                AirElement.display(location, burst ? 1 : 4, 0.5f, 0.5f, 0.5f);
            }
        } else {
            if (new CompositeRemovalPolicy(
                    new OutOfRangeRemovalPolicy(() -> this.original, () -> this.livingEntity.getLocation(), selectRange + 2),
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
        this.livingEntity = abilityUser.getEntity();
    }

    @Override
    public Collection<Collider> getColliders() {
        if (this.collider == null) return Collections.emptyList();
        return Collections.singleton(collider);
    }

    @Override
    // Used to initialize the blast for bursts.
    public void initialize(AbilityUser user, Location location, Vector3d direction) {
        this.initialize(user, location, direction, false, true);
    }

    public void initialize(AbilityUser user, Location location, Vector3d direction, boolean pushSelf) {
        this.initialize(user, location, direction, pushSelf, false);
    }

    public void initialize(AbilityUser user, Location location, Vector3d direction, boolean pushSelf, boolean burst) {
        this.setUser(user);
        this.direction = direction;
        this.original = location.clone();
        this.location = location.clone();
        this.pushSelf = pushSelf;
        this.burst = burst;
    }
}
