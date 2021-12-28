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
import ru.ckateptb.abilityslots.particle.ParticleEffect;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.OutOfRangeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.ProtectRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.AABB;
import ru.ckateptb.tablecloth.collision.collider.Disc;
import ru.ckateptb.tablecloth.collision.collider.OBB;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

@Getter
@Setter
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
    private static double speed = 1;
    @ConfigField
    private static int angleStep = 10;

    private AbilityUser user;
    private LivingEntity entity;
    private Location location;
    private Location origin;
    private Vector3d direction;
    private Disc collider;
    private final CompositeRemovalPolicy removalPolicy = new CompositeRemovalPolicy();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod activationMethod) {
        AbilityInformation information = getInformation();
        if (user.hasCooldown(information)) return ActivateResult.NOT_ACTIVATE;
        this.setUser(user);
        this.location = entity.getEyeLocation();
        this.origin = location.clone();
        this.removalPolicy.addPolicy(
                new IsDeadRemovalPolicy(user),
                new OutOfRangeRemovalPolicy(() -> this.origin, () -> this.location, range),
                new ProtectRemovalPolicy(user, () -> this.location)
        );
        this.direction = new Vector3d(this.location.getDirection()).normalize();
        AABB bounds = new AABB(new Vector3d(-0.15, -radius, -radius), new Vector3d(0.15, radius, radius));
        double angle = Math.toRadians(this.entity.getLocation().getYaw());
        this.collider = new Disc(new OBB(bounds, Vector3d.PLUS_J, angle), new Sphere(radius));
        user.setCooldown(information, information.getCooldown());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        Location location = this.location.add(direction.multiply(speed).toBukkitVector());
        if (this.removalPolicy.shouldRemove()) return UpdateResult.REMOVE;

        Vector3d rotationAxis = Vector3d.PLUS_J.cross(this.direction);

        for (double angle = 0; angle < 360; angle += angleStep) {
            Vector3d offset = direction.rotate(rotationAxis, Math.toRadians(angle)).multiply(radius);
            ParticleEffect.SPELL.display(location.clone().add(offset.toBukkitVector()), 1, 0, 0, 0);
        }

        Disc checkCollider = this.collider.addPosition(location);

        boolean hit = CollisionUtils.handleEntityCollisions(this.entity, checkCollider, (entity) -> {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).damage(damage, this.entity);
                return true;
            }
            return false;
        }, true) || CollisionUtils.handleBlockCollisions(this.entity, checkCollider, o -> false, block -> block.getType().isSolid(), true).size() > 0;

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
