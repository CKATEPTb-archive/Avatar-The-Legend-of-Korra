package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirGlide",
        displayName = "AirGlide",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Replaces falling with a slow and steady descent",
        instruction = "Hold Sneak at falling time",
        cooldown = 4000,
        cost = 10
)
public class AirGlide extends Ability {
    @ConfigField
    private static long duration = 10000;
    @ConfigField
    private static double speed = 0.5;
    @ConfigField
    private static double fallSpeed = 0.1;
    @ConfigField
    private static int particles = 4;
    @ConfigField
    private static long energyCostInterval = 1000;

    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .slot()
                .sneaking(true)
                .duration(duration)
                .costInterval(energyCostInterval)
                .build();
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        Location location = livingEntity.getLocation();
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        if (!user.isOnGround()) {
            ImmutableVector direction = user.getDirection();
            direction = direction.setY(0).normalize();
            double distanceFromPlayer = speed;
            ImmutableVector shootFromPlayer = new ImmutableVector(direction.getX() * distanceFromPlayer, -fallSpeed, direction.getZ() * distanceFromPlayer);
            AbilityTarget.of(livingEntity).setVelocity(shootFromPlayer, this);
            AirElement.display(location, particles, (float) Math.random(), (float) Math.random(), (float) Math.random());
        }
        return block.isLiquid() || !block.isPassable() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        this.user.setCooldown(this);
    }
}
