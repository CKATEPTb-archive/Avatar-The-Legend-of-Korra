package ru.ckateptb.abilityslots.avatar.chi;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;
import ru.ckateptb.tablecloth.config.ConfigField;

@Getter
@Setter
public class ChiElement extends AbstractAbilityCategory {
    @ConfigField
    @Getter
    private static double hitActivationRange = 6;
    private final String name = "Chi";
    private String displayName = "§5Chi";
    private String prefix = "§5";
}
