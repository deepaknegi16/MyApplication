package com.hackerrank.hotel;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HotelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String userToken;
    private String adminToken;

    @BeforeEach
    void obtainTokens() throws Exception {
        userToken = login("user", "user123");
        adminToken = login("admin", "admin123");
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- Auth: POST /auth/login ----------

    @Test
    void loginReturnsTokenWithExpiry() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"user123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Q1: GET /hotel/{id} ----------

    @Test
    void getHotelByIdReturnsHotel() throws Exception {
        mockMvc.perform(get("/hotel/1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("The Imperial"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getUnknownHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/9999").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSoftDeletedHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/10").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    // ---------- Q2: DELETE /hotel/{id} (soft delete) ----------

    @Test
    void deleteHotelMarksItDeletedInsteadOfRemovingIt() throws Exception {
        mockMvc.perform(delete("/hotel/4").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        // soft-deleted hotels behave as gone for the API...
        mockMvc.perform(get("/hotel/4").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());

        // ...and no longer show up in search results
        mockMvc.perform(get("/search/1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 4)]", hasSize(0)));
    }

    // ---------- Q3: GET /search/{cityId} ----------

    @Test
    void searchReturnsHotelsSortedByDistanceToCityCenter() throws Exception {
        // Mumbai (city 2): ITC Maratha is closest to the center, Taj Mahal Palace farthest
        mockMvc.perform(get("/search/2").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("ITC Maratha"))
                .andExpect(jsonPath("$[1].name").value("Trident Nariman Point"))
                .andExpect(jsonPath("$[2].name").value("Taj Mahal Palace"));
    }

    @Test
    void searchExcludesSoftDeletedHotels() throws Exception {
        // Bengaluru (city 3) has 3 hotels seeded, one already marked deleted
        mockMvc.perform(get("/search/3").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchUnknownCityReturns404() throws Exception {
        mockMvc.perform(get("/search/9999").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchRespectsLimitParam() throws Exception {
        mockMvc.perform(get("/search/2").param("limit", "1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("ITC Maratha"));
    }

    // ---------- PUT /hotel/{id} ----------

    @Test
    void adminCanUpdateHotelAndAuditTimestampsExist() throws Exception {
        mockMvc.perform(put("/hotel/3")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Radisson Blu Dwarka Renovated\",\"latitude\":28.5823,\"longitude\":77.05,\"rating\":5,\"time\":\"2026-08-18T14:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Radisson Blu Dwarka Renovated"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.time").value("2026-08-18T14:30:00"))
                .andExpect(jsonPath("$.createdAt").exists());

        // cache was evicted, so a fresh GET sees the update
        mockMvc.perform(get("/hotel/3").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Radisson Blu Dwarka Renovated"));
    }

    @Test
    void updateRequiresAdminRole() throws Exception {
        mockMvc.perform(put("/hotel/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacked\",\"latitude\":0,\"longitude\":0,\"rating\":1,\"time\":\"2026-08-18T14:30:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        // rating 9 violates @Max(5); blank name violates @NotBlank
        mockMvc.perform(put("/hotel/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"latitude\":28.6,\"longitude\":77.2,\"rating\":9,\"time\":\"2026-08-18T14:30:00\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- POST /hotel ----------

    @Test
    void adminCanCreateHotelWithTime() throws Exception {
        String body = mockMvc.perform(post("/hotel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"The Oberoi\",\"latitude\":28.6035,\"longitude\":77.2405,"
                                + "\"rating\":5,\"cityId\":1,\"time\":\"2026-08-18T09:15:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("The Oberoi"))
                .andExpect(jsonPath("$.time").value("2026-08-18T09:15:00"))
                .andReturn().getResponse().getContentAsString();

        // the created hotel is readable and the time was persisted
        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(get("/hotel/" + id).header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("The Oberoi"))
                .andExpect(jsonPath("$.time").value("2026-08-18T09:15:00"));
    }

    @Test
    void createRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/hotel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sneaky Hotel\",\"latitude\":28.6,\"longitude\":77.2,"
                                + "\"rating\":3,\"cityId\":1,\"time\":\"2026-08-18T09:15:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWithoutTimeReturns400() throws Exception {
        // time is @NotNull, so omitting it fails validation
        mockMvc.perform(post("/hotel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Time Hotel\",\"latitude\":28.6,\"longitude\":77.2,"
                                + "\"rating\":3,\"cityId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createInUnknownCityReturns404() throws Exception {
        mockMvc.perform(post("/hotel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nowhere Inn\",\"latitude\":28.6,\"longitude\":77.2,"
                                + "\"rating\":3,\"cityId\":9999,\"time\":\"2026-08-18T09:15:00\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /city ----------

    @Test
    void cityListReturnsAllCities() throws Exception {
        mockMvc.perform(get("/city").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // ---------- Security ----------

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(get("/hotel/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/search/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithGarbageTokenAreRejected() throws Exception {
        mockMvc.perform(get("/hotel/1").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteRequiresAdminRole() throws Exception {
        mockMvc.perform(delete("/hotel/1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        // the hotel is untouched
        mockMvc.perform(get("/hotel/1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
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
        mockMvc.perform(get("/hotel/-1").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/hotel/abc").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isBadRequest());
    }
}
