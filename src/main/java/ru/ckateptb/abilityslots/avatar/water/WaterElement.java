package ru.ckateptb.abilityslots.avatar.water;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class WaterElement extends AbstractAbilityCategory {
    private final String name = "Water";
    private String displayName = "§1Water";
    private String prefix = "§1";
}
