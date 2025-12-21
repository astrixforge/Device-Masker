package com.astrixforge.devicemasker.common.generators

import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.SIMProfile

/**
 * Generates complete, correlated SIM card profiles.
 * 
 * This ensures all SIM-related values (IMSI, ICCID, carrier, phone number,
 * country ISO, operator name, network operator) are consistent and match
 * the same carrier.
 * 
 * This is CRITICAL for avoiding detection - in the real world, all these
 * values come from the same physical SIM card and MUST correlate.
 */
object SIMProfileGenerator {
    
    /**
     * Generates a complete SIM profile with all correlated values.
     * 
     * All derived fields (simCountryIso, networkCountryIso, simOperatorName,
     * networkOperator) are automatically populated from the carrier data.
     * 
     * @param carrier Optional specific carrier. If null, selects randomly.
     * @return SIMProfile with all values matching the carrier
     */
    fun generate(carrier: Carrier? = null): SIMProfile {
        // Select carrier (random if not specified)
        val selectedCarrier = carrier ?: Carrier.random()
        
        // Generate all values using the SAME carrier
        // SIMProfile.create() automatically populates derived fields
        return SIMProfile.create(
            carrier = selectedCarrier,
            imsi = IMSIGenerator.generate(selectedCarrier),
            iccid = ICCIDGenerator.generate(selectedCarrier),
            phoneNumber = PhoneNumberGenerator.generate(selectedCarrier)
        )
    }
    
    /**
     * Generates a SIM profile for a specific country.
     * 
     * @param countryIso ISO 3166-1 alpha-2 country code (e.g., "US", "IN")
     * @return SIMProfile from a carrier in that country
     */
    fun generateForCountry(countryIso: String): SIMProfile {
        val carriers = Carrier.getByCountry(countryIso)
        require(carriers.isNotEmpty()) {
            "No carriers found for country: $countryIso"
        }
        
        return generate(carriers.random())
    }
    
    /**
     * Generates a SIM profile for India.
     * Convenience method for the most common use case.
     * 
     * @return SIMProfile from a random India carrier (Airtel, Jio, Vi, BSNL)
     */
    fun generateForIndia(): SIMProfile {
        return generate(Carrier.randomIndia())
    }
    
    /**
     * Generates a SIM profile for a specific carrier by name.
     * 
     * @param carrierName Name of carrier (e.g., "Airtel", "Jio")
     * @return SIMProfile for the carrier, or null if not found
     */
    fun generateForCarrier(carrierName: String): SIMProfile? {
        val carriers = Carrier.getByName(carrierName)
        if (carriers.isEmpty()) return null
        return generate(carriers.random())
    }
}
