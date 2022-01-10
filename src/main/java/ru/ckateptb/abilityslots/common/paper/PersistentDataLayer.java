package ru.ckateptb.abilityslots.common.paper;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import ru.ckateptb.abilityslots.AbilitySlots;

public class PersistentDataLayer {
    @Getter
    private static final PersistentDataLayer instance = new PersistentDataLayer(AbilitySlots.getInstance());
    private static final byte VALUE = (byte) 0x1;

    public static final String STR_ARMOR = "abilityslots-armor";
    public static final String STR_MATERIAL = "abilityslots-material";

    public final NamespacedKey NSK_ARMOR;
    public final NamespacedKey NSK_MATERIAL;

    public PersistentDataLayer(@NonNull AbilitySlots plugin) {
        NSK_ARMOR = new NamespacedKey(plugin, STR_ARMOR);
        NSK_MATERIAL = new NamespacedKey(plugin, STR_MATERIAL);
    }

    public boolean hasArmorKey(@Nullable ItemMeta meta) {
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(NSK_ARMOR, PersistentDataType.BYTE);
    }

    public boolean addArmorKey(@NonNull ItemMeta meta) {
        if (!hasArmorKey(meta)) {
            meta.getPersistentDataContainer().set(NSK_ARMOR, PersistentDataType.BYTE, VALUE);
            return true;
        }
        return false;
    }
}
