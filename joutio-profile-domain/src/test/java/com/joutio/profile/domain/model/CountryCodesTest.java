package com.joutio.profile.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountryCodesTest {

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "FR, FR",
            "be, BE",
            "' ch ', CH",
            "France, FR",
            "Algérie, DZ",
            "algerie, DZ",
            "Côte d’Ivoire, CI",
            "Royaume-Uni, GB",
            "United States, US",
            "États-Unis, US",
            "Atlantide, NULL",
            "ZZ, NULL",
            "'', NULL",
    })
    void normalize(String input, String expected) {
        assertEquals(expected, CountryCodes.normalize(input));
    }
}
