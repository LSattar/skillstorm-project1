package com.shelfsync.controllers;

import com.shelfsync.dtos.WarehouseDto;
import com.shelfsync.services.WarehouseService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService service;

    public WarehouseController(WarehouseService service) {
        this.service = service;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<WarehouseDto> create(@Valid @RequestBody WarehouseDto dto) {
        WarehouseDto created = service.create(dto);
        return ResponseEntity.ok(created);
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getById(@PathVariable Integer id) {
        WarehouseDto dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getAll() {
        List<WarehouseDto> warehouses = service.findAllWarehouses();
        return ResponseEntity.ok(warehouses);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseDto> update(@PathVariable Integer id,
                                               @RequestBody WarehouseDto dto) {
        WarehouseDto updated = service.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
