package com.fluxit.demo.auth.provider.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluxit.demo.auth.provider.LegacyUserEntity;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class LegacyUserStorageProvider
		implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {

	private static final Logger log = LoggerFactory.getLogger(LegacyUserStorageProvider.class);

	private KeycloakSession session;
	private ComponentModel model;
	

	public LegacyUserStorageProvider(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.model = model;
	}

	@Override
	public void close() {
		log.info("Close");
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		log.info("supportsCredentialType({})", credentialType);
		return PasswordCredentialModel.TYPE.endsWith(credentialType);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		log.info("isConfiguredFor(realm={},user={},credentialType={})", realm.getName(), user.getUsername(),
				credentialType);
		return supportsCredentialType(credentialType);
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
		log.info("isValid(realm={},user={},credentialInput.type={})", realm.getName(), user.getUsername(),
				credentialInput.getType());
		log.info("isValid passsword ({}) and supported password ({})", credentialInput.getChallengeResponse(), this.supportsCredentialType(credentialInput.getType()));
		if (!this.supportsCredentialType(credentialInput.getType())) {
			log.info("Invalid credential type: {}", credentialInput.getType());
			return false;
		}
		StorageId sid = new StorageId(user.getId());
		//String username = sid.getExternalId();
		String username = user.getUsername();
		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement("select password from \"user\" where username = ?");
			st.setString(1, username);
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("bcryptCheck({},{})", credentialInput.getChallengeResponse(), rs.getString(1));
				log.info("bcryptCheckResult({})", bcryptCheck(credentialInput.getChallengeResponse(), rs.getString(1)));
				return bcryptCheck(credentialInput.getChallengeResponse(), rs.getString(1));
			} else {
				log.info("isValid User not found: {}", username);
				return false;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Database error:" + ex.getMessage(), ex);
		}
	}

	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		log.info("getUserById({})", id);
		String persistenceId = StorageId.externalId(id);

		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"SELECT id, username, password FROM \"user\" WHERE id = ?");
			st.setInt(1, Integer.parseInt(persistenceId));
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserById user:({})", mapUser(realm, rs));
				return mapUser(realm, rs);
			} else {
				return null;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Database error:" + ex.getMessage(), ex);
		}
	}

	@Override
	public UserModel getUserByUsername(RealmModel realm, String username) {
		log.info("getUserByUsername({})", username);
		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"select id, username, password from \"user\" where username = ?");
			st.setString(1, username);
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserByUsername user:({})", mapUser(realm, rs));
				return mapUser(realm, rs);
			} else {
				return null;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Database error:" + ex.getMessage(), ex);
		}
	}


	@Override
	public UserModel getUserByEmail(RealmModel realm, String email) {
		log.info("getUserByEmail({})", email);
		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"SELECT id, username, password FROM \"user\" WHERE username = ?");
			st.setString(1, email);
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserByEmail user:({})", mapUser(realm, rs));
				return mapUser(realm, rs);
			} else {
				return null;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Database error:" + ex.getMessage(), ex);
		}
	}

	private UserModel mapUser(RealmModel realm, ResultSet rs) throws SQLException {
		LegacyUserEntity entity = new LegacyUserEntity();
		entity.setId(rs.getString(1));
		entity.setUsername(rs.getString(2));
		entity.setPassword(rs.getString(3));
		LegacyUser user = new LegacyUser(session, realm, model, entity);

		return user;
	}

    private boolean bcryptCheck(String plainPassword, String hashedPassword) {
		BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }

}
