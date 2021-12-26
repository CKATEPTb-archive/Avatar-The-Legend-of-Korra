package ru.ckateptb.abilityslots.avatar.earth;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class EarthElement extends AbstractAbilityCategory {
    private final String name = "Earth";
    private String displayName = "§2Earth";
    private String prefix = "§2";
}
