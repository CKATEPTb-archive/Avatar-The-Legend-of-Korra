package ru.ckateptb.abilityslots.avatar.air.ability;

import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;

@AbilityInfo(
        author = "CKATEPTb",
        name = "AirBlast",
        displayName = "AirBlast",
        activationMethods = {ActivationMethod.SNEAK, ActivationMethod.LEFT_CLICK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction"
)
public class AirBlast implements Ability {
    @ConfigField(comment = "ExampleComment")
    private static double range = 20;
    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        return ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return null;
    }

    @Override
    public void destroy() {

    }

    @Override
    public AbilityUser getUser() {
        return null;
    }

    @Override
    public void setUser(AbilityUser abilityUser) {

    }
}
