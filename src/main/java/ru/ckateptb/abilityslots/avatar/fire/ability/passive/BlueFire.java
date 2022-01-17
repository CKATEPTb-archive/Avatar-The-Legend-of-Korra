package ru.ckateptb.abilityslots.avatar.fire.ability.passive;

import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;

@AbilityInfo(
        author = "CKATEPTb",
        name = "BlueFire",
        displayName = "BlueFire",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "fire",
        description = "This passive ability allows FireBender to manipulate the blue fire.\nBlue flames have a higher temperature than normal yellow flames, and therefore higher power.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class BlueFire extends Ability {
    @Override
    public ActivateResult activate(ActivationMethod method) {
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }
}
