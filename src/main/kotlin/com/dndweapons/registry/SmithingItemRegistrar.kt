// src/main/kotlin/com/dndweapons/registry/SmithingItemRegistrar.kt
package com.dndweapons.registry

import com.dndweapons.item.SmithingComponentItems
import com.dndweapons.item.SmithingTemplateItems

/** Phase 4 facade: register the 8 smithing-system items as a single call. */
object SmithingItemRegistrar {
    fun registerAll() {
        SmithingComponentItems.registerAll()
        SmithingTemplateItems.registerAll()
    }
}
