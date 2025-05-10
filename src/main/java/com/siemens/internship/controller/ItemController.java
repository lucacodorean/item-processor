package com.siemens.internship.controller;

import com.siemens.internship.exception.NotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        // Items list should be saved in a variable so we reduce the number of calls to .findAll() method,
        // and return the status code according to the list's content.

        List<Item> items = itemService.findAll();
        if(items.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

        return new ResponseEntity<>(items, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item, BindingResult result) {
        ///  Proper status code for creation is 201, not 200, so CREATED will be used instead of OK.

        if (result.hasErrors()) return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        /// Here I set the returned status to NOT_FOUND because the resource doesn't exist in the database if the
        /// findById method fails.
        try {
            Optional<Item> item = itemService.findById(id);
            if(item.isPresent()) return new ResponseEntity<>(item.get(), HttpStatus.OK);
        }
        catch (NotFoundException e) { return new ResponseEntity<>(HttpStatus.NOT_FOUND); }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        // The problem here was that in first instance, the item with the given id wasn't updated. That issue has been
        // fixed. Also, because this code intends to follow the Layer-Architecture, the update logic belongs to the
        // ItemService class. Thus, the updateItem() method has been implemented.
        // Given that the updateItem() method returns an Optional object, we should handle both cases.
        if(result.hasErrors()) return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        try {
            Optional<Item> updatedItem = itemService.updateItem(id, item);
            if(updatedItem.isPresent()) return new ResponseEntity<>(updatedItem.get(), HttpStatus.OK);
        } catch (NotFoundException e) { return new ResponseEntity<>(HttpStatus.NOT_FOUND); }

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        // NO_CONTENT should be returned if the operation succeeds. Also, it's good approach to evaluate the existence
        // of a resource with the given ID first. It's pointless to delete something that doesn't exist. This logic
        // is implemented in the service class.
        try {
            itemService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException ex) { return new ResponseEntity<>(HttpStatus.NOT_FOUND); }
    }

    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() {
        try {
            List<Item> processedItems = itemService.processItemsAsync().get();
            if (processedItems.isEmpty()) return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            return new ResponseEntity<>(processedItems, HttpStatus.OK);
        } catch (InterruptedException | ExecutionException e) { return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); }
    }
}
