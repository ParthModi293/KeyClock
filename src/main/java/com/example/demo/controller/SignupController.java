package com.example.demo.controller;

import com.example.demo.dto.SignUpRequest;
import com.example.demo.service.KeyCloakServiceFinal;
import com.example.demo.service.KeyClockService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final KeyClockService keycloakService;
    private final KeyCloakServiceFinal keyCloakServiceFinal;

    public SignupController(KeyClockService keycloakService, KeyCloakServiceFinal keyCloakServiceFinal) {
        this.keycloakService = keycloakService;
        this.keyCloakServiceFinal = keyCloakServiceFinal;
    }


  /*  @PostMapping
    public Map<String, String> registerUser(@RequestBody SignUpRequest request) {

        String organizationId = keycloakService.createOrganization(request.getOrganizationName());

        if (organizationId == null) {
            return Map.of("error", "Failed to create organization.");
        }

        Map<String, String> userData = keycloakService.createUser(request.getName(), request.getEmail(), organizationId);

        if (userData != null) {
            return userData;
        }
        return Map.of("error", "User registration failed.");
    }*/

    @PostMapping
    public Map<String, String> registerUser(@RequestBody SignUpRequest request) {

        Map<String, String> organization = keyCloakServiceFinal.createOrganization(request);
        return organization;


    }


/*    @PostMapping("/token")
    public String authenticate(@RequestBody SignUpRequest request) {

       String token = keyCloakServiceFinal.authenticateUser(request.getName(), request.getPassword());
        return token;


    }*/


}
