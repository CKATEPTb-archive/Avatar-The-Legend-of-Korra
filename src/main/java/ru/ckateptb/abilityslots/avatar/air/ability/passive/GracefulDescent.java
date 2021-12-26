package ru.ckateptb.abilityslots.avatar.air.ability.passive;

import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;

@AbilityInfo(
        author = "CKATEPTb",
        name = "GracefulDescent",
        displayName = "GracefulDescent",
        activationMethods = {ActivationMethod.PASSIVE, ActivationMethod.FALL},
        category = "air",
        description = "Is a passive ability which allows AirBenders to make a gentle landing, negating all fall damage on any surface.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class GracefulDescent implements Ability {
    private AbilityUser abilityUser;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        if (activationMethod == ActivationMethod.FALL && getAbilityInstanceService().hasAbility(abilityUser, this.getClass())) {
            return ActivateResult.NOT_ACTIVATE_AND_CANCEL_EVENT;
        }
        this.abilityUser = abilityUser;
        return activationMethod == ActivationMethod.PASSIVE ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public AbilityUser getUser() {
        return this.abilityUser;
    }

    @Override
    public void setUser(AbilityUser abilityUser) {
        this.abilityUser = abilityUser;
    }
}
