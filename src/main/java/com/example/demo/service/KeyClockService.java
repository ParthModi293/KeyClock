package com.example.demo.service;

import com.example.demo.beans.KeyCloakBean;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
public class KeyClockService {

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

    public KeyClockService(KeyCloakBean keyCloakBean) {
        this.keyCloakBean = keyCloakBean;

    }




    public String createOrganization(String organizationName) {
        Keycloak keycloak = keyCloakBean.getKeycloakInstance();
        RealmResource realmResource = keycloak.realm(realm);

        String formattedOrgName = formatOrganizationName(organizationName);


        List<OrganizationRepresentation> existingOrgs = realmResource.organizations().getAll();
        for (OrganizationRepresentation org : existingOrgs) {
            if (org.getName().equalsIgnoreCase(formattedOrgName)) {
                return null;
            }
        }

        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName(formattedOrgName);
        organization.setAlias(formattedOrgName);
        organization.setEnabled(true);

        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName("clapcle.com");
        domain.setVerified(false);

        organization.addDomain(domain);
        organization.setDescription("");
        organization.setRedirectUrl("");

        organization.setAttributes(Map.of());

        Response response = realmResource.organizations().create(organization);

        if (response.getStatus() == 201) {
            return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        }
        return null;
    }


    public Map<String, String> createUser(String username, String email, String organizationId) {
        Keycloak keycloak = keyCloakBean.getKeycloakInstance();
        UsersResource usersResource = keycloak.realm(realm).users();

        String randomPassword = generateRandomPassword();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);



        List<CredentialRepresentation> credentials = new ArrayList<>();

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setValue(randomPassword);
        credential.setType(CredentialRepresentation.PASSWORD);

        credentials.add(credential);
        user.setCredentials(credentials);

        Response response = usersResource.create(user);
        if (response.getStatus() == 201) {
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
//            setUserPassword(usersResource, userId, randomPassword);
            assignUserToOrganization(userId, organizationId);
            assignRoleToUser(userId, ORG_ADMIN_ROLE);

            Map<String, String> userData = new HashMap<>();
            userData.put("userId", userId);
            userData.put("password", randomPassword);
            return userData;
        }
        return null;
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);


        usersResource.get(userId).resetPassword(credential);
    }

    public void assignUserToOrganization(String userId, String organizationId) {
        Keycloak keycloak = keyCloakBean.getKeycloakInstance();
        RealmResource realmResource = keycloak.realm(realm);

       realmResource.organizations().get(organizationId).members().addMember(userId);

    }

    public void assignRoleToUser(String userId, String roleName) {
        Keycloak keycloak = keyCloakBean.getKeycloakInstance();

        UsersResource usersResource = keycloak.realm(realm).users();
        UserResource userResource = usersResource.get(userId);

        String clientUuid = keycloak.realm(realm).clients().findByClientId(adminClientId).get(0).getId();



        RoleRepresentation roleRepresentation = keycloak.realm(realm)
                .clients()
                .get(clientUuid)
                .roles()
                .get(roleName)
                .toRepresentation();

        userResource.roles().clientLevel(clientUuid).add(Collections.singletonList(roleRepresentation));

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
