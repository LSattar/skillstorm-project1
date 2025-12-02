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

	// CREATE
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

	// READ ALL
	public List<InventoryHistoryDto> findAll() {
		log.debug("Fetching all InventoryHistory records");
		List<InventoryHistory> records = repo.findAll();
		log.info("Fetched {} InventoryHistory records", records.size());
		return records.stream().map(this::toDto).toList();
	}

	// READ ONE
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
	
	@Transactional
	public List<InventoryHistoryDto> findRecentActivities() {
	    log.debug("Fetching top 10 most recent inventory history records");

	    List<InventoryHistory> records = repo.findTop10ByOrderByOccurredAtDesc();

	    return records.stream()
	            .map(this::toDto)
	            .toList();
	}

	// UPDATE
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

	// DELETE
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
