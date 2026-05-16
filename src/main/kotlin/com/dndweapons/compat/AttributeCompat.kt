package com.dndweapons.compat

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.item.Item
//? if >=1.21 {
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.AttributeModifierSlot
import net.minecraft.component.type.AttributeModifiersComponent
import net.minecraft.util.Identifier
//?}
//? if <1.21 {
/*import java.util.UUID
*///?}

/**
 * Per-epoch attribute-modifier builder.
 *
 * Epoch C (1.21+): bakes an `AttributeModifiersComponent` into `Item.Settings`.
 *   On 1.21.2+ the attribute fields are `ATTACK_DAMAGE` / `ATTACK_SPEED`;
 *   on 1.21–1.21.1 they retain the `GENERIC_` prefix.
 *
 * Epoch A (<1.21): caches UUID-keyed modifiers per spec; the
 *   `DndWeaponItem.getAttributeModifiers(slot)` override (Stonecutter-forked)
 *   returns them at runtime.
 */
object AttributeCompat {

    //? if >=1.21.2 {
    private val damageAttr = EntityAttributes.ATTACK_DAMAGE
    private val speedAttr = EntityAttributes.ATTACK_SPEED
    //?} else {
    
    /*private val damageAttr = EntityAttributes.GENERIC_ATTACK_DAMAGE
    private val speedAttr = EntityAttributes.GENERIC_ATTACK_SPEED
    
    *///?}

    //? if >=1.21 {
    fun applyTo(settings: Item.Settings, spec: WeaponSpec): Item.Settings {
        val mods = AttributeModifiersComponent.builder()
            .add(
                damageAttr,
                EntityAttributeModifier(
                    Identifier.of(DndWeaponsMod.MOD_ID, "base_attack_damage_${spec.id}"),
                    spec.attackDamage - 1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE,
                ),
                AttributeModifierSlot.MAINHAND,
            )
            .add(
                speedAttr,
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

    fun storeFor(spec: WeaponSpec) { /* no-op on Epoch C */ }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<EntityAttribute, EntityAttributeModifier> =
        ImmutableMultimap.of()
    //?}

    //? if <1.21 {
    
    /*private data class CachedMods(
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
            .put(damageAttr, cached.damage)
            .put(speedAttr, cached.speed)
            .build()
    }
    
    *///?}
}
