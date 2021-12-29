package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.DestroyAbilities;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.util.VectorUtils;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.OutOfRangeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.ProtectRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.*;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBlade",
        displayName = "AirBlade",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class AirBlade implements Ability {
    @ConfigField
    private static double damage = 4;
    @ConfigField
    private static double range = 20;
    @ConfigField
    private static double radius = 1;
    @ConfigField
    private static double blockCollisionMultiply = 0.5;
    @ConfigField
    private static double speed = 1;
    @ConfigField
    private static int angleStep = 9;

    private AbilityUser user;
    private LivingEntity entity;
    private Location location;
    private Location origin;
    private Vector3d direction;
    private Disc entityCollider;
    private Disc blockCollider;
    private final CompositeRemovalPolicy removalPolicy = new CompositeRemovalPolicy();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod activationMethod) {
        this.setUser(user);
        this.location = entity.getEyeLocation();
        this.origin = location.clone();
        this.removalPolicy.addPolicy(
                new IsDeadRemovalPolicy(user),
                new OutOfRangeRemovalPolicy(() -> this.origin, () -> this.location, range),
                new ProtectRemovalPolicy(user, () -> this.location)
        );
        this.direction = new Vector3d(this.location.getDirection()).normalize();
        double radius = AirBlade.radius * 1.5;
        double angle = Math.toRadians(this.entity.getLocation().getYaw());

        AABB entityBounds = new AABB(new Vector3d(-0.15, -radius, -radius), new Vector3d(0.15, radius, radius));
        OBB entityObb = new OBB(entityBounds, Vector3d.PLUS_J, angle);
        this.entityCollider = new Disc(entityObb, new Sphere(radius));

        radius = AirBlade.radius;

        AABB blockBounds = new AABB(new Vector3d(-0.15, -radius, -radius), new Vector3d(0.15, radius, radius));
        OBB blockObb = new OBB(blockBounds, Vector3d.PLUS_J, angle);
        this.blockCollider = new Disc(blockObb, new Sphere(radius));

        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        Location location = this.location.add(direction.multiply(speed).toBukkitVector());
        if (this.removalPolicy.shouldRemove()) return UpdateResult.REMOVE;


        Vector3d locationVector = new Vector3d(location);

        Vector3d rotationAxis = Vector3d.PLUS_J.cross(this.direction);
        VectorUtils.circle(new Ray(locationVector, direction).direction.multiply(radius), rotationAxis, 360 / angleStep).forEach(v ->
                AirElement.display(location.clone().add(v.toBukkitVector()), 1, 0, 0, 0)
        );

        boolean hit = CollisionUtils.handleEntityCollisions(this.entity, entityCollider.at(locationVector), (entity) -> {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).damage(damage, this.entity);
                return true;
            }
            return false;
        }, true) || CollisionUtils.handleBlockCollisions(this.entity, blockCollider.at(locationVector), o -> false, block -> block.getType().isSolid(), true).size() > 0;

        return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    public void setUser(AbilityUser user) {
        this.user = user;
        this.entity = user.getEntity();
    }

    @Override
    public void destroy() {
    }
}
