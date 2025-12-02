package com.shelfsync.controllers;

import com.shelfsync.dtos.InventoryHistoryDto;
import com.shelfsync.services.InventoryHistoryService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/inventory-history")
public class InventoryHistoryController {

    private final InventoryHistoryService service;

    public InventoryHistoryController(InventoryHistoryService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<InventoryHistoryDto> create(@Valid @RequestBody InventoryHistoryDto dto) {
        InventoryHistoryDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<InventoryHistoryDto> getById(@PathVariable Integer id) {
        InventoryHistoryDto dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<InventoryHistoryDto>> getAll() {
        List<InventoryHistoryDto> history = service.findAll();
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<InventoryHistoryDto>> getHistoryForWarehouse(
            @PathVariable Integer warehouseId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime end
    ) {
        List<InventoryHistoryDto> history =
                service.findByWarehouseAndDateRange(warehouseId, start, end);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<InventoryHistoryDto>> getRecentActivities() {
        List<InventoryHistoryDto> history = service.findRecentActivities();
        return ResponseEntity.ok(history);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<InventoryHistoryDto> update(@PathVariable Integer id,
                                                      @RequestBody InventoryHistoryDto dto) {
        InventoryHistoryDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
