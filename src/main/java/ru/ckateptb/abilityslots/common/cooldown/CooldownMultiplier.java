package ru.ckateptb.abilityslots.common.cooldown;

import ru.ckateptb.abilityslots.ability.Ability;

public class CooldownMultiplier {
    private final Ability ability;
    private final long increase;
    private final long threshold;
    private long expireTime;
    private int count = 0;

    public CooldownMultiplier(Ability ability, long threshold, long increase) {
        this.ability = ability;
        this.threshold = threshold;
        this.increase = increase;
    }

    public long increaseAndGetCooldown() {
        long cooldown = ability.getInformation().getCooldown();
        long timeMillis = System.currentTimeMillis();
        if (count > 0) {
            if(timeMillis < expireTime) {
                cooldown += (increase * count);
            } else {
                count = 0;
            }
        }
        this.count++;
        this.expireTime = timeMillis + threshold;
        return cooldown;
    }
}
