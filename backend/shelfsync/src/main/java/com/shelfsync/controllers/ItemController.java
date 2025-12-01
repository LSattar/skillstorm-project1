package com.shelfsync.controllers;

import com.shelfsync.dtos.ItemDto;
import com.shelfsync.dtos.ItemResponseDto;
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
    public ResponseEntity<ItemResponseDto> create(@Valid @RequestBody ItemDto dto) {
        ItemResponseDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getById(@PathVariable Integer id) {
        ItemResponseDto dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<ItemResponseDto>> getAll() {
        List<ItemResponseDto> items = service.findAllItems();
        return ResponseEntity.ok(items);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ItemResponseDto> update(@PathVariable Integer id,
                                          @RequestBody ItemDto dto) {
        ItemResponseDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
