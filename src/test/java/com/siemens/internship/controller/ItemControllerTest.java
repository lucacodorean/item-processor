package com.siemens.internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@EnableAsync
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllItems_ShouldReturnList() throws Exception {
        List<Item> items = List.of(new Item(1L, "Test Item", "A test description", "NEW", "test@example.com"));
        Mockito.when(itemService.findAll()).thenReturn(items);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Item"));
    }

    @Test
    void getAllItems_ShouldReturnNoContent() throws Exception {
        Mockito.when(itemService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createItem_ShouldReturnCreated() throws Exception {
        Item item = new Item(1L, "Test Item", "A test description", "NEW", "test@example.com");
        Mockito.when(itemService.save(any(Item.class))).thenReturn(item);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void getItemById_ShouldReturnItem() throws Exception {
        Item item = new Item(1L, "Test Item", "A test description", "NEW", "test@example.com");
        Mockito.when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void getItemById_ShouldReturnNotFound() throws Exception {
        Mockito.when(itemService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/999"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateItem_ShouldReturnUpdatedItem() throws Exception {
        Item item = new Item(1L, "Test Item", "A test description", "NEW", "test@example.com");
        Mockito.when(itemService.updateItem(eq(1L), any(Item.class)))
                .thenReturn(Optional.of(item));

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void updateItem_ShouldReturnNotFound() throws Exception {
        Item item = new Item(1L, "Test Item", "A test description", "NEW", "test@example.com");
        Mockito.when(itemService.updateItem(eq(1L), any(Item.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteItem_ShouldReturnNoContent() throws Exception {
        Mockito.doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_ShouldReturnNotFound() throws Exception {
        Mockito.doThrow(new NotFoundException("Item not found"))
                .when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void processItems_ShouldReturnProcessedItems() throws Exception {
        List<Item> processed = List.of(new Item(1L, "Test Item", "A test description", "PROCESSED", "test@example.com"));

        CompletableFuture<List<Item>> completedFuture = CompletableFuture.completedFuture(processed);
        Mockito.when(itemService.processItemsAsync()).thenReturn(completedFuture);

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }

    @Test
    void processItems_ShouldReturnError() throws Exception {
        CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new NotFoundException("Processing failed"));

        Mockito.when(itemService.processItemsAsync()).thenReturn(failedFuture);

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().is(500));
    }
}
