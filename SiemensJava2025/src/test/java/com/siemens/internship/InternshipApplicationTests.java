package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.dao.DataAccessException;

import java.util.Arrays;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class InternshipApplicationTests {
	@Mock
	private ItemRepository itemRepository;

	@InjectMocks
	private ItemService itemService;

	@Test
	void processItemsAsync_ShouldProcessAllItems() throws ExecutionException, InterruptedException {
		//setare
		List<Long> itemIds = Arrays.asList(1L, 2L);
		Item item1 = new Item(1L, "Item1", "Desc1", "NEW", "test1@example.com");
		Item item2 = new Item(2L, "Item2", "Desc2", "NEW", "test2@example.com");

		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
		when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
		when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

		//execut
		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		//verifica
		assertEquals(2, result.size());
		assertEquals("PROCESSED", result.get(0).getStatus());
		assertEquals("PROCESSED", result.get(1).getStatus());
	}

	@Test
	void processItemsAsync_ShouldHandleErrorsGracefully() throws ExecutionException, InterruptedException {
		//setup
		List<Long> itemIds = Arrays.asList(1L, 2L);
		Item item1 = new Item(1L, "Item1", "Desc1", "NEW", "test1@example.com");

		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
		when(itemRepository.findById(2L)).thenThrow(new DataAccessException("Database error") {});
		when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

		//execute
		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		//verify
		assertEquals(1, result.size());
		assertEquals("PROCESSED", result.get(0).getStatus());
	}
}
