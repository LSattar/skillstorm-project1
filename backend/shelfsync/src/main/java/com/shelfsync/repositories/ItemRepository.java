package com.shelfsync.repositories;

import com.shelfsync.models.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer>{

	// Used for FK check in category
    boolean existsByCategory_CategoryId(Integer categoryId);
    
    // Used for FK check in company
    boolean existsByCompany_CompanyId(Integer companyId);
	
}
