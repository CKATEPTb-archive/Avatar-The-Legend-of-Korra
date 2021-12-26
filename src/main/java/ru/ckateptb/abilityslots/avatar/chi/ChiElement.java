package ru.ckateptb.abilityslots.avatar.chi;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class ChiElement extends AbstractAbilityCategory {
    private final String name = "Chi";
    private String displayName = "§5Chi";
    private String prefix = "§5";
}
