package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.info.CollisionParticipant;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.common.particlestream.ParticleStream;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.Collider;
import ru.ckateptb.tablecloth.collision.collider.Sphere;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.util.CollisionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "SonicBlast",
        displayName = "SonicBlast",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Compresses air, making a sound so loud that targets affected by it lose focus and take damage",
        instruction = "Hold Sneak for charge up then Release Sneak",
        cooldown = 3500
)
@CollisionParticipant
public class SonicBlast implements Ability {
    @ConfigField
    private static double damage = 4;
    @ConfigField
    private static double range = 20;
    @ConfigField
    private static double radius = 2;
    @ConfigField
    private static double speed = 1;
    @ConfigField
    private static long chargeTime = 500;
    @ConfigField
    private static long duration = 2000;
    @ConfigField
    private static int power = 2;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private boolean charged;
    private Vector3d location;
    private Vector3d origin;
    private Vector3d direction;
    private Collider collider;
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.startTime = System.currentTimeMillis();
        this.charged = false;
        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new SwappedSlotsRemovalPolicy<>(user, getClass()),
                new OutOfWorldRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user)
        );
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        World world = livingEntity.getWorld();
        if (livingEntity instanceof Player player && player.isSneaking()) {
            if (System.currentTimeMillis() > startTime + chargeTime) {
                charged = true;
                Location eyeLocation = livingEntity.getEyeLocation();
                Vector3d direction = new Vector3d(eyeLocation.getDirection());
                Vector3d location = new Vector3d(eyeLocation).add(direction);
                Vector3d side = direction.cross(Vector3d.PLUS_J).normalize(Vector3d.PLUS_I);
                Vector3d l1 = location.add(side.multiply(0.5));
                Vector3d l2 = location.subtract(side.multiply(0.5));
                AirElement.display(l1.toLocation(world), 1, 0.0f, 0.0f, 0.0f, 0.0f);
                AirElement.display(l2.toLocation(world), 1, 0.0f, 0.0f, 0.0f, 0.0f);
            }
            return UpdateResult.CONTINUE;
        } else if (!charged) {
            return UpdateResult.REMOVE;
        } else {
            if (direction == null) {
                Location eyeLocation = livingEntity.getEyeLocation();
                direction = new Vector3d(eyeLocation.getDirection()).normalize();
                location = new Vector3d(eyeLocation).add(direction.multiply(speed));
                origin = location;
                world.playSound(location.toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 1, 0);
                AbilityInformation information = getInformation();
                user.setCooldown(information, information.getCooldown());
                removalPolicy.removePolicyType(SwappedSlotsRemovalPolicy.class);
            }
            location = location.add(direction.multiply(speed));

            if (location.distance(origin) > range) {
                return UpdateResult.REMOVE;
            }

            if (!user.canUse(location.toLocation(world))) {
                return UpdateResult.REMOVE;
            }

            if (location.toBlock(world).isLiquid()) {
                return UpdateResult.REMOVE;
            }

            AirElement.display(location.toLocation(world), 1, (float) radius / 2, (float) radius / 2, (float) radius / 2);
            this.collider = new Sphere(location, radius);
            boolean hit = CollisionUtils.handleEntityCollisions(livingEntity, collider, (entity) -> {
                if (entity instanceof LivingEntity target) {
                    target.damage(damage, livingEntity);
                    target.removePotionEffect(PotionEffectType.CONFUSION);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, (int) (duration / 50), power - 1, true, false));
                    target.removePotionEffect(PotionEffectType.BLINDNESS);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (duration / 50), power - 1, true, false));
                    return true;
                }
                return false;
            }, true, false) || CollisionUtils.handleBlockCollisions(livingEntity, new Sphere(location, radius / 2), o -> false, block -> !block.isPassable() || block.isLiquid(), false).size() > 0;
            return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }


    @Override
    public Collection<Collider> getColliders() {
        if(this.collider == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(this.collider);
    }
}
