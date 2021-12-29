package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlade;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.chi.ability.Paralyze;
import ru.ckateptb.abilityslots.user.AbilityUser;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSweep",
        displayName = "AirSweep",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
@Sequence({
        @AbilityAction(ability = AirBlade.class, action = SequenceAction.RIGHT_CLICK_BLOCK),
})
public class AirSweep implements Ability {
    private AbilityUser user;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        setUser(abilityUser);
        this.user.setCooldown(getInformation(), getInformation().getCooldown());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.REMOVE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser abilityUser) {
        this.user = abilityUser;
    }
}
