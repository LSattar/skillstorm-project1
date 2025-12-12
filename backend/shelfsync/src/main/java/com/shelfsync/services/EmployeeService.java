package com.shelfsync.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.shelfsync.dtos.EmployeeDto;
import com.shelfsync.dtos.EmployeeResponseDto;
import com.shelfsync.exceptions.ResourceConflictException;
import com.shelfsync.exceptions.ResourceNotFoundException;
import com.shelfsync.models.Employee;
import com.shelfsync.models.Warehouse;
import com.shelfsync.repositories.EmployeeRepository;
import com.shelfsync.repositories.WarehouseRepository;

/**
 * Service for managing employee operations.
 * 
 * <p>Handles CRUD operations for employees, including validation of email uniqueness
 * and assignment to warehouses. Employees are identified by UUID.
 * 
 * <p>Key business rules:
 * <ul>
 *   <li>Email addresses must be unique across all employees</li>
 *   <li>Employees can be assigned to a warehouse (optional)</li>
 *   <li>Employee deletion does not cascade to related records</li>
 * </ul>
 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository repo;
    private final WarehouseRepository warehouseRepo;

    public EmployeeService(EmployeeRepository repo, WarehouseRepository warehouseRepo) {
        this.repo = repo;
        this.warehouseRepo = warehouseRepo;
    }

    private EmployeeDto toDto(Employee employee) {
        Integer warehouseId = employee.getAssignedWarehouse() != null
                ? employee.getAssignedWarehouse().getWarehouseId()
                : null;

        return new EmployeeDto(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhone(),
                employee.getEmail(),
                warehouseId
        );
    }
    
    private EmployeeResponseDto toResponseDto(Employee employee) {
        return new EmployeeResponseDto(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getAssignedWarehouse() 
        );
    }

    private Warehouse resolveWarehouse(Integer warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        return warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
    }


    /**
     * Creates a new employee with the specified information.
     * 
     * <p>Validates that the email address is unique before creating the employee.
     * The employee can optionally be assigned to a warehouse.
     * 
     * @param dto The employee data transfer object containing employee information
     * @return The created employee response
     * @throws ResourceConflictException if the email address is already in use
     * @throws ResourceNotFoundException if the assigned warehouse does not exist
     */
    public EmployeeResponseDto create(EmployeeDto dto) {
        log.debug("Request to create Employee with email='{}'", dto.email());

        if (repo.existsByEmail(dto.email())) {
            log.warn("Conflict creating Employee: email='{}' already exists", dto.email());
            throw new ResourceConflictException("Email must be unique");
        }

        Warehouse assignedWarehouse = resolveWarehouse(dto.assignedWarehouseId());

        Employee employee = new Employee();
        employee.setPasswordHash("CHANGE_ME"); // create password hash after Spring Security is added
        employee.setFirstName(dto.firstName());
        employee.setLastName(dto.lastName());
        employee.setPhone(dto.phone());
        employee.setEmail(dto.email());
        employee.setAssignedWarehouse(assignedWarehouse);

        Employee saved = repo.save(employee);
        log.info("Created Employee id={} email='{}'", saved.getEmployeeId(), saved.getEmail());
        return toResponseDto(saved);
    }

    /**
     * Retrieves all employees in the system.
     * 
     * @return A list of all employees
     */
    public List<EmployeeResponseDto> findAllEmployees() {
        log.debug("Fetching all Employees");
        List<Employee> employees = repo.findAll();
        log.info("Fetched {} Employees", employees.size());
        return employees.stream()
                .map(this::toResponseDto)
                .toList();
    }

    /**
     * Retrieves a specific employee by their UUID.
     * 
     * @param id The employee UUID
     * @return The employee response
     * @throws ResourceNotFoundException if the employee does not exist
     */
    public EmployeeResponseDto findById(UUID id) {
        log.debug("Fetching Employee by id={}", id);
        Employee employee = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Employee not found for id={}", id);
                    return new ResourceNotFoundException("Employee not found: " + id);
                });

        log.info("Found Employee id={} email='{}'", employee.getEmployeeId(), employee.getEmail());
        return toResponseDto(employee);
    }

    /**
     * Updates an existing employee's information.
     * 
     * <p>Validates email uniqueness if the email is being changed. If the new email
     * is different from the current email and already exists, a conflict exception is thrown.
     * 
     * @param id The employee UUID
     * @param dto The employee data transfer object with updated information
     * @return The updated employee response
     * @throws ResourceNotFoundException if the employee does not exist
     * @throws ResourceConflictException if the new email address is already in use by another employee
     */
    public EmployeeResponseDto update(UUID id, EmployeeDto dto) {
        log.debug("Updating Employee id={} with email='{}'", id, dto.email());

        Employee existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot update: Employee not found for id={}", id);
                    return new ResourceNotFoundException("Employee not found: " + id);
                });

        // Email uniqueness check on update
        if (dto.email() != null &&
            !dto.email().equals(existing.getEmail()) &&
            repo.existsByEmail(dto.email())) {
            log.warn("Cannot update Employee id={}: email='{}' already in use", id, dto.email());
            throw new ResourceConflictException("Email must be unique");
        }

        Warehouse assignedWarehouse = resolveWarehouse(dto.assignedWarehouseId());

        existing.setFirstName(dto.firstName());
        existing.setLastName(dto.lastName());
        existing.setPhone(dto.phone());
        existing.setEmail(dto.email());
        existing.setAssignedWarehouse(assignedWarehouse);

        Employee saved = repo.save(existing);
        log.info("Updated Employee id={} email='{}'", saved.getEmployeeId(), saved.getEmail());
        return toResponseDto(saved);
    }

    /**
     * Deletes an employee by their UUID.
     * 
     * @param id The employee UUID to delete
     * @throws ResourceNotFoundException if the employee does not exist
     */
    public void deleteById(UUID id) {
        log.debug("Deleting Employee id={}", id);

        Employee existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cannot delete: Employee not found for id={}", id);
                    return new ResourceNotFoundException("Employee not found: " + id);
                });

        repo.delete(existing);
        log.info("Deleted Employee id={}", id);
    }
}
