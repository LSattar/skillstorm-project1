package com.shelfsync.repositories;

import com.shelfsync.models.*;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseItemRepository extends JpaRepository <WarehouseItem, WarehouseItemKey> {

	// FK check for warehouse
	boolean existsByWarehouse_WarehouseId(Integer warehouseId);
	
	// FK check for items
	boolean existsByItem_ItemId(Integer itemId);
	
    @Query("""
            SELECT SUM(wi.quantity * i.cubicFeet)
            FROM WarehouseItem wi
            JOIN wi.item i
            WHERE wi.warehouse.warehouseId = :warehouseId
        """)
        BigDecimal findUsedCapacityCubicFeet(@Param("warehouseId") Integer warehouseId);
	
}
