package ru.ckateptb.abilityslots.avatar.chi.ability;

import lombok.NoArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;
import ru.ckateptb.abilityslots.avatar.chi.ChiElement;
import ru.ckateptb.abilityslots.predicate.AbilityConditional;
import ru.ckateptb.abilityslots.service.AbilityUserService;
import ru.ckateptb.abilityslots.user.AbilityUser;
import ru.ckateptb.tablecloth.config.ConfigField;
import ru.ckateptb.tablecloth.math.ImmutableVector;
import ru.ckateptb.tablecloth.spring.SpringContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@AbilityInfo(
        author = "CKATEPTb",
        name = "ChiBlock",
        displayName = "ChiBlock",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "chi",
        description = "This is a passive ability that allows you to disable your opponent's abilities.",
        instruction = "Triggers with some chance when you attack an enemy",
        canBindToSlot = false
)
public class ChiBlock extends Ability {
    @ConfigField
    private static int chance = 15;
    @ConfigField
    private static int backAttackChance = 35;
    @ConfigField
    private static long duration = 2500;
    @ConfigField
    private static long backAttackDuration = 5000;
    @ConfigField
    private static int backAttackAngle = 90;

    private final Map<AbilityUser, Long> expiresMap = new HashMap<>();

    private final AbilityConditional chiBlock = (user, ability) -> false;

    @Override
    public ActivateResult activate(ActivationMethod method) {
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        long currentTimeMillis = System.currentTimeMillis();
        List<AbilityUser> toRemove = new ArrayList<>();
        this.expiresMap.forEach((target, expireTime) -> {
            if (currentTimeMillis > expireTime) {
                toRemove.add(target);
                target.removeAbilityActivateConditional(chiBlock);
            }
        });
        toRemove.forEach(this.expiresMap::remove);
        toRemove.clear();
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        this.expiresMap.forEach((target, expireTime) -> target.removeAbilityActivateConditional(chiBlock));
        this.expiresMap.clear();
    }

    @NoArgsConstructor
    public class ChiBlockHandler implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void onPlayerMove(EntityDamageByEntityEvent event) {
            Entity damager = event.getDamager();
            if (damager == livingEntity) {
                Entity target = event.getEntity();
                if(!(target instanceof LivingEntity targetLiving)) return;
                AbilityUser targetUser = SpringContext.getInstance().getBean(AbilityUserService.class).getAbilityUser(targetLiving);
                if (targetUser != null) {
                    double activationAngle = Math.toRadians(backAttackAngle);
                    ImmutableVector targetDirection = targetUser.getDirection().setY(0).normalize();
                    ImmutableVector targetLocation = targetUser.getLocation();
                    ImmutableVector userLocation = user.getLocation();
                    ImmutableVector toTarget = targetLocation.subtract(userLocation).setY(0).normalize();
                    double angle = toTarget.angle(targetDirection);
                    boolean back = angle <= activationAngle;
                    if (targetLocation.distance(userLocation) <= ChiElement.getHitActivationRange()) {
                        if (ThreadLocalRandom.current().nextInt(100) <= (back ? backAttackChance : chance)) {
                            World world = targetUser.getWorld();
                            world.playSound(targetLocation.toLocation(world), Sound.ENTITY_ENDER_DRAGON_HURT, 2, 0);
                            targetUser.addAbilityActivateConditional(chiBlock);
                            expiresMap.put(targetUser, System.currentTimeMillis() + (back ? backAttackDuration : duration));
                        }
                    }
                }
            }
        }
    }
}
