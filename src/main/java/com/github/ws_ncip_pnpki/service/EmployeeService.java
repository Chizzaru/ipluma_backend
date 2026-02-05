package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.model.Employee;
import com.github.ws_ncip_pnpki.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public List<Employee> searchEmployees(String search){
        try{
            return employeeRepository.searchByName(search);
        }catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }



}
