package com.shelfsync.services;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.InventoryHistoryDto;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Employee;
import com.shelfsync.models.InventoryHistory;
import com.shelfsync.models.Item;
import com.shelfsync.models.Warehouse;
import com.shelfsync.repositories.EmployeeRepository;
import com.shelfsync.repositories.InventoryHistoryRepository;
import com.shelfsync.repositories.ItemRepository;
import com.shelfsync.repositories.WarehouseRepository;

import jakarta.transaction.Transactional;

/**
 * Service for managing inventory transaction history.
 * 
 * <p>Handles CRUD operations for inventory history records, which track all
 * inventory movements (inbound, outbound, and transfers). This service automatically
 * updates warehouse item quantities when history records are created, updated, or deleted.
 * 
 * <p>Key business rules:
 * <ul>
 *   <li>Creating a history record automatically applies quantity changes to warehouse items</li>
 *   <li>Updating a history record reverses the old changes and applies the new changes</li>
 *   <li>Deleting a history record reverses the quantity changes</li>
 *   <li>All operations are transactional to ensure data consistency</li>
 * </ul>
 * 
 * <p>Transaction types:
 * <ul>
 *   <li>INBOUND: Items added to a warehouse (toWarehouse required, fromWarehouse null)</li>
 *   <li>OUTBOUND: Items removed from a warehouse (fromWarehouse required, toWarehouse null)</li>
 *   <li>TRANSFER: Items moved between warehouses (both fromWarehouse and toWarehouse required)</li>
 * </ul>
 */
@Service
public class InventoryHistoryService {

	private static final Logger log = LoggerFactory.getLogger(InventoryHistoryService.class);

	private final InventoryHistoryRepository repo;
	private final ItemRepository itemRepo;
	private final WarehouseRepository warehouseRepo;
	private final EmployeeRepository employeeRepo;
	private final WarehouseItemService warehouseItemService;

	public InventoryHistoryService(InventoryHistoryRepository repo, ItemRepository itemRepo,
			WarehouseRepository warehouseRepo, EmployeeRepository employeeRepo,
			WarehouseItemService warehouseItemService) { 
		this.repo = repo;
		this.itemRepo = itemRepo;
		this.warehouseRepo = warehouseRepo;
		this.employeeRepo = employeeRepo;
		this.warehouseItemService = warehouseItemService; 
	}

	private InventoryHistoryDto toDto(InventoryHistory history) {
		Integer fromId = history.getFromWarehouse() != null ? history.getFromWarehouse().getWarehouseId() : null;

		Integer toId = history.getToWarehouse() != null ? history.getToWarehouse().getWarehouseId() : null;

		Integer itemId = history.getItem() != null ? history.getItem().getItemId() : null;

		UUID employeeId = history.getPerformedBy() != null ? history.getPerformedBy().getEmployeeId() : null;

		return new InventoryHistoryDto(history.getInventoryHistoryId(), itemId, fromId, toId,
				history.getQuantityChange(), history.getTransactionType(), history.getReason(), history.getOccurredAt(),
				employeeId);
	}

