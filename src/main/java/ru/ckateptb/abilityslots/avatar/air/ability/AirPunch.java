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
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.particlestream.ParticleStream;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsOfflineRemovalPolicy;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
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
        name = "AirPunch",
        displayName = "AirPunch",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "High density air currents to deal minor damage. Multiple hits can be made before the ability runs out of cooldown.",
        instruction = "Left Click",
        cooldown = 3500
)
@CollisionParticipant
public class AirPunch implements Ability {
    @ConfigField
    private static long threshold = 2500;
    @ConfigField
    private static int shots = 3;
    @ConfigField
    private static double range = 30;
    @ConfigField
    private static double speed = 1.5;
    @ConfigField
    private static double damage = 2;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private final List<PunchStream> streams = new ArrayList<>();

    private int currentShots;
    private long lastShotTime;
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        for (AirPunch punch : abilityInstanceService.getAbilityUserInstances(user, getClass())) {
            punch.createShot();
            return ActivateResult.NOT_ACTIVATE;
        }

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user)
        );
        this.currentShots = shots;
        this.createShot();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (this.removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        streams.removeIf(punch -> !punch.update());
        return streams.isEmpty() && (currentShots == 0 || System.currentTimeMillis() > lastShotTime + threshold) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }

    private void createShot() {
        if (currentShots-- > 0) {
            lastShotTime = System.currentTimeMillis();
            Location eyeLocation = livingEntity.getEyeLocation();
            streams.add(new PunchStream(user, eyeLocation, new Vector3d(eyeLocation.getDirection()), range, speed, 0.5, 0.5, damage));
        }
    }


    @Override
    public Collection<Collider> getColliders() {
        return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
    }

    private class PunchStream extends ParticleStream {
        public PunchStream(AbilityUser user, Location origin, Vector3d direction, double range, double speed, double entityCollisionRadius, double abilityCollisionRadius, double damage) {
            super(user, origin, direction, range, speed, entityCollisionRadius, abilityCollisionRadius, damage, false);
        }

        @Override
        public void render() {
            AirElement.display(location, 2, (float) Math.random() / 5, (float) Math.random() / 5, (float) Math.random() / 5);
        }

        @Override
        public boolean onEntityHit(Entity entity) {
            if (user.canUse(entity.getLocation()) && entity instanceof LivingEntity target) {
                target.setNoDamageTicks(0);
                target.damage(damage, livingEntity);
                return true;
            }
            return false;
        }
    }
}
