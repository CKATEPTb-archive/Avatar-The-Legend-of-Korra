package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.general.ParticleStream;
import ru.ckateptb.abilityslots.avatar.util.VectorUtils;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.collider.Ray;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;

import java.util.ArrayList;
import java.util.List;
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
    private static double radius = 1.5;
    @ConfigField
    private static double speed = 2;
    @ConfigField
    private static int angleStep = 30;

    private AbilityUser user;
    private LivingEntity entity;
    private final CompositeRemovalPolicy removalPolicy = new CompositeRemovalPolicy();
    private final List<Entity> affected = new ArrayList<>();
    private final List<BladeStream> streams = new ArrayList<>();
    private double size;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod activationMethod) {
        this.setUser(user);
        this.removalPolicy.addPolicy(
                new IsDeadRemovalPolicy(user)
        );
        Location eyeLocation = entity.getEyeLocation().add(0, -radius, 0);
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        for (int i = 0; i <= radius + radius - 1; ++i) {
            streams.add(new BladeStream(user, eyeLocation.clone().add(0, i, 0), direction, range, speed, 0.5, 0.5, damage));
        }
        size = Math.max(streams.size(), 1);
        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());

        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (this.removalPolicy.shouldRemove()) return UpdateResult.REMOVE;
        streams.removeIf(blade -> !blade.update());
        return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    public void setUser(AbilityUser user) {
        this.user = user;
        this.entity = user.getEntity();
    }

    @Override
    public void destroy() {
    }

    private class BladeStream extends ParticleStream {
        public BladeStream(AbilityUser user, Location origin, Vector3d direction, double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double damage) {
            super(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, 0.5, damage);
        }

        @Override
        public void render() {
            Vector3d rotationAxis = Vector3d.PLUS_J.cross(direction);
            VectorUtils.circle(new Ray(new Vector3d(location), direction).direction.multiply(radius/size), rotationAxis, 360 / angleStep).forEach(v ->
                    AirElement.display(location.clone().add(v.toBukkitVector()), 1, 0, 0, 0)
            );
        }

        @Override
        public boolean onEntityHit(Entity entity) {
            if (!user.canUse(entity.getLocation())) {
                return false;
            }

            if (affected.contains(entity)) {
                return true;
            }

            ((LivingEntity) entity).damage(damage, user.getEntity());

            affected.add(entity);

            return true;
        }
    }
}
