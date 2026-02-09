package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.model.Employee;
import com.github.ws_ncip_pnpki.model.Role;
import com.github.ws_ncip_pnpki.model.TemporaryCredential;
import com.github.ws_ncip_pnpki.model.User;
import com.github.ws_ncip_pnpki.repository.EmployeeRepository;
import com.github.ws_ncip_pnpki.repository.RoleRepository;
import com.github.ws_ncip_pnpki.repository.TemporaryCredentialRepository;
import com.github.ws_ncip_pnpki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TemporaryCredentialService {

    private final TemporaryCredentialRepository temporaryCredentialRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;


    @Transactional
    public void savedTempCredential(){

        List<Employee> employees =  employeeRepository.findDistinctByUserIsNull();

        employees.forEach(employee -> {
            try{
                TemporaryCredential tc = new TemporaryCredential();
                tc.setEmployee(employee);
                String password = generateStrongPassword();
                tc.setPassword(password);
                temporaryCredentialRepository.save(tc);

                User user = new User();

                String firstName = cleanName(employee.getFirstName()).toLowerCase();
                String lastName = cleanName(employee.getLastName()).toLowerCase();
                String username = firstName + "." + lastName;

                SecureRandom secureRandom = new SecureRandom();
                int num1 = secureRandom.nextInt(90) + 10;  // 10-99
                //int num2 = secureRandom.nextInt(90) + 10;  // 10-99
                //String usernameWithNumbers = username + num1 + num2;
                String usernameWithNumbers = username + num1;

                user.setUsername(usernameWithNumbers);
                user.setEmail(employee.getEmail());
                user.setPassword(new BCryptPasswordEncoder().encode(password));

                Set<Role> roles = new HashSet<>();
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Role not found"));
                roles.add(userRole);

                user.setRoles(roles);
                user.setEmployee(employee);

                userRepository.save(user);
            }catch (Exception ex){
                throw new RuntimeException("Error occur while auto create account");
            }

        });
    }

    @Transactional
    public void updateTempEmailSentToTrue(Long employeeId){
        TemporaryCredential tc = temporaryCredentialRepository.findByEmployeeId(employeeId);
        tc.setTempEmailSent(true);
        temporaryCredentialRepository.save(tc);
    }

    public String getTemporaryPassword(Long employeeId){
        return temporaryCredentialRepository.findPasswordByEmployeeId(employeeId);
    }

    public boolean tempEmailSent(Long employeeId){
        return temporaryCredentialRepository.isTempEmailSent(employeeId);
    }


    private String generateStrongPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[6]; // 6 bytes = 8 Base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String cleanName(String name) {
        if (name == null) return "";
        return name.replaceAll("[\"\\s/\\\\\\-]+", "").toLowerCase();
    }

}
