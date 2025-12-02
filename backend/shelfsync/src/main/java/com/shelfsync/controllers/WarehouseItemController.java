package com.shelfsync.controllers;

import com.shelfsync.dtos.WarehouseItemDto;
import com.shelfsync.dtos.WarehouseItemResponse;
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
    public ResponseEntity<WarehouseItemResponse> create(@Valid @RequestBody WarehouseItemDto dto) {
        WarehouseItemResponse created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<WarehouseItemResponse> getById(@PathVariable Integer warehouseId,
                                                    @PathVariable Integer itemId) {
        WarehouseItemResponse dto = service.findById(warehouseId, itemId);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<WarehouseItemResponse>> getAll() {
        List<WarehouseItemResponse> all = service.findAll();
        return ResponseEntity.ok(all);
    }

    // UPDATE
    @PutMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<WarehouseItemResponse> update(@PathVariable Integer warehouseId,
                                                   @PathVariable Integer itemId,
                                                   @RequestBody WarehouseItemDto dto) {
        WarehouseItemResponse updated = service.update(warehouseId, itemId, dto);
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
