package com.dndweapons.compat

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.Item
import net.minecraft.util.Identifier

object AttributeCompat {

    fun applyTo(settings: Item.Settings, spec: WeaponSpec): Item.Settings {
        val mods = AttributeModifiersComponent.builder()
            .add(
                EntityAttributes.ATTACK_DAMAGE,
                EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_damage_${spec.id}"),
                    spec.attackDamage - 1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE,
                ),
                AttributeModifierSlot.MAINHAND,
            )
            .add(
                EntityAttributes.ATTACK_SPEED,
                EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_speed_${spec.id}"),
                    spec.attackSpeed - 4.0,
                    EntityAttributeModifier.Operation.ADD_VALUE,
                ),
                AttributeModifierSlot.MAINHAND,
            )
            .build()

        return settings
            .maxDamage(spec.baseDurability)
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods)
    }

    fun storeFor(spec: WeaponSpec) {}

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> =
        ImmutableMultimap.of()
}
