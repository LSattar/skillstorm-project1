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

/**
 * Service for managing warehouse inventory operations.
 * 
 * <p>This service handles the relationship between warehouses and items, including:
 * <ul>
 *   <li>Quantity changes (inbound, outbound, and transfer operations)</li>
 *   <li>Capacity validation to prevent exceeding warehouse maximum capacity</li>
 *   <li>Quantity validation to prevent negative quantities</li>
 *   <li>Inventory search and aggregation across warehouses</li>
 * </ul>
 * 
 * <p>Key business rules enforced:
 * <ul>
 *   <li>Quantities cannot be reduced below zero</li>
 *   <li>Adding items cannot exceed warehouse capacity (calculated as item quantity × cubic feet)</li>
 *   <li>All quantity changes are transactional to ensure data consistency</li>
 * </ul>
 * 
 */
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
     * Validates that adding the specified quantity of an item will not exceed the warehouse's capacity.
     * 
     * <p>This method enforces the business rule that warehouse capacity cannot be exceeded.
     * Capacity is calculated as: (item quantity) × (item cubic feet per unit).
     * 
     * <p>Only performs validation when adding items (delta > 0). Removing items does not
     * require capacity validation.
     * 
     * <p>If capacity would be exceeded, throws an IllegalArgumentException with a user-friendly
     * message indicating available space and space needed. Full details are logged for debugging.
     * 
     * @param warehouse The warehouse to check capacity for
     * @param item The item being added (must have a valid cubicFeet value)
     * @param delta The quantity change (must be positive for capacity check to occur)
     * @throws IllegalArgumentException if adding the items would exceed warehouse maximum capacity.
     *         The exception message includes available space and space needed in cubic feet.
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
    
    /**
     * Applies a quantity change to a warehouse item (inbound, outbound, or transfer operation).
     * 
     * <p>This is the primary method for recording inventory transactions. It handles:
     * <ul>
     *   <li>Creating new warehouse items when they don't exist (for inbound operations)</li>
     *   <li>Updating existing warehouse item quantities</li>
     *   <li>Validating that quantities don't go negative</li>
     *   <li>Validating that capacity isn't exceeded when adding items</li>
     * </ul>
     * 
     * <p>Business rules enforced:
     * <ul>
     *   <li>Cannot reduce quantity below zero (throws IllegalArgumentException)</li>
     *   <li>Cannot add items that would exceed warehouse capacity (throws IllegalArgumentException)</li>
     *   <li>Cannot reduce quantity for non-existent items (throws IllegalArgumentException)</li>
     * </ul>
     * 
     * <p>This method is transactional to ensure data consistency. If any validation fails,
     * the entire operation is rolled back.
     * 
     * @param warehouseId The ID of the warehouse where the quantity change occurs
     * @param itemId The ID of the item whose quantity is changing
     * @param delta The quantity change: positive for adding items (inbound), 
     *              negative for removing items (outbound), or positive/negative for transfers
     * @return The updated warehouse item response with current quantity
     * @throws IllegalArgumentException if the operation would result in negative quantity 
     *         or exceed warehouse capacity. The message is user-friendly and does not expose IDs.
     * @throws ResourceNotFoundException if the warehouse or item does not exist
     */
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

    /**
     * Creates a new warehouse item with the specified initial quantity.
     * 
     * <p>This method creates a new relationship between a warehouse and an item.
     * Note: This method does not perform capacity validation. For adding inventory,
     * use {@link #applyQuantityChange(Integer, Integer, int)} which includes capacity checks.
     * 
     * @param dto The warehouse item data transfer object containing warehouseId, itemId, and quantity
     * @return The created warehouse item response
     * @throws ResourceNotFoundException if the warehouse or item does not exist
     */
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

    /**
     * Retrieves all warehouse items across all warehouses.
     * 
     * @return A list of all warehouse items with their warehouse and item details
     */
    public List<WarehouseItemResponse> findAll() {
        log.debug("Fetching all WarehouseItems");
        List<WarehouseItem> all = repo.findAll();
        log.info("Fetched {} WarehouseItems", all.size());
        return all.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves a specific warehouse item by warehouse ID and item ID.
     * 
     * @param warehouseId The warehouse ID
     * @param itemId The item ID
     * @return The warehouse item response
     * @throws ResourceNotFoundException if the warehouse item does not exist
     */
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
    
    /**
     * Searches for inventory items by game title or SKU and aggregates quantities across all warehouses.
     * 
     * <p>This method performs a case-insensitive search on item game titles and SKUs.
     * Results are grouped by item and include:
     * <ul>
     *   <li>Total quantity across all warehouses</li>
     *   <li>Breakdown of quantities by warehouse location</li>
     * </ul>
     * 
     * <p>The search query is trimmed and must not be empty. Partial matches are supported
     * (e.g., searching "Mario" will find "Super Mario Bros").
     * 
     * @param q The search query string (game title or SKU). Must not be null or empty.
     * @return A list of inventory summary responses, one per matching item, with aggregated quantities
     * @throws IllegalArgumentException if the search query is null or empty
     */
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

    /**
     * Updates the quantity of an existing warehouse item.
     * 
     * <p>This method sets the quantity to an absolute value. For relative quantity changes
     * (increment/decrement), use {@link #applyQuantityChange(Integer, Integer, int)} instead,
     * which includes capacity and negative quantity validation.
     * 
     * <p>Note: This method does not validate capacity constraints. It simply sets the quantity
     * to the specified value. Use with caution.
     * 
     * @param warehouseId The warehouse ID
     * @param itemId The item ID
     * @param dto The warehouse item DTO containing the new quantity value
     * @return The updated warehouse item response
     * @throws ResourceNotFoundException if the warehouse item does not exist
     */
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

    /**
     * Deletes a warehouse item, removing the relationship between a warehouse and an item.
     * 
     * <p>This operation permanently removes the warehouse item record. Any inventory history
     * records associated with this warehouse item are not affected by this deletion.
     * 
     * @param warehouseId The warehouse ID
     * @param itemId The item ID
     * @throws ResourceNotFoundException if the warehouse item does not exist
     */
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
