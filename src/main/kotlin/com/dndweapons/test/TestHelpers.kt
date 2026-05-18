package com.dndweapons.test

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

internal fun primeAttackStrength(entity: LivingEntity) {
    entity.attackStrengthTicker = 100
    applyMainHandModifiers(entity)
}

// Mock players spawned via GameTestHelper.makeMockPlayer have a fresh AttributeMap
// with no equipment modifiers applied. setItemInHand only updates the inventory;
// the ATTACK_DAMAGE/ATTACK_SPEED bonuses from the held weapon are normally folded
// into the AttributeMap by LivingEntity.aiStep() -> collectEquipmentChanges() on
// the next tick. Gametests don't tick the player between spawn and attack, so we
// apply the held-item modifiers explicitly here.
internal fun applyMainHandModifiers(entity: LivingEntity) {
    val stack = entity.mainHandItem
    if (stack.isEmpty) return
    try {
        applyMainHandModifiersImpl(entity, stack)
    } catch (t: Throwable) {
        println("[applyMainHandModifiers] threw on ${entity.javaClass.name} with ${stack.item}: ${t.javaClass.name}: ${t.message}")
        t.printStackTrace()
        throw t
    }
}

private fun applyMainHandModifiersImpl(entity: LivingEntity, stack: ItemStack) {
    //? if >=1.20.5 {
    val mods = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS) ?: return
    val map = com.google.common.collect.HashMultimap.create<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>()
    for (entry in mods.modifiers()) {
        if (entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND ||
            entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.HAND ||
            entry.slot() == net.minecraft.world.entity.EquipmentSlotGroup.ANY) {
            map.put(entry.attribute(), entry.modifier())
        }
    }
    entity.attributes.addTransientAttributeModifiers(map)
    //?} else {
    /*val item = stack.item
    val mm = item.getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
    entity.attributes.addTransientAttributeModifiers(mm)
    *///?}
}
