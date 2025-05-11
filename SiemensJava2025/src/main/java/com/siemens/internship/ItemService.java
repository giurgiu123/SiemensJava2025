package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();
   // private int processedCount = 0;
   private final AtomicInteger processedCount = new AtomicInteger(); // asta e un counter thread safe


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return itemRepository.existsById(id);
    }

    /**
     * Your Tasks
     * TODO Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * TODO Proper error handling and propagation
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
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() { //From hints

        List<Long> itemIds = itemRepository.findAllIds();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        List<Item> safeProcessedItems = new CopyOnWriteArrayList<>();

        for (Long id : itemIds) {
           // CompletableFuture.runAsync(() -> {// TODO asta ne ajuta la paralelizare - pana la urma motivul folosirii threadurilor
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(100);
                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isPresent()) {
                        Item item = optionalItem.get();
                        item.setStatus("PROCESSED");
                        itemRepository.save(item);
                        safeProcessedItems.add(item);// TODO SAFE
                        processedCount.incrementAndGet();
                      //  return;
                    }
               //     processedCount++; //TODO AICI NU E THREAD SAFE TREBUIE SA O DECLARAM CA VARIABILA ATOMICA CARE ORI SE EXECUTA COMPLET ORI NU!

                   // item.setStatus("PROCESSED");
                   // itemRepository.save(item);
                   // processedItems.add(item);//TODO AICI IAR NU E THREAD SAFE

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // ✅ corect
                } catch (Exception e) {
                    // poti loga sau salva excepția aici
                    System.out.println("Unexpected error: " + e.getMessage());
                }
            });
            futures.add(future);
        }
        // asteptam sa se termine toate task urile
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        return allDone.thenApply(ignored -> {
            System.out.println("Total processed: " + processedCount.get());
            return safeProcessedItems;
        });

   //     return processedItems; //TODO EXISTA PODIBILATETEA SA NU FIE GATA TASKURILE
    }
}



