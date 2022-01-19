package ru.ckateptb.abilityslots.avatar.chi.ability;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.chi.ChiElement;
import ru.ckateptb.abilityslots.entity.AbilityTarget;
import ru.ckateptb.abilityslots.entity.AbilityTargetLiving;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;

@Getter
@AbilityInfo(
        author = "CKATEPTb",
        name = "RapidPunch",
        displayName = "RapidPunch",
        activationMethods = {ActivationMethod.LEFT_CLICK},
        category = "chi",
        description = "Example Description",
        instruction = "Example Instruction",
        cooldown = 3500
)
public class RapidPunch extends Ability {
    @ConfigField
    private static int punches = 10;
    @ConfigField
    private static double damagePerPunch = 0.5;
    @ConfigField
    private static long interval = 50;

    private LivingEntity target;
    private AbilityTargetLiving abilityTarget;
    private long expireTime;
    private int punchLefts;
    private RemovalConditional removal;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        if (user.hasAbility(RapidPunch.class)) return ActivateResult.NOT_ACTIVATE;
        this.target = user.findLivingEntity(ChiElement.getHitActivationRange());
        if (target == null) return ActivateResult.NOT_ACTIVATE;
        Location targetLocation = target.getLocation();
        if (!user.canUse(targetLocation) || !user.removeEnergy(this)) return ActivateResult.NOT_ACTIVATE;
        this.abilityTarget = AbilityTarget.of(target);
        this.expireTime = System.currentTimeMillis();
        RemovalConditional.Builder builder = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .slot()
                .range(() -> livingEntity.getLocation(), () -> target.getLocation(), ChiElement.getHitActivationRange())
                .custom((user, ability) -> target.isDead())
                .custom((user, ability) -> !world.equals(target.getWorld()));
        if (target instanceof Player targetPlayer) {
            builder.custom((user, ability) -> !targetPlayer.isOnline());
        }
        this.removal = builder.build();
        this.punchLefts = punches;
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        if(removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
        if (System.currentTimeMillis() > expireTime) punch();
        return punchLefts < 1 ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
    }

    private void punch() {
        this.expireTime = System.currentTimeMillis() + interval;
        abilityTarget.damage(damagePerPunch, this, true);
        abilityTarget.setVelocity(ImmutableVector.ZERO, this);
        if (livingEntity instanceof Player player) {
            switch (punchLefts % 2) {
                case 0 -> player.swingMainHand();
                case 1 -> player.swingOffHand();
            }
        }
        punchLefts--;
    }

    @Override
    public void destroy() {
        user.setCooldown(this);
    }
}
