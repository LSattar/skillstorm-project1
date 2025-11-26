package com.shelfsync.repositories;

import com.shelfsync.models.*;
import org.springframework.data.jpa.repository.*;

public interface WarehouseItemRepository extends JpaRepository <WarehouseItem, WarehouseItemKey> {

}
