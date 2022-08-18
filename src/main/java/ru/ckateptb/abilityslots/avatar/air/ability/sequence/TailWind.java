package ru.ckateptb.abilityslots.avatar.air.ability.sequence;

import java.util.concurrent.CompletableFuture;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import lombok.Getter;
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
import ru.ckateptb.abilityslots.avatar.air.ability.AirBurst;
import ru.ckateptb.abilityslots.predicate.RemovalConditional;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@AbilityInfo(
        author = "MordaSobaki",
        name = "Tailwind",
        displayName = "Tailwind",
        activationMethods = {ActivationMethod.SEQUENCE},
        category = "air",
        description = "Создайте попутный ветер позади себя чтобы ускориться",
        instruction = "AirBlast \\(Hold Sneak\\) > AirBurst \\(Release Sneak\\)",
        cooldown = 7000,
        cost = 20,
        canBindToSlot = false
)
@Sequence({
    @AbilityAction(ability = AirBlast.class, action = SequenceAction.SNEAK),
    @AbilityAction(ability = AirBurst.class, action = SequenceAction.SNEAK_RELEASE)
})
public class TailWind extends Ability{

	
	@ConfigField
    private static long duration = 6000;
	
	@ConfigField
    private static int amplifier = 3;
	
	private RemovalConditional removal;
	
	
	@Override
	public ActivateResult activate(ActivationMethod arg0) {
		
		if (livingEntity.getEyeLocation().getBlock().isLiquid()) return ActivateResult.NOT_ACTIVATE;
		
		this.removal = new RemovalConditional.Builder()
                .offline()
                .dead()
                .world()
                .custom((user, ability) -> user.getEyeLocation().toBlock(world).isLiquid())
                .duration(duration)
                .build();
		
		user.addPotionEffect(this, new PotionEffect(PotionEffectType.SPEED, (int) (duration/50), amplifier));
		return ActivateResult.ACTIVATE;
	}

	@Override
	public void destroy() {
		user.setCooldown(this);
		user.removeEnergy(this);
		
	}

	@Override
	public UpdateResult update() {
		if (removal.shouldRemove(user, this)) return UpdateResult.REMOVE;
		
		CompletableFuture.runAsync(() -> {
            for (int i = 0; i <= 4; ++i) {
                AirElement.display(livingEntity.getLocation().add(0, -0.4 + (0.5*i), 0), 2, 0.2f, 0.2f, 0.2f);
            }
            AirElement.sound(livingEntity.getLocation());
        });
		
		return UpdateResult.CONTINUE;
	}

}
