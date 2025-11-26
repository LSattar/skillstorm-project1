package com.shelfsync.repositories;

import com.shelfsync.models.*;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>{

}
