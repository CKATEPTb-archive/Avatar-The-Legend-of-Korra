package ru.ckateptb.abilityslots.avatar.earth.ability.subcategories;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;

@AbilityInfo(
        author = "CKATEPTb",
        name = "LavaBender",
        displayName = "LavaBender",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "earth",
        description = "This passive ability allows EarthBender to manipulate the lava.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class LavaBender extends Ability {
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
