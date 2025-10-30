/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13191")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@EnversTest
@Jpa(
		annotatedClasses = {
				IdentifierProxyJtaSessionClosedBeforeCommitTest.AuthUser.class,
				IdentifierProxyJtaSessionClosedBeforeCommitTest.AuthClient.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
				@Setting(name = AvailableSettings.JPA_PROXY_COMPLIANCE, value = "true")
		},
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class IdentifierProxyJtaSessionClosedBeforeCommitTest {
	private Integer authUserId;
	private Integer authClientId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		// Revision 1
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		var entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			final AuthUser authUser = new AuthUser();
			final AuthClient authClient = new AuthClient();

			authClient.getAuthUsers().add( authUser );
			authUser.setAuthClient( authClient );

			entityManager.persist( authClient );

			this.authUserId = authUser.getId();
			this.authClientId = authClient.getId();
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}

		// Revision 2
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			final AuthUser authUser = entityManager.find( AuthUser.class, authUserId );
			authUser.setSomeValue( "test" );
			entityManager.merge( authUser );
		}
		finally {
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				Arrays.asList( 1, 2 ),
				AuditReaderFactory.get( entityManager ).getRevisions( AuthUser.class, authUserId )
		) );
	}

	@Entity(name = "AuthUser")
	@Audited
	public static class AuthUser {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		private String someValue;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idclient", insertable = false, updatable = false)
		private AuthClient authClient;

		public AuthUser() {

		}

		public AuthUser(Integer id, String someValue) {
			this.id = id;
			this.someValue = someValue;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getSomeValue() {
			return someValue;
		}

		public void setSomeValue(String someValue) {
			this.someValue = someValue;
		}

		public AuthClient getAuthClient() {
			return authClient;
		}

		public void setAuthClient(AuthClient authClient) {
			this.authClient = authClient;
		}
	}

	@Entity(name = "AuthClient")
	@Audited
	public static class AuthClient {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "idclient")
		@AuditJoinTable(name = "AuthClient_AuthUser_AUD")
		private List<AuthUser> authUsers = new ArrayList<>();

		public AuthClient() {

		}

		public AuthClient(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<AuthUser> getAuthUsers() {
			return authUsers;
		}

		public void setAuthUsers(List<AuthUser> authUsers) {
			this.authUsers = authUsers;
		}
	}

}
