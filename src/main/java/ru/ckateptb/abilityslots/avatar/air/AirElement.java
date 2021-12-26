package ru.ckateptb.abilityslots.avatar.air;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class AirElement extends AbstractAbilityCategory {
    private final String name = "Air";
    private String displayName = "§7Air";
    private String prefix = "§7";
}
