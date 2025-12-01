package com.shelfsync.repositories;

import com.shelfsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Integer>{

	// FK check for items
	boolean existsByItem_ItemId(Integer itemId);
	
}
