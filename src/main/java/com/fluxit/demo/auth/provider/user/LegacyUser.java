package com.fluxit.demo.auth.provider.user;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import com.fluxit.demo.auth.provider.LegacyUserEntity;

public class LegacyUser extends AbstractUserAdapterFederatedStorage {

	protected LegacyUserEntity entity;
    protected String keycloakId;

    public LegacyUser(KeycloakSession session, RealmModel realm, ComponentModel model, LegacyUserEntity entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getId());
    }
	
	public String getPassword() {
        return entity.getPassword();
    }

    public void setPassword(String password) {
        entity.setPassword(password);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);

    }

	@Override
    public String getId() {
        return keycloakId;
    }

	@Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
        attributes.add(UserModel.USERNAME, getUsername());
        return attributes;
    }

	
	@Override
	public SubjectCredentialManager credentialManager() {
		// Create a new credential manager based on the LegacyUserCredentialManager
		// class
		return new LegacyUserCredentialManager(session, realm, this) {
		};
	}

}
