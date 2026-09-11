package com.integration.systema.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerATest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsSeededCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void listWithFutureUpdatedSinceReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/customers").param("updatedSince", "2030-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByIdReturnsSeededCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.emailAddress").value("alice@example.com"));
    }

    @Test
    void getByIdReturns404ForUnknownCustomer() throws Exception {
        mockMvc.perform(get("/api/customers/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCreatesCustomer() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Zoe Test",
                                  "emailAddress": "zoe@example.com",
                                  "phoneNumber": "+1-555-0000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fullName").value("Zoe Test"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void updateModifiesExistingCustomer() throws Exception {
        mockMvc.perform(put("/api/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alice Updated",
                                  "emailAddress": "alice@example.com",
                                  "phoneNumber": "+1-555-9999"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alice Updated"))
                .andExpect(jsonPath("$.phoneNumber").value("+1-555-9999"));
    }
}