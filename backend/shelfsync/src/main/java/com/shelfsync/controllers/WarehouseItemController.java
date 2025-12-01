package com.shelfsync.controllers;

import com.shelfsync.dtos.WarehouseItemDto;
import com.shelfsync.services.WarehouseItemService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse-item")
public class WarehouseItemController {

    private final WarehouseItemService service;

    public WarehouseItemController(WarehouseItemService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<WarehouseItemDto> create(@Valid @RequestBody WarehouseItemDto dto) {
        WarehouseItemDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<WarehouseItemDto> getById(@PathVariable Integer warehouseId,
                                                    @PathVariable Integer itemId) {
        WarehouseItemDto dto = service.findById(warehouseId, itemId);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<WarehouseItemDto>> getAll() {
        List<WarehouseItemDto> all = service.findAll();
        return ResponseEntity.ok(all);
    }

    // UPDATE
    @PutMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<WarehouseItemDto> update(@PathVariable Integer warehouseId,
                                                   @PathVariable Integer itemId,
                                                   @RequestBody WarehouseItemDto dto) {
        WarehouseItemDto updated = service.update(warehouseId, itemId, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Integer warehouseId,
                                       @PathVariable Integer itemId) {
        service.deleteById(warehouseId, itemId);
        return ResponseEntity.noContent().build();
    }
}
