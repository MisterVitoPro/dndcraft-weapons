package com.dndweapons.compat

import com.dndweapons.DndWeaponsMod
import com.dndweapons.catalog.WeaponSpec
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.item.component.ItemAttributeModifiers
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation
//?}
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as ResourceLocation*/
//?}
//?}

//? if <1.20.5 {
/*
import java.util.UUID
*/
//?}

/**
 * Per-epoch attribute-modifier builder.
 *
 * Epoch C (1.20.5+): bakes an `ItemAttributeModifiers` data component into Item.Properties.
 *   On 1.21.2+ the attribute fields are `ATTACK_DAMAGE` / `ATTACK_SPEED`;
 *   on 1.20.5-1.21.1 they retain the `GENERIC_` prefix.
 *   Modifier IDs are ResourceLocation-based; Operation.ADD_VALUE.
 *
 * Epoch A (<1.20.5): caches UUID-keyed modifiers per spec via Operation.ADDITION;
 *   DndWeaponItem.getAttributeModifiers(slot) override returns them at runtime.
 */
object AttributeCompat {

    //? if >=1.21.2 {
    private val damageAttr = Attributes.ATTACK_DAMAGE
    private val speedAttr = Attributes.ATTACK_SPEED
    //?} else {
    /*
    private val damageAttr = Attributes.GENERIC_ATTACK_DAMAGE
    private val speedAttr = Attributes.GENERIC_ATTACK_SPEED
    */
    //?}

    //? if >=1.20.5 {
    fun applyTo(settings: Item.Properties, spec: WeaponSpec): Item.Properties {
        val mods = ItemAttributeModifiers.builder()
            .add(
                damageAttr,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "base_attack_damage_${spec.id}"),
                    spec.attackDamage - 1.0,
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.MAINHAND,
            )
            .add(
                speedAttr,
                AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath(DndWeaponsMod.MOD_ID, "base_attack_speed_${spec.id}"),
                    spec.attackSpeed - 4.0,
                    AttributeModifier.Operation.ADD_VALUE,
                ),
                EquipmentSlotGroup.MAINHAND,
            )
            .build()
        return settings
            .durability(spec.baseDurability)
            .component(DataComponents.ATTRIBUTE_MODIFIERS, mods)
    }

    fun storeFor(spec: WeaponSpec) { /* no-op on Epoch C */ }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<Attribute, AttributeModifier> =
        ImmutableMultimap.of()
    //?}

    //? if <1.20.5 {
    /*
    private data class CachedMods(
        val damage: AttributeModifier,
        val speed: AttributeModifier,
    )

    private val store = mutableMapOf<String, CachedMods>()

    fun applyTo(settings: Item.Properties, spec: WeaponSpec): Item.Properties {
        storeFor(spec)
        return settings.durability(spec.baseDurability)
    }

    fun storeFor(spec: WeaponSpec) {
        store.getOrPut(spec.id) {
            CachedMods(
                damage = AttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:dmg:${spec.id}".toByteArray()),
                    "Weapon base attack damage",
                    (spec.attackDamage - 1).toDouble(),
                    AttributeModifier.Operation.ADDITION,
                ),
                speed = AttributeModifier(
                    UUID.nameUUIDFromBytes("dndweapons:spd:${spec.id}".toByteArray()),
                    "Weapon base attack speed",
                    (spec.attackSpeed - 4.0).toDouble(),
                    AttributeModifier.Operation.ADDITION,
                ),
            )
        }
    }

    fun modifiersFor(spec: WeaponSpec, slot: EquipmentSlot): Multimap<Attribute, AttributeModifier> {
        if (slot != EquipmentSlot.MAINHAND) return ImmutableMultimap.of()
        val cached = store[spec.id] ?: return ImmutableMultimap.of()
        return ImmutableMultimap.builder<Attribute, AttributeModifier>()
            .put(damageAttr, cached.damage)
            .put(speedAttr, cached.speed)
            .build()
    }
    */
    //?}
}
