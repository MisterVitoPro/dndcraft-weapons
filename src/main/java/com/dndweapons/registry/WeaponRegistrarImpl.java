package com.dndweapons.registry;

import com.dndweapons.DndWeaponsMod;
import com.dndweapons.catalog.WeaponSpec;
import com.dndweapons.item.DndWeaponItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 1.21.4 implementation: data components + Identifier-keyed attribute modifiers.
 * In Phase 2 this file gets Stonecutter directives to also support the 1.20.1
 * (NBT + UUID-keyed override) and 26.1 (ItemStackTemplate) epochs.
 */
public final class WeaponRegistrarImpl implements WeaponRegistrar {

    @Override
    public void register(WeaponSpec spec) {
        if (spec.isVanillaMapped()) {
            // Phase 2 wires this up via role tags. For now skip silently.
            return;
        }

        Identifier itemId = Identifier.of(DndWeaponsMod.MOD_ID, spec.id());

        AttributeModifiersComponent attrs = AttributeModifiersComponent.builder()
            .add(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_damage_" + spec.id()),
                    spec.attackDamage() - 1.0,  // vanilla convention: net = setting - 1
                    EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
            )
            .add(
                EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_speed_" + spec.id()),
                    spec.attackSpeed() - 4.0,   // vanilla convention: net = setting - 4
                    EntityAttributeModifier.Operation.ADD_VALUE
                ),
                AttributeModifierSlot.MAINHAND
            )
            .build();

        Item.Settings settings = new Item.Settings()
            .maxDamage(spec.baseDurability())
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, attrs);

        Item item = new DndWeaponItem(spec, settings);
        Registry.register(Registries.ITEM, itemId, item);
        DndWeaponsMod.LOGGER.info("Registered weapon: {}", itemId);
    }
}
