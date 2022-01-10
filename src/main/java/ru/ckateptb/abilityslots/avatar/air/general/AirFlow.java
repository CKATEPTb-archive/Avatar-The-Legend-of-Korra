package ru.ckateptb.abilityslots.avatar.air.general;

import org.bukkit.Location;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.user.AbilityUser;

import java.util.Optional;

public interface AirFlow {
    void setOriginal(Location location);

    Location getOriginal();

    void setPushSelf(boolean pushSelf);

    AbilityUser getUser();

    void setUser(AbilityUser user);

    boolean selectOriginal();

    void launch();

    default ActivateResult getActivationResult(AbilityUser abilityUser, ActivationMethod activationMethod, Optional<? extends AirFlow> optional) {
        ActivateResult result = ActivateResult.ACTIVATE;
        AirFlow flow = this;
        if (optional.isPresent()) {
            flow = optional.get();
            result = ActivateResult.NOT_ACTIVATE;
        }

        flow.setUser(abilityUser);

        if (activationMethod == ActivationMethod.SNEAK && !flow.selectOriginal()) {
            return ActivateResult.NOT_ACTIVATE;
        } else if (activationMethod == ActivationMethod.LEFT_CLICK) {
            flow.launch();
        }
        return result;
    }
}
