package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {



    @Query("SELECT e FROM Employee e WHERE e.firstName LIKE %:search% OR e.lastName LIKE %:search%")
    List<Employee> searchByName(@Param("search") String search);


    List<Employee> findDistinctByUserIsNull();
}
