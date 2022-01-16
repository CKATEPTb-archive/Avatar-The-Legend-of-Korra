package ru.ckateptb.abilityslots.avatar.air.ability.passive;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
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
public class AirAgility extends Ability {
    @ConfigField(name = "requireSprint")
    private static boolean requireSprint = true;
    @ConfigField(name = "speedAmplifier")
    private static int speedAmplifier = 2;
    @ConfigField(name = "jumpAmplifier")
    private static int jumpAmplifier = 2;

    private long previousHandleTime = System.currentTimeMillis();

    @Override
    public ActivateResult activate(ActivationMethod activationMethod) {
        return activationMethod == ActivationMethod.PASSIVE ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (!user.isDead() && (!requireSprint || user.isSprinting()) && System.currentTimeMillis() > previousHandleTime) {
            this.handlePotionEffect(PotionEffectType.SPEED, speedAmplifier);
            this.handlePotionEffect(PotionEffectType.JUMP, jumpAmplifier);
            previousHandleTime = System.currentTimeMillis() + 1000;
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        user.removePotionEffect(this, PotionEffectType.JUMP);
        user.removePotionEffect(this, PotionEffectType.SPEED);
    }

    private void handlePotionEffect(PotionEffectType type, int amplifier) {
        PotionEffect effect = user.getPotionEffect(this, type);
        if (effect == null || effect.getDuration() < 20 || effect.getAmplifier() < amplifier) {
            user.addPotionEffect(this, new PotionEffect(type, 100, amplifier - 1, true, false));
        }
    }
}
