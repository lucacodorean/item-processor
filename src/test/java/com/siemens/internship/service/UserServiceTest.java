package com.siemens.internship.service;


import com.siemens.internship.controller.ItemController;
import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebMvcTest(UserServiceTest.class)
@EnableAsync
class UserServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindAll() {
        List<Item> items = List.of(
                new Item(1L, "Item1", "desc", "NEW", "email@test.com"),
                new Item(2L, "Item2", "desc", "NEW", "email2@test.com")
        );

        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();

        assertEquals(2, result.size());
        verify(itemRepository).findAll();
    }

    @Test
    void testFindById_found() throws NotFoundException {
        Item item = new Item(2L, "Item2", "desc", "NEW", "email2@test.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(1L);

        assertTrue(result.isPresent());
        verify(itemRepository).findById(1L);
    }

    @Test
    void testFindById_notFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> itemService.findById(1L));
    }

    @Test
    void testSave() {
        Item item = new Item();
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.save(item);

        assertNotNull(result);
        verify(itemRepository).save(item);
    }

    @Test
    void testUpdateItem_found() throws NotFoundException {
        Item existing = new Item(1L, "old", "desc", "NEW", "old@mail.com");
        Item update = new Item(null, "new", null, "PROCESSED", null);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        Optional<Item> result = itemService.updateItem(1L, update);

        assertTrue(result.isPresent());
        assertEquals("new", result.get().getName());
        assertEquals("PROCESSED", result.get().getStatus());
    }

    @Test
    void testUpdateItem_notFound() throws NotFoundException {
        Item update = new Item(2L, "Item2", "desc", "NEW", "email2@test.com");
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.updateItem(1L, update);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteById_success() throws NotFoundException {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(new Item()));

        itemService.deleteById(1L);
        verify(itemRepository).deleteById(1L);
    }

    @Test
    void testDeleteById_notFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> itemService.deleteById(1L));
    }
}