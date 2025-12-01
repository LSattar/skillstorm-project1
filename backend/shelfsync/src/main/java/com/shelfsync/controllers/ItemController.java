package com.shelfsync.controllers;

import com.shelfsync.dtos.ItemDto;
import com.shelfsync.services.ItemService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/item")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<ItemDto> create(@Valid @RequestBody ItemDto dto) {
        ItemDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getById(@PathVariable Integer id) {
        ItemDto dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<ItemDto>> getAll() {
        List<ItemDto> items = service.findAllItems();
        return ResponseEntity.ok(items);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> update(@PathVariable Integer id,
                                          @RequestBody ItemDto dto) {
        ItemDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
