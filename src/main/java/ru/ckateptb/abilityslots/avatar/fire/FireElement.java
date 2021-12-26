package ru.ckateptb.abilityslots.avatar.fire;

import lombok.Getter;
import lombok.Setter;
import ru.ckateptb.abilityslots.category.AbstractAbilityCategory;

@Getter
@Setter
public class FireElement extends AbstractAbilityCategory {
    private final String name = "Fire";
    private String displayName = "§4Fire";
    private String prefix = "§4";
}
