package ru.ckateptb.abilityslots.avatar.air.ability.passive;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import ru.ckateptb.abilityslots.AbilitySlots;
import ru.ckateptb.abilityslots.ability.Ability;
import ru.ckateptb.abilityslots.ability.enums.ActivateResult;
import ru.ckateptb.abilityslots.ability.enums.ActivationMethod;
import ru.ckateptb.abilityslots.ability.enums.UpdateResult;
import ru.ckateptb.abilityslots.ability.info.AbilityInfo;

@AbilityInfo(
        author = "CKATEPTb",
        name = "GracefulDescent",
        displayName = "GracefulDescent",
        activationMethods = {ActivationMethod.PASSIVE},
        category = "air",
        description = "Is a passive ability which allows AirBenders to make a gentle landing, negating all fall damage on any surface.",
        instruction = "Passive Ability",
        canBindToSlot = false
)
public class GracefulDescent extends Ability {
    private FallHandler listener;

    @Override
    public ActivateResult activate(ActivationMethod activationMethod) {
        this.listener = new FallHandler();
        Bukkit.getPluginManager().registerEvents(listener, AbilitySlots.getInstance());
        return ActivateResult.ACTIVATE;
    }

    @Override
    public UpdateResult update() {
        return UpdateResult.CONTINUE;
    }

    @Override
    public void destroy() {
        EntityDamageEvent.getHandlerList().unregister(listener);
    }

    private class FallHandler implements Listener {
        @EventHandler(priority = EventPriority.LOWEST)
        public void on(EntityDamageEvent event) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                if (event.getEntity() instanceof LivingEntity entity && livingEntity == entity) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
