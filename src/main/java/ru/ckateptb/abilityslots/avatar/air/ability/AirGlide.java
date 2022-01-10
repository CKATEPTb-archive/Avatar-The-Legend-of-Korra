package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.removalpolicy.*;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirGlide",
        displayName = "AirGlide",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Replaces falling with a slow and steady descent",
        instruction = "Hold Sneak at falling time",
        cooldown = 4000
)
public class AirGlide implements Ability {
    @ConfigField
    private static long duration = 10000;
    @ConfigField
    private static double speed = 0.5;
    @ConfigField
    private static double fallSpeed = 0.1;
    @ConfigField
    private static int particles = 4;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private CompositeRemovalPolicy removalPolicy;
    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);
        this.removalPolicy = new CompositeRemovalPolicy(
                new SneakingRemovalPolicy(user, true),
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user),
                new SwappedSlotsRemovalPolicy<>(user, AirGlide.class)
        );
        if (duration > 0) {
            this.removalPolicy.addPolicy(new DurationRemovalPolicy(duration));
        }
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if(removalPolicy.shouldRemove()) return UpdateResult.REMOVE;
        Location location = livingEntity.getLocation();
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        if (!livingEntity.isOnGround()) {
            Location firstLocation = livingEntity.getEyeLocation();
            Vector3d directionVector = new Vector3d(firstLocation.getDirection()).normalize();
            directionVector = directionVector.setY(0).normalize(Vector3d.PLUS_I);
            double distanceFromPlayer = speed;
            Vector3d shootFromPlayer = new Vector3d(directionVector.getX() * distanceFromPlayer, -fallSpeed, directionVector.getZ() * distanceFromPlayer);
            livingEntity.setVelocity(shootFromPlayer.toBukkitVector());
            AirElement.display(location, particles, (float) Math.random(), (float) Math.random(), (float) Math.random());
        }
        if (block.isLiquid() || !block.isPassable()) {
            return UpdateResult.REMOVE;
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
