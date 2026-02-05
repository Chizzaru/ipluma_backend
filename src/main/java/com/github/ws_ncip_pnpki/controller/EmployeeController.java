package com.github.ws_ncip_pnpki.controller;

import com.github.ws_ncip_pnpki.dto.EmployeeResponse;
import com.github.ws_ncip_pnpki.model.Employee;
import com.github.ws_ncip_pnpki.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<List<EmployeeResponse>> searchEmployee(
            @RequestParam(value = "search", defaultValue = "") String search
    ){
        List<EmployeeResponse> employees = employeeService.searchEmployees(search).stream().map(this::convertToResponse).toList();



        return ResponseEntity.status(HttpStatus.OK).body(employees);
    }


    private EmployeeResponse convertToResponse(Employee employee){
        return new EmployeeResponse(employee.getId(), employee.getLastName(), employee.getFirstName(), employee.getEmail());
    }

}
