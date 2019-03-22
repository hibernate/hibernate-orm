/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey="HHH-13191")
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class IdentifierProxyJtaSessionClosedBeforeCommitTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AuthUser.class, AuthClient.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );

		// NOTE: This option is critical in order for the problem to be reproducable.
		// If this option is not set to 'true', then the failure condition does not happen.
		settings.put( AvailableSettings.JPA_PROXY_COMPLIANCE, "true" );
	}

	private Integer authUserId;
	private Integer authClientId;

	@DynamicBeforeAll
	public void prepareAuditData() throws Exception {
		// Revision 1
		inJtaTransaction(
				entityManager -> {
					final AuthUser authUser = new AuthUser();
					final AuthClient authClient = new AuthClient();

					authClient.getAuthUsers().add( authUser );
					authUser.setAuthClient( authClient );

					entityManager.persist( authClient );

					this.authUserId = authUser.getId();
					this.authClientId = authClient.getId();
				}
		);

		// Revision 2
		inJtaTransaction(
				entityManager -> {
					final AuthUser authUser = entityManager.find( AuthUser.class, authUserId );
					authUser.setSomeValue( "test" );
					entityManager.merge( authUser );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( AuthUser.class, authUserId ), contains( 1, 2 ) );
	}

	@Entity(name = "AuthUser")
	@Audited
	public static class AuthUser {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		private String someValue;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="idclient", insertable=false, updatable = false)
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
