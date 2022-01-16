package ru.ckateptb.abilityslots.avatar.chi.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.chi.ChiElement;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.spring.SpringContext;
import ru.ckateptb.tablecloth.temporary.TemporaryService;
import ru.ckateptb.tablecloth.temporary.paralyze.TemporaryParalyze;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "Paralyze",
        displayName = "Paralyze",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "chi",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class Paralyze extends Ability {
    @ConfigField
    private static long duration = 5000;

    private LivingEntity target;
    private TemporaryParalyze paralyze;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod activationMethod) {
        this.target = user.findLivingEntity(ChiElement.getHitActivationRange());
        if (target == null) return ActivateResult.NOT_ACTIVATE;

        Location targetLocation = target.getLocation();
        if (!user.canUse(targetLocation)) return ActivateResult.NOT_ACTIVATE;

        this.paralyze = new TemporaryParalyze(target, duration);
        target.getWorld().playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_HURT, 2, 0);

        this.removal = new RemovalConditional.Builder().offline().dead().world().duration(duration).build();

        user.setCooldown(this);
        return ActivateResult.ACTIVATE_AND_CANCEL_EVENT;
    }

    @Override
    public UpdateResult update() {
        return removal.shouldRemove(user, this) ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        SpringContext.getInstance().getBean(TemporaryService.class).revert(paralyze);
    }
}
