package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Meditate",
        displayName = "Meditate",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Aerial Monks derive their amplification from meditation, this ability will greatly enhance your agility and resilience.",
        instruction = "Hold Sneak for charge up then Release Sneak",
        cooldown = 12500,
        cost = 10
)
public class Meditate extends Ability {
    @ConfigField
    private static long duration = 5000;
    @ConfigField
    private static long chargeTime = 1000;
    @ConfigField
    private static int absorptionStrength = 2;
    @ConfigField
    private static int speedStrength = 2;
    @ConfigField
    private static int jumpStrength = 2;
    @ConfigField
    private static double costRegen = 100;

    private long startTime;
    private boolean charged;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        this.startTime = System.currentTimeMillis();
        this.charged = false;
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (!user.canUse(livingEntity.getLocation())) {
            return UpdateResult.REMOVE;
        }
        if (user.isSneaking()) {
            if (System.currentTimeMillis() > startTime + chargeTime) {
                charged = true;
                ImmutableVector direction = user.getDirection();
                ImmutableVector location = user.getEyeLocation().add(direction);
                ImmutableVector side = direction.crossProduct(ImmutableVector.PLUS_J).normalize();
                ImmutableVector right = location.add(side.multiply(0.5));
                ImmutableVector left = location.subtract(side.multiply(0.5));
                AirElement.display(left.toLocation(world), 1, 0.0f, 0.0f, 0.0f, false);
                AirElement.display(right.toLocation(world), 1,0.0f, 0.0f, 0.0f, false);
            }
            return UpdateResult.CONTINUE;
        } else if (!charged) return UpdateResult.REMOVE;
        else {
            if(user.removeEnergy(this)) {
                user.setCooldown(this);
                world.playSound(livingEntity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0);
                if (absorptionStrength != -1) {
                    user.removePotionEffect(this, PotionEffectType.ABSORPTION);
                    user.addPotionEffect(this, new PotionEffect(PotionEffectType.ABSORPTION, (int) (duration / 50), absorptionStrength - 1, true, false));
                }
                if (speedStrength != -1) {
                    user.removePotionEffect(this, PotionEffectType.SPEED);
                    user.addPotionEffect(this, new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedStrength - 1, true, false));
                }
                if (jumpStrength != -1) {
                    user.removePotionEffect(this, PotionEffectType.JUMP);
                    user.addPotionEffect(this, new PotionEffect(PotionEffectType.JUMP, (int) (duration / 50), jumpStrength - 1, true, false));
                }
            }
            return UpdateResult.REMOVE;
        }
    }

    @Override
    public void destroy() {
    }
}
