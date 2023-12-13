package com.fluxit.demo.auth.provider.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProvider;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.models.credential.dto.PasswordSecretData;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.storage.UserStorageUtil;
import org.jboss.logging.Logger;

import com.fluxit.demo.auth.provider.LegacyUserEntity;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class LegacyUserStorageProvider
		implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {

	private static final Logger log = Logger.getLogger(LegacyUserStorageProvider.class);

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
		log.info("supportsCredentialType "+ credentialType);
		return PasswordCredentialModel.TYPE.endsWith(credentialType);
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		// Convert log to use jboss logger
		log.info("isConfiguredFor: "+ realm.getName() + "  " + user.getUsername() + "  " +
				credentialType);
		return supportsCredentialType(credentialType);
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
		log.info("isValid: realmName"+ realm.getName() + "  user username" + user.getUsername() + "  credentialInputType" +
				credentialInput.getType());
		log.info("isValid password: "+ credentialInput.getChallengeResponse()+ "  supported password: " +
				this.supportsCredentialType(credentialInput.getType()));
		if (!this.supportsCredentialType(credentialInput.getType())) {
			log.info("Invalid credential type: "+ credentialInput.getType());
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
				log.info("bcryptCheck: (input, savedPassword)"+ credentialInput.getChallengeResponse()+ rs.getString(1));
				log.info("bcryptCheckResult: "+ bcryptCheck(credentialInput.getChallengeResponse(), rs.getString(1)));
				boolean isValid = bcryptCheck(credentialInput.getChallengeResponse(), rs.getString(1));
				if (!isValid) {
					log.info("isValid User not valid: "+ username);
					return isValid;
				}
				log.info("isValid User found: "+ username);
				// Store the user back to Keycloak.
				UserModel local = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, user.getUsername());
				if (local != null) {
					log.info("isValid User found in Keycloak: "+ username);
					log.info("isValid Not importing the new user");
					return isValid;
				}
				log.info("isValid User not found in Keycloak: "+ username);
				// If user is valid, create the user in Keycloak if they don't exist
				local = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, user.getUsername());
				log.info("isValid User created in Keycloak: "+ username);
				// local.setFederationLink(model.getId());
				// Here, you can add logic to create the user in Keycloak
				local.setEnabled(true); // Enable the user
				local.setEmail(username); // Set a default email
				local.setEmailVerified(true);
				local.setSingleAttribute("legacy_user", "true"); // Add an attribute to mark legacy users
				PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-password");
				passwordProvider.createCredential(realm, local, credentialInput.getChallengeResponse());
				/* 
				Pbkdf2PasswordHashProvider encoder = new Pbkdf2PasswordHashProvider(
					Pbkdf2Sha512PasswordHashProviderFactory.ID,
					Pbkdf2Sha512PasswordHashProviderFactory.PBKDF2_ALGORITHM,
					Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS,
					512 // Tried setting this to 512, as well, no dice.
        		);
				log.info("isValid password: {}", credentialInput.getChallengeResponse());
				PasswordCredentialModel pcm = encoder.encodedCredential(credentialInput.getChallengeResponse(), Pbkdf2Sha512PasswordHashProviderFactory.DEFAULT_ITERATIONS);
				
				Date date = new Date();

				if (!local.credentialManager().isConfiguredFor(credentialInput.getType())) {
					try {
						local.credentialManager().createStoredCredential(pcm);
						log.info("isValid pcm created: {}", pcm);
						encoder.verify(credentialInput.getChallengeResponse(), pcm);
						log.info("isValid pcm verified: {}", pcm);
						log.info("isValid pcm secretData: {}", pcm.getSecretData());
						log.info("isValid pcm credentialData: {}", pcm.getType());
						log.info("isValid pcm credentialData: {}", pcm.getCredentialData());
						pcm.
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				*/
				return isValid;
			} else {
				log.info("isValid User not found: "+ username);
				return false;
			}
		} catch (SQLException ex) {
			throw new RuntimeException("Database error:" + ex.getMessage(), ex);
		}
	}

	@Override
	public UserModel getUserById(RealmModel realm, String id) {
		log.info("getUserById()"+ id);
		String persistenceId = StorageId.externalId(id);

		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"SELECT id, username, password FROM \"user\" WHERE id = ?");
			st.setInt(1, Integer.parseInt(persistenceId));
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserById user:()"+ mapUser(realm, rs));
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
		log.info("getUserByUsername()" + username);
		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"SELECT id, username, password FROM \"user\" WHERE LOWER(username) = ?");
			st.setString(1, username.toLowerCase());
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserByUsername user:()" + mapUser(realm, rs));
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
		log.info("getUserByEmail()"+ email);
		try (Connection c = LegacyDBConnection.getConnection(this.model)) {
			PreparedStatement st = c.prepareStatement(
					"SELECT id, username, password FROM \"user\" WHERE username = ?");
			st.setString(1, email);
			st.execute();
			ResultSet rs = st.getResultSet();
			if (rs.next()) {
				log.info("getUserByEmail user:()"+ mapUser(realm, rs));
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