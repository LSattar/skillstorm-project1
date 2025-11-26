package com.shelfsync.repositories;

import com.shelfsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer>{

    boolean existsByCategory_CategoryId(Integer categoryId);
	
}
