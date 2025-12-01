package com.shelfsync.repositories;

import com.shelfsync.models.*;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>{

	// check for unique email
	boolean existsByEmail(String email);
	
	// FK check for warehouses
	boolean existsByAssignedWarehouse_WarehouseId(Integer warehouseId);
}
