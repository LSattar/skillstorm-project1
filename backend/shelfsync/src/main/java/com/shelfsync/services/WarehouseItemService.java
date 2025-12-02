package com.shelfsync.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.WarehouseItemDto;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Item;
import com.shelfsync.models.Warehouse;
import com.shelfsync.models.WarehouseItem;
import com.shelfsync.dtos.WarehouseItemResponse;
import com.shelfsync.models.WarehouseItemKey;
import com.shelfsync.repositories.ItemRepository;
import com.shelfsync.repositories.WarehouseItemRepository;
import com.shelfsync.repositories.WarehouseRepository;

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
        return new WarehouseItemResponse(
                wi.getWarehouse(),
                wi.getItem(),
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
