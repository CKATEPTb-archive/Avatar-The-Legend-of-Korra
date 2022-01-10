package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.SequenceAction;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.ability.info.AbilityInformation;
import ru.ckateptb.abilityslots.ability.sequence.AbilityAction;
import ru.ckateptb.abilityslots.ability.sequence.Sequence;
import ru.ckateptb.abilityslots.avatar.air.AirElement;
import ru.ckateptb.abilityslots.avatar.air.ability.AirBlast;
import ru.ckateptb.abilityslots.avatar.air.ability.AirSwipe;
import ru.ckateptb.abilityslots.removalpolicy.CompositeRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsDeadRemovalPolicy;
import ru.ckateptb.abilityslots.removalpolicy.IsOfflineRemovalPolicy;
import ru.ckateptb.abilityslots.service.AbilityInstanceService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.collision.RayTrace;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.Vector3d;
import ru.ckateptb.tablecloth.temporary.flight.TemporaryFlight;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "AirSlam",
        displayName = "AirSlam",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Raises the target vertically and repels it with a strong air flow, dealing damage",
        instruction = "Look at the target and follow these steps: AirSwipe (Hold Sneak) > AirBlast (Release Sneak) > AirBlast (Hold Sneak)",
        cooldown = 4000,
        canBindToSlot = false
)
@Sequence({
        @AbilityAction(ability = AirSwipe.class, action = SequenceAction.SNEAK),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK_RELEASE),
        @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK)
})
public class AirSlam implements Ability {
    @ConfigField
    private static int range = 8;
    @ConfigField
    private static double power = 3;
    @ConfigField
    private static int particles = 6;
    @ConfigField
    private static double damage = 6;

    private AbilityUser user;
    private LivingEntity livingEntity;

    private Vector3d direction;
    private long time;
    private LivingEntity target;
    private CompositeRemovalPolicy removalPolicy;
    private boolean launch = true;

    @Override
    public ActivateResult activate(AbilityUser user, ActivationMethod method) {
        this.setUser(user);

        this.target = (LivingEntity) RayTrace.of(livingEntity)
                .range(range)
                .raySize(2)
                .type(RayTrace.Type.ENTITY)
                .filter(e -> e instanceof LivingEntity && e != livingEntity)
                .result(livingEntity.getWorld()).entity();

        if (target == null || !user.canUse(target.getLocation()) || target.getLocation().getBlock().isLiquid()) {
            return ActivateResult.NOT_ACTIVATE;
        }

        AbilityInstanceService abilityInstanceService = getAbilityInstanceService();
        abilityInstanceService.destroyInstanceType(user, AirSwipe.class);
        abilityInstanceService.destroyInstanceType(user, AirBlast.class);

        this.removalPolicy = new CompositeRemovalPolicy(
                new IsDeadRemovalPolicy(user),
                new IsOfflineRemovalPolicy(user)
        );

        this.direction = new Vector3d(livingEntity.getEyeLocation().getDirection());
        target.setVelocity(new Vector3d(0, 2, 0).toBukkitVector());
        new TemporaryFlight(target, 20000, true, true, false);
        this.time = System.currentTimeMillis();
        AbilityInformation information = getInformation();
        user.setCooldown(information, information.getCooldown());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if (removalPolicy.shouldRemove()) {
            return UpdateResult.REMOVE;
        }
        AirElement.display(target.getLocation(), particles, 0.275f, 0.275f, 0.275f);
        if (launch && System.currentTimeMillis() > time + 50) {
            launch = false;
            target.setNoDamageTicks(0);
            target.damage(damage, livingEntity);
            target.setVelocity(new Vector3d(direction.getX(), 0.05, direction.getZ()).multiply(power).toBukkitVector());
        }
        if (System.currentTimeMillis() > time + (400 * power)) {
            return UpdateResult.REMOVE;
        }
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void setUser(AbilityUser user) {
        this.user = user;
        this.livingEntity = user.getEntity();
    }
}
