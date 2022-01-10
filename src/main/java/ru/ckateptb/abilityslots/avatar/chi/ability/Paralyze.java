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
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.avatar.chi.ChiElement;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.DurationRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.OutOfWorldRemovalPolicy;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.RayTrace;
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
public class Paralyze implements Ability {
    @ConfigField
    private static long duration = 5000;

    private AbilityUser user;
    private LivingEntity entity;
    private LivingEntity target;
    private TemporaryParalyze paralyze;
    private CompositeRemovalPolicy removalPolicy;

    @Override
    public ActivateResult activate(AbilityUser abilityUser, ActivationMethod activationMethod) {
        this.setUser(abilityUser);

        this.target = (LivingEntity) RayTrace.of(entity)
                .range(ChiElement.getHitActivationRange())
                .type(RayTrace.Type.ENTITY)
                .filter(e -> e instanceof LivingEntity && e != entity)
                .result(entity.getWorld()).entity();
        if (target == null) return ActivateResult.NOT_ACTIVATE;

        Location targetLocation = target.getLocation();
        if(!user.canUse(targetLocation)) return ActivateResult.NOT_ACTIVATE;

        this.paralyze = new TemporaryParalyze(target, duration);
        target.getWorld().playSound(targetLocation, Sound.ENTITY_ENDER_DRAGON_HURT, 2, 0);

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new DurationRemovalPolicy(duration),
                new OutOfWorldRemovalPolicy(user)
        );

        user.setCooldown(this);
        return ActivateResult.ACTIVATE_AND_CANCEL_EVENT;
    }

    @Override
    public UpdateResult update() {
        return removalPolicy.shouldRemove() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        SpringContext.getInstance().getBean(TemporaryService.class).revert(paralyze);
    }

    @Override
    public void setUser(AbilityUser abilityUser) {
        this.user = abilityUser;
        this.entity = abilityUser.getEntity();
    }
}
