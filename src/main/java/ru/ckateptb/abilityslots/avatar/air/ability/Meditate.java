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
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.math.Vector3d;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Meditate",
        displayName = "Meditate",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Aerial Monks derive their amplification from meditation, this ability will greatly enhance your agility and resilience.",
        instruction = "Hold Sneak for charge up then Release Sneak",
        cooldown = 12500
)
public class Meditate implements Ability {
    private static long duration = 5000;
    private static long chargeTime = 1000;
    private static int absorptionStrength = 2;
    private static int speedStrength = 2;
    private static int jumpStrength = 2;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private long startTime;
    private boolean charged;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        this.startTime = System.currentTimeMillis();
        this.charged = false;
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (!user.canUse(livingEntity.getLocation())) {
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
                AirElement.display(l1.toLocation(world), 1, 0.0f, 0.0f, 0.0f);
                AirElement.display(l2.toLocation(world), 1, 0.0f, 0.0f, 0.0f, 0.0f);
            }
            return UpdateResult.CONTINUE;
        } else if (!charged) return UpdateResult.REMOVE;
        else {
            world.playSound(livingEntity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 0);
            if (absorptionStrength != -1) {
                livingEntity.removePotionEffect(PotionEffectType.ABSORPTION);
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, (int) (duration / 50), absorptionStrength - 1, true, false));
            }
            if (speedStrength != -1) {
                livingEntity.removePotionEffect(PotionEffectType.SPEED);
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (duration / 50), speedStrength - 1, true, false));
            }
            if (jumpStrength != -1) {
                livingEntity.removePotionEffect(PotionEffectType.JUMP);
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, (int) (duration / 50), jumpStrength - 1, true, false));
            }
            return UpdateResult.REMOVE;
        }
    }

    @Override
    public void destroy() {
        user.setCooldown(getInformation(), getInformation().getCooldown());
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
