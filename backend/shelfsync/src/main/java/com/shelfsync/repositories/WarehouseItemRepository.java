package com.shelfsync.repositories;

import com.shelfsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseItemRepository extends JpaRepository <WarehouseItem, WarehouseItemKey> {

}
