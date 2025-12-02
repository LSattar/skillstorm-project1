package com.shelfsync.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.WarehouseCapacityResponse;
import com.shelfsync.dtos.WarehouseDto;
import com.shelfsync.dtos.WarehouseResponseDto;
import com.shelfsync.exceptions.ResourceConflictException;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Employee;
import com.shelfsync.models.Warehouse;
import com.shelfsync.repositories.EmployeeRepository;
import com.shelfsync.repositories.WarehouseItemRepository;
import com.shelfsync.repositories.WarehouseRepository;

@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseRepository repo;
    private final EmployeeRepository employeeRepo;
    private final WarehouseItemRepository warehouseItemRepo;

    public WarehouseService(WarehouseRepository repo,
                            EmployeeRepository employeeRepo,
                            WarehouseItemRepository warehouseItemRepo) {
        this.repo = repo;
        this.employeeRepo = employeeRepo;
        this.warehouseItemRepo = warehouseItemRepo;
    }

    private WarehouseDto toDto(Warehouse warehouse) {
        UUID managerId = warehouse.getManager() != null
                ? warehouse.getManager().getEmployeeId()
                : null;

        return new WarehouseDto(
                warehouse.getWarehouseId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getZip(),
                managerId,
                warehouse.getMaximumCapacityCubicFeet()
        );
    }
    
    private WarehouseResponseDto toResponseDto(Warehouse warehouse) {
        return new WarehouseResponseDto(
                warehouse.getWarehouseId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getZip(),
                warehouse.getManager(),                 
                warehouse.getMaximumCapacityCubicFeet()
        );
    }

    private Employee resolveManager(UUID managerId) {
        if (managerId == null) {
            return null;
        }
        return employeeRepo.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager employee not found: " + managerId));
    }
    
    private Warehouse resolveWarehouse(Integer warehouseId) {
        return repo.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
    }

 // CREATE
    public WarehouseResponseDto create(WarehouseDto dto) {
        log.debug("Request to create Warehouse with name='{}'", dto.name());

        Employee manager = resolveManager(dto.managerEmployeeId());
        BigDecimal maxCap = dto.maximumCapacityCubicFeet();

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.name());
        warehouse.setAddress(dto.address());
        warehouse.setCity(dto.city());
        warehouse.setState(dto.state());
        warehouse.setZip(dto.zip());
        warehouse.setManager(manager);
        warehouse.setMaximumCapacityCubicFeet(maxCap);

        Warehouse saved = repo.save(warehouse);
        log.info("Created Warehouse id={} name='{}'", saved.getWarehouseId(), saved.getName());
        return toResponseDto(saved);
    }

    // READ ALL
    public List<WarehouseResponseDto> findAllWarehouses() {
        log.debug("Fetching all Warehouses");
        List<Warehouse> warehouses = repo.findAll();
        log.info("Fetched {} Warehouses", warehouses.size());
        return warehouses.stream()
                .map(this::toResponseDto)
                .toList();
    }

    // READ ONE
    public WarehouseResponseDto findById(Integer id) {
        log.debug("Fetching Warehouse by id={}", id);
        Warehouse warehouse = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Warehouse not found for id={}", id);
                    return new ResourceNotFoundException("Warehouse not found: " + id);
                });

        log.info("Found Warehouse id={} name='{}'", warehouse.getWarehouseId(), warehouse.getName());
        return toResponseDto(warehouse);
    }

    // UPDATE
    public WarehouseResponseDto update(Integer id, WarehouseDto dto) {
        log.debug("Updating Warehouse id={} with name='{}'", id, dto.name());

        Warehouse existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: Warehouse not found for id={}", id);
                    return new ResourceNotFoundException("Warehouse not found: " + id);
                });

        Employee manager = resolveManager(dto.managerEmployeeId());

        existing.setName(dto.name());
        existing.setAddress(dto.address());
        existing.setCity(dto.city());
        existing.setState(dto.state());
        existing.setZip(dto.zip());
        existing.setManager(manager);
        existing.setMaximumCapacityCubicFeet(dto.maximumCapacityCubicFeet());

        Warehouse saved = repo.save(existing);
        log.info("Updated Warehouse id={} to name='{}'", saved.getWarehouseId(), saved.getName());
        return toResponseDto(saved);
    }

    // DELETE
    public void deleteById(Integer id) {
        log.debug("Deleting Warehouse id={}", id);

        Warehouse existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: Warehouse not found for id={}", id);
                    return new ResourceNotFoundException("Warehouse not found: " + id);
                });

        boolean hasItems = warehouseItemRepo.existsByWarehouse_WarehouseId(id);
        boolean hasEmployees = employeeRepo.existsByAssignedWarehouse_WarehouseId(id);

        if (hasItems || hasEmployees) {
            log.warn(
                "Cannot delete Warehouse id={} because it is referenced by items={}, employees={}",
                id,  hasItems, hasEmployees
            );
            throw new ResourceConflictException(
                "Warehouse is in use by existing items, or employees and cannot be deleted"
            );
        }

        repo.delete(existing);
        log.info("Deleted Warehouse id={}", id);
    }
    
    public WarehouseCapacityResponse getCapacity(Integer warehouseId) {
        Warehouse warehouse = resolveWarehouse(warehouseId);

        BigDecimal max = warehouse.getMaximumCapacityCubicFeet(); 

        BigDecimal used = warehouseItemRepo.findUsedCapacityCubicFeet(warehouseId);
        if (used == null) {
            used = BigDecimal.ZERO;
        }

        BigDecimal available = (max != null)
                ? max.subtract(used)
                : BigDecimal.ZERO;

        BigDecimal utilization = BigDecimal.ZERO;
        if (max != null && max.compareTo(BigDecimal.ZERO) > 0) {
            // used / max * 100 with scale & rounding
            utilization = used
                    .divide(max, 1, RoundingMode.HALF_UP)       
                    .multiply(BigDecimal.valueOf(100));
        }

        return new WarehouseCapacityResponse(
                warehouseId,
                max,
                used,
                available,
                utilization
        );
    }
    
    public List<WarehouseCapacityResponse> getAllCapacities() {
        List<Warehouse> warehouses = repo.findAll();

        return warehouses.stream()
                .map(w -> {
                    BigDecimal max = w.getMaximumCapacityCubicFeet();
                    BigDecimal used = warehouseItemRepo.findUsedCapacityCubicFeet(w.getWarehouseId());
                    if (used == null) {
                        used = BigDecimal.ZERO;
                    }

                    BigDecimal available = (max != null)
                            ? max.subtract(used)
                            : BigDecimal.ZERO;

                    BigDecimal utilization = BigDecimal.ZERO;
                    if (max != null && max.compareTo(BigDecimal.ZERO) > 0) {
                        utilization = used
                                .divide(max, 4, RoundingMode.HALF_UP)   
                                .multiply(BigDecimal.valueOf(100));
                    }

                    return new WarehouseCapacityResponse(
                            w.getWarehouseId(),
                            max,
                            used,
                            available,
                            utilization
                    );
                })
                .toList();
    }
}
