package ru.ckateptb.abilityslots.avatar.air.ability.passive;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;

@AbilityInfo(
        author = "CKATEPTb",
        name = "AirAgility",
        displayName = "AirAgility",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "air",
        description = "Is a passive ability which enables AirBenders to run faster and jump higher.",
        instruction = "Start running to get power-ups",
        canBindToSlot = false
)
public class AirAgility implements Ability {
    @ConfigField(name = "requireSprint")
    private static boolean requireSprint = true;
    @ConfigField(name = "speedAmplifier")
    private static int speedAmplifier = 2;
    @ConfigField(name = "jumpAmplifier")
    private static int jumpAmplifier = 2;
    private AbilityUser user;
    private LivingEntity entity;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        this.setUser(abilityUser);
        return activationMethod == ActivationMethod.PASSIVE ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if(!entity.isDead()) {
            if(entity instanceof Player player && requireSprint && !player.isSprinting()) return UpdateResult.CONTINUE;
            this.handlePotionEffect(PotionEffectType.SPEED, speedAmplifier);
            this.handlePotionEffect(PotionEffectType.JUMP, jumpAmplifier);
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        entity.removePotionEffect(PotionEffectType.JUMP);
        entity.removePotionEffect(PotionEffectType.SPEED);
    }

    private void handlePotionEffect(PotionEffectType type, int amplifier) {
        PotionEffect effect = entity.getPotionEffect(type);
        if (effect == null || effect.getDuration() < 20 || effect.getAmplifier() < amplifier) {
            entity.addPotionEffect(new PotionEffect(type, 100, amplifier - 1, true, false));
        }
    }

    @Override
    public AbilityUser getUser() {
        return user;
    }

    @Override
    public void setUser(AbilityUser abilityUser) {
        this.user = abilityUser;
        this.entity = abilityUser.getEntity();
    }
}
