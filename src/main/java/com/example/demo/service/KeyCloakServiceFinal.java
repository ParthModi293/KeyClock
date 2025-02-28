package com.example.demo.service;

import com.example.demo.beans.KeyCloakBean;
import com.example.demo.dto.SignUpRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;


@Service
public class KeyCloakServiceFinal {

    @Value("${keycloak.clientId}")
    private String adminClientId;

    @Value("${keycloak.server-url}")
    private String url;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.secret}")
    private String clientSecret;


    private final KeyCloakBean keyCloakBean;


    private static final String ORG_ADMIN_ROLE = "ORG_ADMIN_ROLE";


    public KeyCloakServiceFinal(KeyCloakBean keyCloakBean) {
        this.keyCloakBean = keyCloakBean;
    }


    public Map<String, String> createOrganization(SignUpRequest signUpRequest) {
        Keycloak keycloak = keyCloakBean.getKeycloakInstance();
        RealmResource realmResource = keycloak.realm(realm);

        String formattedOrgName = formatOrganizationName(signUpRequest.getOrganizationName());
        String formattedUserName = formatOrganizationName(signUpRequest.getName());


        List<OrganizationRepresentation> existingOrgs = realmResource.organizations().getAll();
        for (OrganizationRepresentation org : existingOrgs) {
            if (org.getName().equalsIgnoreCase(formattedOrgName)) {
                return Map.of("error", "Organization already exists");
            }
        }

        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName(formattedOrgName);
        organization.setAlias(formattedOrgName);
        organization.setEnabled(true);

        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(formattedOrgName.replaceAll("_", "") + ".com");
        domain.setVerified(false);

        organization.addDomain(domain);
        organization.setDescription("");
        organization.setRedirectUrl("");

        organization.setAttributes(Map.of());

        Response orgResponse = realmResource.organizations().create(organization);
        if (orgResponse.getStatus() != 201) {
            System.out.println(orgResponse.readEntity(String.class));
            return Map.of("error", "Failed to create organization");
        }
        String organizationId = orgResponse.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        UsersResource usersResource = realmResource.users();
        String randomPassword = generateRandomPassword();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(formattedUserName);
        user.setEmail(signUpRequest.getEmail());
        user.setEnabled(true);
//        user.setRequiredActions(List.of("VERIFY_EMAIL"));
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(true);
        credential.setValue(randomPassword);
        credential.setType(CredentialRepresentation.PASSWORD);
        user.setCredentials(List.of(credential));

        Response userResponse = usersResource.create(user);
        if (userResponse.getStatus() != 201) {
            return Map.of("error", "Failed to create user");
        }

        String userId = userResponse.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        realmResource.organizations().get(organizationId).members().addMember(userId);


        String clientUuid = realmResource.clients().findByClientId(adminClientId).get(0).getId();
        RoleRepresentation roleRepresentation = realmResource.clients()
                .get(clientUuid)
                .roles()
                .get(ORG_ADMIN_ROLE)
                .toRepresentation();

        usersResource.get(userId).roles().clientLevel(clientUuid).add(Collections.singletonList(roleRepresentation));


     /*   FOR GETTING ALL ROLE

        List<RoleRepresentation> list = realmResource.clients().get(clientUuid).roles().list();


        for(RoleRepresentation role : list) {
            System.out.println(role.getName());
            System.out.println(role.getId());
            System.out.println(role.getAttributes());
            System.out.println(role.getComposites());
            System.out.println(role.getClientRole());
        }
*/


        return Map.of(
                "organizationId", organizationId,
                "userUuId", userId,
                "password", randomPassword,
                "userName", formattedUserName
        );

    }

 /*   public String authenticateUser(String username, String password) {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm(realm)
                .clientId(adminClientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.PASSWORD)
                .username(username)
                .password(password)
                .build();

        KeycloakBuilder.builder().

        System.out.println(keycloak.tokenManager().getAccessToken().getToken());

        return keycloak.tokenManager().getAccessToken().getToken();
    }*/

    // login into the client and check client roles
    public static void test() {

//        CODE FOR ACCESS TOKEM

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080/")
                .realm("demo")
                .clientId("test")
                .clientSecret("P7T49ntbfwrVGGFv9o8e8PnEa5Snev37")
                .grantType(OAuth2Constants.PASSWORD)
                .username("E1@gmail.com")
                .password("123456")
                .build();

        String uuid = "7f5bb999-5833-4399-80d4-aa201a720794";
        try {
            System.out.println("-------------------------");
        } catch (BadRequestException e) {
            System.out.println("print: " + e.getResponse().readEntity(String.class));
        } catch (ProcessingException e) {
            e.printStackTrace();
            System.out.println("print: " + e.getLocalizedMessage());
            System.out.println("print: " + e.getMessage());
        }

        try {
            System.out.println(keycloak.tokenManager().grantToken());
        } catch (BadRequestException e) {
            e.printStackTrace();
            System.out.println(e.getResponse().readEntity(String.class));
        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getToken());
        } catch (Exception e) {

        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getErrorUri());
        } catch (Exception e) {

        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getTokenType());
        } catch (Exception e) {

        }

    }

    public static void main(String[] args) {

//        CODE FOR EVAULATE

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080/")
                .realm("demo")
                .clientId("test")
                .clientSecret("P7T49ntbfwrVGGFv9o8e8PnEa5Snev37")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();

        String userId = "2d7f42f2-443f-488c-8650-9c1e751a7989";
        String scope = "POST";
        String resourceName = "entity";

        ClientsResource clientsResource = keycloak.realm("demo").clients();
        String clientUUID = clientsResource.findByClientId("test").get(0).getId();
        ClientResource clientResource = clientsResource.get(clientUUID);

        AuthorizationResource authorizationResource = clientResource.authorization();

        List<ResourceRepresentation> resources = authorizationResource.resources().findByName(resourceName);

        if (resources.isEmpty()) {
            throw new RuntimeException("Resource not found: " + resourceName);
        }

        String resourceId = resources.get(0).getId();

        Set<ScopeRepresentation> scopeRepresentation = new HashSet<>();
        scopeRepresentation.add(new ScopeRepresentation(scope));

        /*Map<String, List<String>> attributes= new HashMap<>();
        attributes.put("organization",List.of("jp_morgan"));*/

        List<ResourceRepresentation> resourceRepresentation1 = new ArrayList<>();
        ResourceRepresentation resourceRepresentation = new ResourceRepresentation();
        resourceRepresentation.setName("entity");
        resourceRepresentation.setDisplayName("entity");
        resourceRepresentation.setOwner(clientUUID);
        resourceRepresentation.setScopes(scopeRepresentation);
//        resourceRepresentation.setAttributes(attributes);

        resourceRepresentation1.add(resourceRepresentation);

        PolicyEvaluationRequest policyEvaluationRequest = new PolicyEvaluationRequest();
        policyEvaluationRequest.setUserId(userId);
        policyEvaluationRequest.setClientId(clientUUID);
        policyEvaluationRequest.setEntitlements(false);
        policyEvaluationRequest.setResources(resourceRepresentation1);

        PolicyEvaluationResponse response = authorizationResource.policies().evaluate(policyEvaluationRequest);
        System.out.println("Evaluation Response: " + response.getStatus());

    }


    public static void performRequiredActions() {

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080/")
                .realm("demo")
                .clientId("test")
                .clientSecret("P7T49ntbfwrVGGFv9o8e8PnEa5Snev37")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setValue("123456");
        credential.setType(CredentialRepresentation.PASSWORD);

        String uuid = "7f5bb999-5833-4399-80d4-aa201a720794";
        try {
            keycloak.realm("demo").users().get(uuid).resetPassword(credential);
//            keycloak.realm("demo").users().get(uuid).toRepresentation().setCredentials(List.of(credential));
            keycloak.realm("demo").users().get(uuid).toRepresentation().setFirstName("John");
            keycloak.realm("demo").users().get(uuid).toRepresentation().setLastName("Smith");
        } catch (BadRequestException e) {
            System.out.println("print: " + e.getResponse().readEntity(String.class));
        } catch (ProcessingException e) {
            e.printStackTrace();
            System.out.println("print: " + e.getLocalizedMessage());
            System.out.println("print: " + e.getMessage());
        }

        try {
            System.out.println(keycloak.tokenManager().grantToken());
        } catch (BadRequestException e) {
            e.printStackTrace();
            System.out.println(e.getResponse().readEntity(String.class));
        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getToken());
        } catch (Exception e) {

        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getErrorUri());
        } catch (Exception e) {

        }

        try {
            System.out.println(keycloak.tokenManager().grantToken().getTokenType());
        } catch (Exception e) {

        }

    }


    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }


    public String formatOrganizationName(String organizationName) {
        return organizationName.toLowerCase().replace(" ", "_");
    }
}
