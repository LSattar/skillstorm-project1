package com.shelfsync.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shelfsync.dtos.InventoryHistoryDto;
import com.shelfsync.dtos.WarehouseItemDto;
import com.shelfsync.dtos.WarehouseItemResponse;
import com.shelfsync.services.WarehouseItemService;

import jakarta.validation.Valid;

/**
 * Controller for warehouse item operations.
 * 
 * <p>Note: Direct modification of warehouse items (CREATE, UPDATE, DELETE) is deprecated.
 * All inventory changes should be made through the {@link InventoryHistoryController}
 * to maintain proper audit trails and enforce business rules.
 */
@RestController
@RequestMapping("/warehouse-item")
public class WarehouseItemController {

    private final WarehouseItemService service;

    public WarehouseItemController(WarehouseItemService service) {
        this.service = service;
    }

    /**
     * Creates a new warehouse item.
     * 
     * @deprecated Use {@link InventoryHistoryController#create(InventoryHistoryDto)} instead.
     *             This endpoint bypasses inventory history tracking and business rule validation.
     *             All inventory changes should go through inventory history to maintain audit trails.
     * @param dto The warehouse item data transfer object
     * @return The created warehouse item response
     */
    @Deprecated
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

    /**
     * Updates a warehouse item's quantity.
     * 
     * @deprecated Use {@link InventoryHistoryController#create(InventoryHistoryDto)} instead.
     *             This endpoint bypasses inventory history tracking and business rule validation.
     *             All inventory changes should go through inventory history to maintain audit trails.
     * @param warehouseId The warehouse ID
     * @param itemId The item ID
     * @param dto The warehouse item data transfer object with updated quantity
     * @return The updated warehouse item response
     */
    @Deprecated
    @PutMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<WarehouseItemResponse> update(@PathVariable Integer warehouseId,
                                                   @PathVariable Integer itemId,
                                                   @RequestBody WarehouseItemDto dto) {
        WarehouseItemResponse updated = service.update(warehouseId, itemId, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a warehouse item.
     * 
     * @deprecated Warehouse items should not be deleted directly. If inventory needs to be removed,
     *             use {@link InventoryHistoryController#create(InventoryHistoryDto)} with an OUTBOUND
     *             transaction to properly record the removal in inventory history.
     * @param warehouseId The warehouse ID
     * @param itemId The item ID
     * @return No content response
     */
    @Deprecated
    @DeleteMapping("/{warehouseId}/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable Integer warehouseId,
                                       @PathVariable Integer itemId) {
        service.deleteById(warehouseId, itemId);
        return ResponseEntity.noContent().build();
    }
}
