package com.shelfsync.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.ItemDto;
import com.shelfsync.exceptions.ResourceConflictException;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Category;
import com.shelfsync.models.Company;
import com.shelfsync.models.Item;
import com.shelfsync.repositories.CategoryRepository;
import com.shelfsync.repositories.CompanyRepository;
import com.shelfsync.repositories.InventoryHistoryRepository;
import com.shelfsync.repositories.ItemRepository;
import com.shelfsync.repositories.WarehouseItemRepository;

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository repo;
    private final CategoryRepository categoryRepo;
    private final CompanyRepository companyRepo;
    private final WarehouseItemRepository warehouseItemRepo;
    private final InventoryHistoryRepository inventoryHistoryRepo;

    public ItemService(ItemRepository repo,
                       CategoryRepository categoryRepo,
                       CompanyRepository companyRepo,
                       WarehouseItemRepository warehouseItemRepo,
                       InventoryHistoryRepository inventoryHistoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
        this.companyRepo = companyRepo;
        this.warehouseItemRepo = warehouseItemRepo;
        this.inventoryHistoryRepo = inventoryHistoryRepo;
    }

    private ItemDto toDto(Item item) {
        Integer categoryId = item.getCategory() != null
                ? item.getCategory().getCategoryId()
                : null;

        Integer companyId = item.getCompany() != null
                ? item.getCompany().getCompanyId()
                : null;

        return new ItemDto(
                item.getItemId(),
                item.getSku(),
                item.getGameTitle(),
                categoryId,
                companyId,
                item.getWeightLbs(),
                item.getCubicFeet()
        );
    }

    private Category resolveCategory(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    private Company resolveCompany(Integer companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepo.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
    }

    // CREATE
    public ItemDto create(ItemDto dto) {
        log.debug("Request to create Item with sku='{}', title='{}'",
                dto.sku(), dto.gameTitle());

        if (repo.existsBySku(dto.sku())) {
            log.warn("Conflict creating Item: sku='{}' already exists", dto.sku());
            throw new ResourceConflictException("SKU must be unique");
        }

        Category category = resolveCategory(dto.categoryId());
        Company company = resolveCompany(dto.companyId());

        Item item = new Item();
        item.setSku(dto.sku());
        item.setGameTitle(dto.gameTitle());
        item.setCategory(category);
        item.setCompany(company);
        item.setWeightLbs(dto.weightLbs());
        item.setCubicFeet(dto.cubicFeet());

        Item saved = repo.save(item);
        log.info("Created Item id={} sku='{}' title='{}'",
                saved.getItemId(), saved.getSku(), saved.getGameTitle());

        return toDto(saved);
    }

    // READ ALL
    public List<ItemDto> findAllItems() {
        log.debug("Fetching all Items");
        List<Item> items = repo.findAll();
        log.info("Fetched {} Items", items.size());
        return items.stream().map(this::toDto).toList();
    }

    // READ ONE
    public ItemDto findById(Integer id) {
        log.debug("Fetching Item by id={}", id);
        Item item = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Item not found for id={}", id);
                    return new ResourceNotFoundException("Item not found: " + id);
                });

        log.info("Found Item id={} sku='{}' title='{}'",
                item.getItemId(), item.getSku(), item.getGameTitle());
        return toDto(item);
    }

    // UPDATE
    public ItemDto update(Integer id, ItemDto dto) {
        log.debug("Updating Item id={} with sku='{}', title='{}'",
                id, dto.sku(), dto.gameTitle());

        Item existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: Item not found for id={}", id);
                    return new ResourceNotFoundException("Item not found: " + id);
                });

        // SKU uniqueness on update
        if (dto.sku() != null &&
            !dto.sku().equals(existing.getSku()) &&
            repo.existsBySku(dto.sku())) {
            log.warn("Cannot update Item id={}: sku='{}' already in use", id, dto.sku());
            throw new ResourceConflictException("SKU must be unique");
        }

        Category category = resolveCategory(dto.categoryId());
        Company company = resolveCompany(dto.companyId());

        existing.setSku(dto.sku());
        existing.setGameTitle(dto.gameTitle());
        existing.setCategory(category);
        existing.setCompany(company);
        existing.setWeightLbs(dto.weightLbs());
        existing.setCubicFeet(dto.cubicFeet());

        Item saved = repo.save(existing);
        log.info("Updated Item id={} sku='{}' title='{}'",
                saved.getItemId(), saved.getSku(), saved.getGameTitle());
        return toDto(saved);
    }

    // DELETE
    public void deleteById(Integer id) {
        log.debug("Deleting Item id={}", id);

        Item existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: Item not found for id={}", id);
                    return new ResourceNotFoundException("Item not found: " + id);
                });

        boolean usedInWarehouse = warehouseItemRepo.existsByItem_ItemId(id);
        boolean usedInHistory = inventoryHistoryRepo.existsByItem_ItemId(id);

        if (usedInWarehouse || usedInHistory) {
            log.warn("Cannot delete Item id={} because it is referenced by warehouse items={} or history={}",
                    id, usedInWarehouse, usedInHistory);
            throw new ResourceConflictException(
                    "Item is in use by warehouse inventory or history records and cannot be deleted"
            );
        }

        repo.delete(existing);
        log.info("Deleted Item id={}", id);
    }
}
