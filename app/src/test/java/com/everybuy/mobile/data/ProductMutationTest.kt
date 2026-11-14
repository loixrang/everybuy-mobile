package com.everybuy.mobile.data

import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductMutationTest {
    @Test fun `optional product fields clear with json null while unchanged cost is omitted`() {
        val json = ProductMutation("Rice", "c1", "b1", "Small Pack", null, 10, null, "8000", "7000", null, null, "900", "800", null, true).toJson()
        assertEquals(JsonNull, json["size"])
        assertEquals(JsonNull, json["piecesPerPack"])
        assertEquals(JsonNull, json["halfBagPrice"])
        assertNull(json["costPerPack"])
    }
}
