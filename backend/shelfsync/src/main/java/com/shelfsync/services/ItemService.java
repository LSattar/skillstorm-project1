package com.shelfsync.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.ItemDto;
import com.shelfsync.dtos.ItemResponseDto;
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

/**
 * Service for managing inventory items.
 * 
 * <p>Handles CRUD operations for items, including validation of SKU uniqueness
 * and enforcement of deletion constraints. Items represent products that can be
 * stored in warehouses and tracked in inventory.
 * 
 * <p>Key business rules:
 * <ul>
 *   <li>SKU (Stock Keeping Unit) must be unique across all items</li>
 *   <li>Items cannot be deleted if they are in use by warehouse inventory or history records</li>
 *   <li>Items can be associated with a category and company (both optional)</li>
 *   <li>Items must have cubic feet and weight values for capacity calculations</li>
 * </ul>
 */
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
    
    private ItemResponseDto toResponseDto(Item item) {
        return new ItemResponseDto(
                item.getItemId(),
                item.getSku(),
                item.getGameTitle(),
                item.getCategory(),   
                item.getCompany(),    
                item.getWeightLbs(),
                item.getCubicFeet()
        );
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

    /**
     * Creates a new inventory item with the specified information.
     * 
     * <p>Validates that the SKU is unique before creating the item. The item can
     * optionally be associated with a category and company.
     * 
     * @param dto The item data transfer object containing item information
     * @return The created item response
     * @throws ResourceConflictException if the SKU is already in use
     * @throws ResourceNotFoundException if the category or company does not exist
     */
    public ItemResponseDto create(ItemDto dto) {
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

        return toResponseDto(saved);
    }

    /**
     * Retrieves all inventory items in the system.
     * 
     * @return A list of all items
     */
    public List<ItemResponseDto> findAllItems() {
        log.debug("Fetching all Items");
        List<Item> items = repo.findAll();
        log.info("Fetched {} Items", items.size());
        return items.stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * Retrieves a specific item by its ID.
     * 
     * @param id The item ID
     * @return The item response
     * @throws ResourceNotFoundException if the item does not exist
     */
    public ItemResponseDto findById(Integer id) {
        log.debug("Fetching Item by id={}", id);
        Item item = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Item not found for id={}", id);
                    return new ResourceNotFoundException("Item not found: " + id);
                });

        log.info("Found Item id={} sku='{}' title='{}'",
                item.getItemId(), item.getSku(), item.getGameTitle());
        return toResponseDto(item);
    }

    /**
     * Updates an existing item's information.
     * 
     * <p>Validates SKU uniqueness if the SKU is being changed. If the new SKU
     * is different from the current SKU and already exists, a conflict exception is thrown.
     * 
     * @param id The item ID
     * @param dto The item data transfer object with updated information
     * @return The updated item response
     * @throws ResourceNotFoundException if the item, category, or company does not exist
     * @throws ResourceConflictException if the new SKU is already in use by another item
     */
    public ItemResponseDto update(Integer id, ItemDto dto) {
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
        return toResponseDto(saved);
    }


    /**
     * Deletes an item by its ID.
     * 
     * <p>Validates that the item is not in use before deletion. An item cannot
     * be deleted if it exists in any warehouse inventory or has history records.
     * 
     * @param id The item ID to delete
     * @throws ResourceNotFoundException if the item does not exist
     * @throws ResourceConflictException if the item is in use by warehouse inventory or history records
     */
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
