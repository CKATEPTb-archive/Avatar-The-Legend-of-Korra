package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.AbilityCollisionResult;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.DestroyAbilities;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.particlestream.ParticleStream;
import ru.ckateptb.abilityslots.common.util.VectorUtils;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
@DestroyAbilities(destroyAbilities = {
        AirShield.class
})
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
    private static double step = 0.1;

    private AbilityUser user;
    private LivingEntity entity;
    private final CompositeRemovalPolicy removalPolicy = new CompositeRemovalPolicy();
    private final List<Entity> affected = new ArrayList<>();
    private final List<BladeStream> streams = new ArrayList<>();

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod activationMethod) {
        this.setUser(user);
        this.removalPolicy.addPolicy(
                new IsDeadRemovalPolicy(user)
        );
        Location eyeLocation = entity.getEyeLocation().add(0, -radius, 0);
        Vector3d direction = new Vector3d(eyeLocation.getDirection());
        Vector3d rotateAxis = Vector3d.PLUS_J.cross(direction);
        for (double d = -radius; d < radius; d += step) {
            double finalD = d;
            VectorUtils.rotate(direction, rotateAxis, 360, 1).forEach(vector3d -> {
                Location location = eyeLocation.clone().add(vector3d.normalize().multiply(finalD).toBukkitVector());
                streams.add(new BladeStream(user, location, direction, range, speed, 0.5, 0.5, damage));
            });
        }
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

    @Override
    public Collection<Collider> getColliders() {
        return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
    }

    @Override
    public AbilityCollisionResult destroyCollider(Ability destroyer, Collider destroyerCollider, Collider destroyedCollider) {
        streams.removeIf(bladeStream -> bladeStream.getCollider().equals(destroyedCollider));
        return streams.isEmpty() ? AbilityCollisionResult.DESTROY_INSTANCE : AbilityCollisionResult.NONE;
    }

    private class BladeStream extends ParticleStream {
        public BladeStream(AbilityUser user, Location origin, Vector3d direction, double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double damage) {
            super(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, 0.5, damage);
        }

        @Override
        public void render() {
            AirElement.display(location, 1, 0, 0, 0);
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
