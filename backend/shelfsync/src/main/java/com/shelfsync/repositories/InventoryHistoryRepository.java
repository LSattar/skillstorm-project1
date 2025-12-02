package com.shelfsync.repositories;

import com.shelfsync.models.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Integer>{

	boolean existsByItem_ItemId(Integer itemId);
	
    @Query("""
            SELECT h FROM InventoryHistory h
            WHERE (h.fromWarehouse.warehouseId = :warehouseId
                   OR h.toWarehouse.warehouseId = :warehouseId)
              AND h.occurredAt >= COALESCE(:start, h.occurredAt)
              AND h.occurredAt <= COALESCE(:end,   h.occurredAt)
            ORDER BY h.occurredAt DESC
            """)
        List<InventoryHistory> findByWarehouseAndDateRange(
                @Param("warehouseId") Integer warehouseId,
                @Param("start") OffsetDateTime start,
                @Param("end") OffsetDateTime end
        );
    
    List<InventoryHistory> findTop10ByOrderByOccurredAtDesc();
	
}
