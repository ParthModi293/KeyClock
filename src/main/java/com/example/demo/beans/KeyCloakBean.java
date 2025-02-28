package com.example.demo.beans;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmsResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyCloakBean {



    @Value("${keycloak.clientId}")
    private String adminClientId;

    @Value("${keycloak.server-url}")
    private String url;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.secret}")
    private String clientSecret;



    @Bean
    public Keycloak getKeycloakInstance() {
        Keycloak build = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm(realm)
                .clientId(adminClientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();

        RealmsResource realms = build.realms();
      realms.findAll().forEach(e -> System.out.println(e.getRealm() + " - " + e.getId()));
        return build;
    }




}
