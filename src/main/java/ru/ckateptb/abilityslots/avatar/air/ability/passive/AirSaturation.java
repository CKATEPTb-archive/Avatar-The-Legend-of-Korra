package ru.ckateptb.abilityslots.avatar.air.ability.passive;

import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.abilityslots.user.PlayerAbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;

@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSaturation",
        displayName = "AirSaturation",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "air",
        description = "Is a passive ability which causes AirBenders hunger to deplete at a slower rate.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class AirSaturation implements Ability {
    @ConfigField(name = "exhaustionModifier", comment = "Hunger multiplier that the user will receive")
    private static double exhaustionModifier = 0.3;
    private AbilityUser abilityUser;
    private Float exhaustionLevel = null;
    private Player player;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        this.setUser(abilityUser);
        return activationMethod == ActivationMethod.PASSIVE ? ActivateResult.ACTIVATE : ActivateResult.NOT_ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (player != null && exhaustionModifier > 0) {
            float exhaustionLevel = player.getExhaustion();
            if(this.exhaustionLevel == null) {
                this.exhaustionLevel = exhaustionLevel;
            } else {
                if (exhaustionLevel < this.exhaustionLevel) {
                    this.exhaustionLevel = 0f;
                } else {
                    this.exhaustionLevel = (float) ((exhaustionLevel - this.exhaustionLevel) * exhaustionModifier + this.exhaustionLevel);
                }
                player.setExhaustion(this.exhaustionLevel);
            }
        }
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
        if (abilityUser instanceof PlayerAbilityUser playerAbilityUser) {
            player = playerAbilityUser.getEntity();
        }
    }
}
