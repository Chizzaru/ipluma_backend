package com.github.ws_ncip_pnpki.repository;

import com.github.ws_ncip_pnpki.model.TemporaryCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemporaryCredentialRepository extends JpaRepository<TemporaryCredential, Long> {


    @Query("select u.password from TemporaryCredential u where u.employee.id = :employeeId")
    String findPasswordByEmployeeId(@Param("employeeId") Long employeeId);


    @Query("select u from TemporaryCredential u where u.employee.id = :employeeId")
    TemporaryCredential findByEmployeeId(@Param("employeeId") Long employeeId);


    @Query("select u.tempEmailSent from TemporaryCredential u where u.employee.id = :employeeId")
    boolean isTempEmailSent(@Param("employeeId") Long employeeId);
}
