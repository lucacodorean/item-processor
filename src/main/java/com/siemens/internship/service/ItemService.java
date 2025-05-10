package com.siemens.internship.service;

import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems       = Collections.synchronizedList(new ArrayList<>());
    private AtomicInteger processedCount    = new AtomicInteger(0);


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) throws NotFoundException {
        return Optional.ofNullable(
            itemRepository
                    .findById(id)
                    .orElseThrow(() -> new NotFoundException("Item not found at the given id.")
            ));
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }


    // In this case, Transactional makes sure that in case of any errors, the data is not affected.
    @Transactional
    public Optional<Item> updateItem(Long id, Item item) throws NotFoundException {
        return itemRepository.findById(id).map(existingItem -> {
            if (item.getName() != null)         existingItem.setName(item.getName());
            if (item.getDescription() != null)  existingItem.setDescription(item.getDescription());
            if (item.getStatus() != null)       existingItem.setStatus(item.getStatus());
            if (item.getEmail() != null)        existingItem.setEmail(item.getEmail());

            return itemRepository.save(existingItem);
        });
    }

    public void deleteById(Long id) throws  NotFoundException {
        if(this.findById(id).isEmpty()) throw new NotFoundException("No item found at the given id.");
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    /*
        Initial wrong-doings.
        First of all, it can be noticed that there is access to an attribute that isn't shared accordingly between
        the Async resources, and thi is the processCount attribute. This should be made atomic. Given that we're working
        with threads, the possibility of two threads attempting to modify the same resource at the same time is huge. Thus,
        the probability of lacking / losing information  is increased. It hoes the same for the processItems list. In short lines,
        race conditions may occur. The class attributes' initializations and declarations have been modified accordingly.

        Secondly, the method doesn't wait until all threads finished their processing. Thus, lose of data may happen.
        Besides this, there may appear some errors with the .save and .findById methods. From my understanding, this
        issue is solved by the @Async annotation. The concurrent updates can be solved by using the @Version annotation
        in the model.

        Based on the hint of using ComparableFuture and also on Spring's mechanism of using Thread pools,
        we can simply delay the executor instead of using .sleep() method.

        The if item == null check can be eliminated and solved by using the .orElseThrow()
        of the Optional .findById() method.
        In return, I decided to use CompletableFuture.allOf() to make sure that all futures threads have been finished,
        and the result found in processedItems is correct. Now, in controller the endpoint should be modified so it handles
        a CompletableFuture correctly.
     */

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        itemIds.forEach(id -> {
            CompletableFuture<Void> currentFuture = CompletableFuture.runAsync(() -> {
                try {
                    Item item = itemRepository.findById(id)
                            .orElseThrow(() -> new NotFoundException("Item with given id can't be found."));

                    item.setStatus("PROCESSED");
                    processedCount.incrementAndGet();
                    processedItems.add(item);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS, executor));
            futures.add(currentFuture);
        });

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(voidResult -> processedItems)
                .exceptionally(ex -> Collections.emptyList());
    }
}