	private Item resolveItem(Integer itemId) {
		if (itemId == null) {
			throw new IllegalArgumentException("itemId is required");
		}
		return itemRepo.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemId));
	}

	private Warehouse resolveWarehouse(Integer warehouseId, String label) {
		if (warehouseId == null) {
			return null;
		}
		return warehouseRepo.findById(warehouseId)
				.orElseThrow(() -> new ResourceNotFoundException(label + " warehouse not found: " + warehouseId));
	}

	private Employee resolveEmployee(UUID employeeId) {
		if (employeeId == null) {
			return null;
		}
		return employeeRepo.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
	}
	
	private void applyHistoryToWarehouseItems(InventoryHistory history, int direction) {
	    if (history.getItem() == null) {
	        log.debug("applyHistoryToWarehouseItems: historyId={} has no item, skipping",
	                history.getInventoryHistoryId());
	        return;
	    }

	    Integer itemId = history.getItem().getItemId();
	    Integer fromWarehouseId = history.getFromWarehouse() != null
	            ? history.getFromWarehouse().getWarehouseId()
	            : null;
	    Integer toWarehouseId = history.getToWarehouse() != null
	            ? history.getToWarehouse().getWarehouseId()
	            : null;

	    Integer qty = history.getQuantityChange();
	    if (qty == null || qty == 0) {
	        log.debug("applyHistoryToWarehouseItems: historyId={} has qty={}, skipping",
	                history.getInventoryHistoryId(), qty);
	        return;
	    }

	    int qtyChange = qty * direction;

	    log.debug("applyHistoryToWarehouseItems: historyId={} itemId={} from={} to={} qty={} dir={}",
	            history.getInventoryHistoryId(), itemId, fromWarehouseId, toWarehouseId, qty, direction);

	    if (fromWarehouseId != null) {
	        warehouseItemService.applyQuantityChange(fromWarehouseId, itemId, -qtyChange);
	    }
	    if (toWarehouseId != null) {
	        warehouseItemService.applyQuantityChange(toWarehouseId, itemId, qtyChange);
	    }
	}

	/**
	 * Creates a new inventory history record and applies the quantity change to warehouse items.
	 * 
	 * <p>This method is transactional. When a history record is created, it automatically
	 * updates the corresponding warehouse item quantities through the WarehouseItemService.
	 * The quantity change is applied based on the transaction type (inbound, outbound, transfer).
	 * 
	 * @param dto The inventory history data transfer object containing transaction details
	 * @return The created inventory history record
	 * @throws ResourceNotFoundException if the item, warehouse(s), or employee does not exist
	 * @throws IllegalArgumentException if the operation would violate business rules (handled by WarehouseItemService)
	 */
    @Transactional
    public InventoryHistoryDto create(InventoryHistoryDto dto) {
        log.debug("Creating InventoryHistory: itemId={} fromWarehouseId={} toWarehouseId={} qtyChange={} type={}",
                dto.itemId(), dto.fromWarehouseId(), dto.toWarehouseId(),
                dto.quantityChange(), dto.transactionType());

        Item item = resolveItem(dto.itemId());
        Warehouse fromWarehouse = resolveWarehouse(dto.fromWarehouseId(), "From");
        Warehouse toWarehouse = resolveWarehouse(dto.toWarehouseId(), "To");
        Employee performedBy = resolveEmployee(dto.performedByEmployeeId());

        InventoryHistory history = new InventoryHistory();
        history.setItem(item);
        history.setFromWarehouse(fromWarehouse);
        history.setToWarehouse(toWarehouse);
        history.setQuantityChange(dto.quantityChange());
        history.setTransactionType(dto.transactionType());
        history.setReason(dto.reason());
        history.setPerformedBy(performedBy);

        InventoryHistory saved = repo.save(history);

        applyHistoryToWarehouseItems(saved, +1);

        log.info("Created InventoryHistory id={} for itemId={} qtyChange={}",
                saved.getInventoryHistoryId(),
                saved.getItem().getItemId(),
                saved.getQuantityChange());

        return toDto(saved);
    }

	/**
	 * Retrieves all inventory history records in the system.
	 * 
	 * @return A list of all inventory history records
	 */
	public List<InventoryHistoryDto> findAll() {
		log.debug("Fetching all InventoryHistory records");
		List<InventoryHistory> records = repo.findAll();
		log.info("Fetched {} InventoryHistory records", records.size());
		return records.stream().map(this::toDto).toList();
	}

	/**
	 * Retrieves a specific inventory history record by its ID.
	 * 
	 * @param id The inventory history ID
	 * @return The inventory history record
	 * @throws ResourceNotFoundException if the history record does not exist
	 */
	public InventoryHistoryDto findById(Integer id) {
		log.debug("Fetching InventoryHistory by id={}", id);
		InventoryHistory history = repo.findById(id).orElseThrow(() -> {
			log.warn("InventoryHistory not found for id={}", id);
			return new ResourceNotFoundException("InventoryHistory not found: " + id);
		});

		log.info("Found InventoryHistory id={} itemId={} qtyChange={}", history.getInventoryHistoryId(),
				history.getItem() != null ? history.getItem().getItemId() : null, history.getQuantityChange());

		return toDto(history);
	}
	
	/**
	 * Retrieves inventory history records for a specific warehouse within a date range.
	 * 
	 * <p>Returns all transactions (inbound, outbound, and transfers) that involve
	 * the specified warehouse, either as source or destination, within the given time period.
	 * 
	 * @param warehouseId The warehouse ID (required)
	 * @param start The start date/time of the range (inclusive)
	 * @param end The end date/time of the range (inclusive)
	 * @return A list of inventory history records matching the criteria
	 * @throws IllegalArgumentException if warehouseId is null
	 */
	@Transactional
	public List<InventoryHistoryDto> findByWarehouseAndDateRange(
	        Integer warehouseId,
	        OffsetDateTime start,
	        OffsetDateTime end
	) {
	    if (warehouseId == null) {
	        throw new IllegalArgumentException("warehouseId is required");
	    }

	    log.debug("Fetching InventoryHistory for warehouseId={} between {} and {}",
	            warehouseId, start, end);

	    List<InventoryHistory> records =
	            repo.findByWarehouseAndDateRange(warehouseId, start, end);

	    return records.stream()
	            .map(this::toDto)
	            .toList();
	}
	
	/**
	 * Retrieves the 10 most recent inventory history records.
	 * 
	 * <p>Useful for displaying recent activity across all warehouses.
	 * Records are ordered by occurrence date/time in descending order (most recent first).
	 * 
	 * @return A list of the 10 most recent inventory history records
	 */
	@Transactional
	public List<InventoryHistoryDto> findRecentActivities() {
	    log.debug("Fetching top 10 most recent inventory history records");

	    List<InventoryHistory> records = repo.findTop10ByOrderByOccurredAtDesc();

	    return records.stream()
	            .map(this::toDto)
	            .toList();
	}

	/**
	 * Updates an existing inventory history record and adjusts warehouse item quantities accordingly.
	 * 
	 * <p>This method is transactional and performs the following steps:
	 * <ol>
	 *   <li>Reverses the original quantity changes from warehouse items</li>
	 *   <li>Updates the history record with new values</li>
	 *   <li>Applies the new quantity changes to warehouse items</li>
	 * </ol>
	 * 
	 * <p>This ensures that warehouse item quantities remain consistent with the history record.
	 * 
	 * @param id The inventory history ID
	 * @param dto The inventory history data transfer object with updated transaction details
	 * @return The updated inventory history record
	 * @throws ResourceNotFoundException if the history record, item, warehouse(s), or employee does not exist
	 * @throws IllegalArgumentException if the operation would violate business rules (handled by WarehouseItemService)
	 */
	@Transactional
    public InventoryHistoryDto update(Integer id, InventoryHistoryDto dto) {
        log.debug("Updating InventoryHistory id={}", id);

        InventoryHistory existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: InventoryHistory not found for id={}", id);
                    return new ResourceNotFoundException("InventoryHistory not found: " + id);
                });

        applyHistoryToWarehouseItems(existing, -1);

        Item item = resolveItem(dto.itemId());
        Warehouse fromWarehouse = resolveWarehouse(dto.fromWarehouseId(), "From");
        Warehouse toWarehouse = resolveWarehouse(dto.toWarehouseId(), "To");
        Employee performedBy = resolveEmployee(dto.performedByEmployeeId());

        existing.setItem(item);
        existing.setFromWarehouse(fromWarehouse);
        existing.setToWarehouse(toWarehouse);
        existing.setQuantityChange(dto.quantityChange());
        existing.setTransactionType(dto.transactionType());
        existing.setReason(dto.reason());
        existing.setPerformedBy(performedBy);

        InventoryHistory saved = repo.save(existing);

        applyHistoryToWarehouseItems(saved, +1);

        log.info("Updated InventoryHistory id={}", saved.getInventoryHistoryId());

        return toDto(saved);
    }

	/**
	 * Deletes an inventory history record and reverses the quantity changes from warehouse items.
	 * 
	 * <p>This method is transactional. When a history record is deleted, it automatically
	 * reverses the quantity changes that were applied when the record was created. This ensures
	 * that warehouse item quantities remain accurate.
	 * 
	 * @param id The inventory history ID to delete
	 * @throws ResourceNotFoundException if the history record does not exist
	 */
    @Transactional
    public void deleteById(Integer id) {
        log.debug("Deleting InventoryHistory id={}", id);

        InventoryHistory existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: InventoryHistory not found for id={}", id);
                    return new ResourceNotFoundException("InventoryHistory not found: " + id);
                });

        applyHistoryToWarehouseItems(existing, -1);

        repo.delete(existing);
        log.info("Deleted InventoryHistory id={}", id);
    }
}
