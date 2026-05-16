package com.dndweapons.compat

import com.dndweapons.catalog.WeaponSpec
import net.minecraft.item.Item

//? if MC >= 1.21 {
import com.dndweapons.DndWeaponsMod
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.util.Identifier

/**
 * Per-epoch attribute-modifier builder. Stonecutter directives select the body
 * at build time.
 *
 * Epoch C (1.21+): writes attributes into the ATTRIBUTE_MODIFIERS data component
 * on Item.Settings. No Item.getAttributeModifiers override needed.
 *
 * Epoch A (1.20.1): cached UUID-keyed modifiers per spec; DndWeaponItem
 * overrides getAttributeModifiers(slot) and delegates here.
 */
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

    /** No-op on Epoch C. On Epoch A this caches UUID-keyed modifiers per spec. */
    fun storeFor(spec: WeaponSpec) {
        // intentional no-op on Epoch C
    }

    /** Empty on Epoch C (attributes resolved via data component). Epoch A returns the cached pair. */
    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> =
        ImmutableMultimap.of()
}
//? } else {
/*
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import java.util.UUID

object AttributeCompat {

    private data class CachedMods(
        val damage: EntityAttributeModifier,
        val speed: EntityAttributeModifier,
    )

    private val store = mutableMapOf<String, CachedMods>()

    fun applyTo(settings: Item.Settings, spec: WeaponSpec): Item.Settings {
        storeFor(spec)
        return settings.maxDamage(spec.baseDurability)
    }

    fun storeFor(spec: WeaponSpec) {
        store.getOrPut(spec.id) {
            CachedMods(
                damage = EntityAttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:dmg:${spec.id}".toByteArray()),
                    "Weapon base attack damage",
                    (spec.attackDamage - 1).toDouble(),
                    EntityAttributeModifier.Operation.ADDITION,
                ),
                speed = EntityAttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:spd:${spec.id}".toByteArray()),
                    "Weapon base attack speed",
                    (spec.attackSpeed - 4.0).toDouble(),
                    EntityAttributeModifier.Operation.ADDITION,
                ),
            )
        }
    }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of()
        val cached = store[spec.id] ?: return ImmutableMultimap.of()
        return ImmutableMultimap.builder<EntityAttribute, EntityAttributeModifier>()
            .put(EntityAttributes.GENERIC_ATTACK_DAMAGE, cached.damage)
            .put(EntityAttributes.GENERIC_ATTACK_SPEED, cached.speed)
            .build()
    }
}
*/
//? }
