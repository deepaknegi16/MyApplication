package com.hackerrank.hotel;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class HotelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor user() {
        return httpBasic("user", "user123");
    }

    private RequestPostProcessor admin() {
        return httpBasic("admin", "admin123");
    }

    // ---------- Q1: GET /hotel/{id} ----------

    @Test
    void getHotelByIdReturnsHotel() throws Exception {
        mockMvc.perform(get("/hotel/1").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("The Imperial"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getUnknownHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/9999").with(user()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSoftDeletedHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/10").with(user()))
                .andExpect(status().isNotFound());
    }

    // ---------- Q2: DELETE /hotel/{id} (soft delete) ----------

    @Test
    void deleteHotelMarksItDeletedInsteadOfRemovingIt() throws Exception {
        mockMvc.perform(delete("/hotel/4").with(admin()))
                .andExpect(status().isNoContent());

        // soft-deleted hotels behave as gone for the API...
        mockMvc.perform(get("/hotel/4").with(user()))
                .andExpect(status().isNotFound());

        // ...and no longer show up in search results
        mockMvc.perform(get("/search/1").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 4)]", hasSize(0)));
    }

    // ---------- Q3: GET /search/{cityId} ----------

    @Test
    void searchReturnsHotelsSortedByDistanceToCityCenter() throws Exception {
        // Mumbai (city 2): ITC Maratha is closest to the center, Taj Mahal Palace farthest
        mockMvc.perform(get("/search/2").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("ITC Maratha"))
                .andExpect(jsonPath("$[1].name").value("Trident Nariman Point"))
                .andExpect(jsonPath("$[2].name").value("Taj Mahal Palace"));
    }

    @Test
    void searchExcludesSoftDeletedHotels() throws Exception {
        // Bengaluru (city 3) has 3 hotels seeded, one already marked deleted
        mockMvc.perform(get("/search/3").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchUnknownCityReturns404() throws Exception {
        mockMvc.perform(get("/search/9999").with(user()))
                .andExpect(status().isNotFound());
    }

    // ---------- Security ----------

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/hotel/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/search/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRequiresAdminRole() throws Exception {
        mockMvc.perform(delete("/hotel/1").with(user()))
                .andExpect(status().isForbidden());

        // the hotel is untouched
        mockMvc.perform(get("/hotel/1").with(user()))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerAndHealthArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    // ---------- Validation ----------

    @Test
    void nonPositiveIdReturns400() throws Exception {
        mockMvc.perform(get("/hotel/-1").with(user()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/hotel/abc").with(user()))
                .andExpect(status().isBadRequest());
    }
}
