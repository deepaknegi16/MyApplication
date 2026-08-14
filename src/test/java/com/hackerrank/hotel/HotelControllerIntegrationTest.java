package com.hackerrank.hotel;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HotelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHotelByIdReturnsHotel() throws Exception {
        mockMvc.perform(get("/hotel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("The Imperial"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getUnknownHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteHotelMarksItDeletedInsteadOfRemovingIt() throws Exception {
        mockMvc.perform(delete("/hotel/4"))
                .andExpect(status().isNoContent());

        // soft-deleted hotels behave as gone for the API...
        mockMvc.perform(get("/hotel/4"))
                .andExpect(status().isNotFound());

        // ...and no longer show up in search results
        mockMvc.perform(get("/search/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 4)]", hasSize(0)));
    }

    @Test
    void getSoftDeletedHotelReturns404() throws Exception {
        mockMvc.perform(get("/hotel/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturnsHotelsSortedByDistanceToCityCenter() throws Exception {
        // Mumbai (city 2): ITC Maratha is closest to the center, Taj Mahal Palace farthest
        mockMvc.perform(get("/search/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("ITC Maratha"))
                .andExpect(jsonPath("$[1].name").value("Trident Nariman Point"))
                .andExpect(jsonPath("$[2].name").value("Taj Mahal Palace"));
    }

    @Test
    void searchExcludesSoftDeletedHotels() throws Exception {
        // Bengaluru (city 3) has 3 hotels seeded, one already marked deleted
        mockMvc.perform(get("/search/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchUnknownCityReturns404() throws Exception {
        mockMvc.perform(get("/search/9999"))
                .andExpect(status().isNotFound());
    }
}
