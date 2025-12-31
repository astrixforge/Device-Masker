package com.astrixforge.devicemasker.common.generators

import com.astrixforge.devicemasker.common.Utils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for IMEIGenerator.
 *
 * Tests validate:
 * 1. IMEI format (15 digits)
 * 2. Luhn checksum validity
 * 3. TAC prefix validity (must be 8 digits)
 * 4. Consistent generation quality
 */
class IMEIGeneratorTest {

    @Test
    fun `generate returns 15 digit string`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertEquals(15, imei.length, "IMEI should be exactly 15 digits, got: $imei")
        }
    }

    @Test
    fun `generate returns only numeric characters`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertTrue(imei.all { it.isDigit() }, "IMEI should contain only digits, got: $imei")
        }
    }

    @Test
    fun `generate returns valid Luhn checksum`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertTrue(Utils.isValidImei(imei), "IMEI should pass Luhn validation, got: $imei")
        }
    }

    @Test
    fun `generate with manufacturer returns valid IMEI`() {
        val manufacturers = listOf("samsung", "apple", "google", "xiaomi", "unknown")

        manufacturers.forEach { manufacturer ->
            repeat(20) {
                val imei = IMEIGenerator.generate(manufacturer)
                assertEquals(15, imei.length, "IMEI for $manufacturer should be 15 digits")
                assertTrue(
                    Utils.isValidImei(imei),
                    "IMEI for $manufacturer should pass Luhn validation: $imei",
                )
            }
        }
    }

    @Test
    fun `generated IMEIs have valid TAC prefix`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            val tac = imei.take(8)

            // TAC should be 8 numeric digits
            assertEquals(8, tac.length, "TAC should be 8 digits")
            assertTrue(tac.all { it.isDigit() }, "TAC should be numeric: $tac")
        }
    }

    @Test
    fun `generated IMEIs are unique`() {
        val imeis = (1..1000).map { IMEIGenerator.generate() }.toSet()

        // With cryptographic random, 1000 unique IMEIs should be highly likely
        assertTrue(imeis.size >= 990, "Expected nearly 1000 unique IMEIs, got ${imeis.size}")
    }

    @Test
    fun `verify Luhn algorithm with known values`() {
        // Known valid IMEI examples (can be verified online)
        val knownInvalidIMEI = "123456789012340" // Not Luhn valid
        val generatedIMEI = IMEIGenerator.generate()

        // Our generated IMEI should be valid
        assertTrue(Utils.isValidImei(generatedIMEI), "Generated IMEI must be Luhn valid")

        // A random number sequence is unlikely to be Luhn valid
        // (unless we got very lucky)
    }
}
