package ru.ckateptb.abilityslots.avatar.air.ability;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;


@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Tornado",
        displayName = "Tornado",
        activationMethods = {ActivationMethod.SNEAK},
        category = "air",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class Tornado implements Ability {
    private AbilityUser user;
    private LivingEntity entity;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        return null;
    }

    @Override
    public UpdateResult update() {
        return null;
    }

    @Override
    public void destroy() {
        this.user.setCooldown(getInformation(), getInformation().getCooldown());
    }

    @Override
    public AbilityUser getUser() {
        return this.user;
    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.entity = user.getEntity();
    }
}
