package com.shelfsync.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shelfsync.dtos.ItemInventorySummaryResponse;
import com.shelfsync.services.WarehouseItemService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final WarehouseItemService warehouseItemService;

    public InventoryController(WarehouseItemService warehouseItemService) {
        this.warehouseItemService = warehouseItemService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemInventorySummaryResponse>> search(
            @RequestParam("q") String q
    ) {
        List<ItemInventorySummaryResponse> results = warehouseItemService.searchInventoryByItem(q);
        return ResponseEntity.ok(results);
    }
}