package com.shelfsync.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.ItemInventorySummaryResponse;
import com.shelfsync.dtos.ItemWarehouseQuantity;
import com.shelfsync.dtos.WarehouseItemDto;
import com.shelfsync.dtos.WarehouseItemResponse;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Item;
import com.shelfsync.models.Warehouse;
import com.shelfsync.models.WarehouseItem;
import com.shelfsync.models.WarehouseItemKey;
import com.shelfsync.repositories.ItemRepository;
import com.shelfsync.repositories.WarehouseItemRepository;
import com.shelfsync.repositories.WarehouseRepository;

import jakarta.transaction.Transactional;

@Service
public class WarehouseItemService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseItemService.class);

    private final WarehouseItemRepository repo;
    private final WarehouseRepository warehouseRepo;
    private final ItemRepository itemRepo;

    public WarehouseItemService(WarehouseItemRepository repo,
                                WarehouseRepository warehouseRepo,
                                ItemRepository itemRepo) {
        this.repo = repo;
        this.warehouseRepo = warehouseRepo;
        this.itemRepo = itemRepo;
    }

    private WarehouseItemKey buildKey(Integer warehouseId, Integer itemId) {
        WarehouseItemKey key = new WarehouseItemKey();
        key.setWarehouseId(warehouseId);
        key.setItemId(itemId);
        return key;
    }

    private WarehouseItemDto toDto(WarehouseItem wi) {
        return new WarehouseItemDto(
                wi.getWarehouse().getWarehouseId(),
                wi.getItem().getItemId(),
                wi.getQuantity()
        );
    }
    
    private WarehouseItemResponse toResponse(WarehouseItem wi) {
        Warehouse w = wi.getWarehouse();
        Item i = wi.getItem();

        return new WarehouseItemResponse(
                w.getWarehouseId(),
                w.getName(),
                w.getAddress(),
                w.getCity(),
                w.getState(),
                w.getZip(),
                i,               
                wi.getQuantity()
        );
    }

    private Warehouse resolveWarehouse(Integer warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("warehouseId is required");
        }
        return warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
    }

    private Item resolveItem(Integer itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId is required");
        }
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
    }
    
    /**
     * Checks if adding the specified quantity of an item would exceed the warehouse's capacity.
     * Only checks when item quantity is being changed.
     * 
     * @param warehouse The warehouse to check capacity for
     * @param item The item being added
     * @param delta The quantity change
     * @throws IllegalArgumentException if adding the items would exceed warehouse capacity
     */
    private void checkCapacity(Warehouse warehouse, Item item, int delta) {
        if (delta <= 0) {
            return;
        }
        
        BigDecimal maxCapacity = warehouse.getMaximumCapacityCubicFeet();
        if (maxCapacity == null) {
            log.warn("Warehouse {} has null maximum capacity, skipping capacity check", warehouse.getWarehouseId());
            return;
        }
        
        BigDecimal currentUsed = repo.findUsedCapacityCubicFeet(warehouse.getWarehouseId());
        if (currentUsed == null) {
            currentUsed = BigDecimal.ZERO;
        }

        BigDecimal itemCubicFeet = item.getCubicFeet();
        if (itemCubicFeet == null) {
            log.warn("Item {} has null cubicFeet, skipping capacity check", item.getItemId());
            return;
        }

        BigDecimal additionalCapacityNeeded = itemCubicFeet.multiply(BigDecimal.valueOf(delta));
        
        BigDecimal newUsedCapacity = currentUsed.add(additionalCapacityNeeded);
        
        if (newUsedCapacity.compareTo(maxCapacity) > 0) {
            log.warn("Attempted to exceed warehouse capacity: warehouseId={} itemId={} currentUsed={} maxCapacity={} additionalNeeded={} newTotal={}",
                    warehouse.getWarehouseId(), item.getItemId(), currentUsed, maxCapacity, 
                    additionalCapacityNeeded, newUsedCapacity);
            
            BigDecimal available = maxCapacity.subtract(currentUsed);
            throw new IllegalArgumentException(
                String.format("Adding this quantity would exceed warehouse capacity. Available space: %.2f cubic feet, needed: %.2f cubic feet",
                    available.doubleValue(), additionalCapacityNeeded.doubleValue())
            );
        }
    }
    
    @Transactional
    public WarehouseItemResponse applyQuantityChange(Integer warehouseId, Integer itemId, int delta) {
        Warehouse warehouse = resolveWarehouse(warehouseId);
        Item item = resolveItem(itemId);

        WarehouseItemKey key = buildKey(warehouseId, itemId);

        WarehouseItem wi = repo.findById(key).orElse(null);

        if (wi == null) {
            if (delta < 0) {
                // Log full details for debugging
                log.warn("Attempted to reduce quantity below zero for non-existent item: warehouseId={} itemId={} delta={}",
                        warehouseId, itemId, delta);
                // Throw user-friendly message without IDs
                throw new IllegalArgumentException("Cannot reduce quantity below zero for a non-existent item");
            }
            
            // Check capacity before creating new warehouse item
            checkCapacity(warehouse, item, delta);
            
            wi = new WarehouseItem();
            wi.setId(key);
            wi.setWarehouse(warehouse);
            wi.setItem(item);
            wi.setQuantity(delta);
        } else {
            int newQty = wi.getQuantity() + delta;
            if (newQty < 0) {
                // Log full details for debugging
                log.warn("Attempted to reduce quantity below zero: warehouseId={} itemId={} currentQty={} delta={}",
                        warehouseId, itemId, wi.getQuantity(), delta);
                // Throw user-friendly message without IDs
                throw new IllegalArgumentException("Resulting quantity would be negative");
            }
            
            // Check capacity before updating quantity (only if adding items)
            checkCapacity(warehouse, item, delta);
            
            wi.setQuantity(newQty);
        }

        WarehouseItem saved = repo.save(wi);
        log.info("Adjusted WarehouseItem warehouseId={} itemId={} delta={} newQty={}",
                warehouseId, itemId, delta, saved.getQuantity());

        return toResponse(saved);
    }

 // CREATE
    public WarehouseItemResponse create(WarehouseItemDto dto) {
        log.debug("Creating WarehouseItem: warehouseId={} itemId={} qty={}",
                dto.warehouseId(), dto.itemId(), dto.quantity());

        Warehouse warehouse = resolveWarehouse(dto.warehouseId());
        Item item = resolveItem(dto.itemId());

        WarehouseItemKey key = buildKey(dto.warehouseId(), dto.itemId());
        WarehouseItem entity = new WarehouseItem();
        entity.setId(key);
        entity.setWarehouse(warehouse);
        entity.setItem(item);
        entity.setQuantity(dto.quantity());

        WarehouseItem saved = repo.save(entity);
        log.info("Created WarehouseItem warehouseId={} itemId={} qty={}",
                saved.getWarehouse().getWarehouseId(),
                saved.getItem().getItemId(),
                saved.getQuantity());

        return toResponse(saved);
    }

    // READ ALL
    public List<WarehouseItemResponse> findAll() {
        log.debug("Fetching all WarehouseItems");
        List<WarehouseItem> all = repo.findAll();
        log.info("Fetched {} WarehouseItems", all.size());
        return all.stream()
                .map(this::toResponse)
                .toList();
    }

    // READ ONE
    public WarehouseItemResponse findById(Integer warehouseId, Integer itemId) {
        log.debug("Fetching WarehouseItem warehouseId={} itemId={}", warehouseId, itemId);

        WarehouseItemKey key = buildKey(warehouseId, itemId);
        WarehouseItem entity = repo.findById(key)
                .orElseThrow(() -> {
                    log.warn("WarehouseItem not found for warehouseId={} itemId={}", warehouseId, itemId);
                    return new ResourceNotFoundException(
                            "WarehouseItem not found for warehouseId=" + warehouseId + " itemId=" + itemId);
                });

        log.info("Found WarehouseItem warehouseId={} itemId={} qty={}",
                entity.getWarehouse().getWarehouseId(),
                entity.getItem().getItemId(),
                entity.getQuantity());

        return toResponse(entity);
    }
    
    public java.util.List<ItemInventorySummaryResponse> searchInventoryByItem(String q) {
        if (q == null || q.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query q is required");
        }

        String trimmed = q.trim();

        List<WarehouseItem> results = repo.searchInventoryByItem(trimmed);

        // group by Item
        Map<Item, List<WarehouseItem>> byItem = results.stream()
                .collect(Collectors.groupingBy(WarehouseItem::getItem));

        return byItem.entrySet().stream()
                .map(entry -> {
                    Item item = entry.getKey();
                    List<WarehouseItem> wis = entry.getValue();

                    int total = wis.stream()
                            .map(WarehouseItem::getQuantity)
                            .filter(Objects::nonNull)
                            .mapToInt(Integer::intValue)
                            .sum();

                    List<ItemWarehouseQuantity> locations = wis.stream()
                            .map(wi -> new ItemWarehouseQuantity(
                                    wi.getWarehouse().getWarehouseId(),
                                    wi.getWarehouse().getName(),
                                    wi.getQuantity()
                            ))
                            .toList();

                    return new ItemInventorySummaryResponse(
                            item.getItemId(),
                            item.getSku(),
                            item.getGameTitle(),
                            total,
                            locations
                    );
                })
                .toList();
    }

    // UPDATE (quantity)
    public WarehouseItemResponse update(Integer warehouseId, Integer itemId, WarehouseItemDto dto) {
        log.debug("Updating WarehouseItem warehouseId={} itemId={} newQty={}",
                warehouseId, itemId, dto.quantity());

        WarehouseItemKey key = buildKey(warehouseId, itemId);
        WarehouseItem existing = repo.findById(key)
                .orElseThrow(() -> {
                    log.warn("Cannot update: WarehouseItem not found for warehouseId={} itemId={}", warehouseId, itemId);
                    return new ResourceNotFoundException(
                            "WarehouseItem not found for warehouseId=" + warehouseId + " itemId=" + itemId);
                });

        existing.setQuantity(dto.quantity());

        WarehouseItem saved = repo.save(existing);
        log.info("Updated WarehouseItem warehouseId={} itemId={} qty={}",
                saved.getWarehouse().getWarehouseId(),
                saved.getItem().getItemId(),
                saved.getQuantity());

        return toResponse(saved);
    }

    // DELETE
    public void deleteById(Integer warehouseId, Integer itemId) {
        log.debug("Deleting WarehouseItem warehouseId={} itemId={}", warehouseId, itemId);

        WarehouseItemKey key = buildKey(warehouseId, itemId);
        WarehouseItem existing = repo.findById(key)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: WarehouseItem not found for warehouseId={} itemId={}", warehouseId, itemId);
                    return new ResourceNotFoundException(
                            "WarehouseItem not found for warehouseId=" + warehouseId + " itemId=" + itemId);
                });

        repo.delete(existing);
        log.info("Deleted WarehouseItem warehouseId={} itemId={}", warehouseId, itemId);
    }
}
