package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableAsync
class AsyncOperationTest {

	@InjectMocks
	private ItemService itemService;

	@Mock
	private ItemRepository itemRepository;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testProcessItemsAsync_success() throws Exception {

		List<Long> itemIds = List.of(1L, 2L);
		Item item1 = new Item(1L, "Item1", "desc", "NEW", "email@test.com");
		Item item2 = new Item(2L, "Item2", "desc", "NEW", "email2@test.com");

		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
		when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
		when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		assertEquals(2, result.size());
		assertTrue(result.stream().allMatch(item -> item.getStatus().equals("PROCESSED")));
	}

	@Test
	void testProcessItemsAsync_itemNotFound() throws Exception {

		List<Long> itemIds = List.of(1L, 2L);
		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(new Item()));
		when(itemRepository.findById(2L)).thenReturn(Optional.empty());
		when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		assertEquals(1, result.size());
	}

}
