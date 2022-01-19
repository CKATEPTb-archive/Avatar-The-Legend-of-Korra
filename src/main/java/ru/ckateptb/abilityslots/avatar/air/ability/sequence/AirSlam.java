package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSwipe;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSlam",
        displayName = "AirSlam",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Raises the target vertically and repels it with a strong air flow, dealing damage",
        instruction = "Look at the target and follow these steps: AirSwipe \\(Hold Sneak\\) > AirBlast \\(Release Sneak\\) > AirBlast \\(Hold Sneak\\)",
        cooldown = 4000,
        cost = 10,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirSwipe.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK_RELEASE),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK)
})
public class AirSlam extends Ability {
    @ConfigField
    private static int range = 8;
    @ConfigField
    private static double power = 3;
    @ConfigField
    private static int particles = 6;
    @ConfigField
    private static double damage = 6;

    private ImmutableVector direction;
    private long time;
    private AbilityTargetLiving target;
    private RemovalConditional removal;
    private boolean launch = true;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        LivingEntity target = user.findLivingEntity(range, 2);

        if (target == null || !user.canUse(target.getLocation()) || target.getLocation().getBlock().isLiquid() || !user.removeEnergy(this)) {
            return ActivateResult.NOT_ACTIVATE;
        }

        this.target = AbilityTarget.of(target);

        user.destroyInstances(AirSwipe.class);
        user.destroyInstances(AirBlast.class);

        this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .build();

        this.direction = user.getDirection();
        target.setVelocity(new ImmutableVector(0, 2, 0));
        this.time = System.currentTimeMillis();
        user.setCooldown(this);
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        LivingEntity entity = target.getEntity();
        AirElement.display(entity.getLocation(), particles, 0.275f, 0.275f, 0.275f);
        if (launch && System.currentTimeMillis() > time + 50) {
            launch = false;
            entity.setNoDamageTicks(0);
            target.damage(damage, this);
            target.setVelocity(new ImmutableVector(direction.getX(), 0.05, direction.getZ()).multiply(power), this);
        }
        if (System.currentTimeMillis() > time + (400 * power)) {
            return UpdateResult.REMOVE;
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }
}
