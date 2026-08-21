/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-20582")
@EnversTest
@Jpa(annotatedClasses = {
		MappedByOnSuperclassOneToOneTest.Account.class,
		MappedByOnSuperclassOneToOneTest.Credential.class,
		MappedByOnSuperclassOneToOneTest.PasswordCredential.class,
		MappedByOnSuperclassOneToOneTest.User.class
})
public class MappedByOnSuperclassOneToOneTest {
	private Integer userId;
	private Integer credentialId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final User user = new User();
			final PasswordCredential credential = new PasswordCredential( user );
			user.setCredential( credential );
			em.persist( user );

			this.userId = user.getId();
			this.credentialId = credential.getId();
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( 1, auditReader.getRevisions( User.class, userId ).size() );
			assertEquals( 1, auditReader.getRevisions( PasswordCredential.class, credentialId ).size() );
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			final User userRev = auditReader.find( User.class, userId, 1 );
			assertNotNull( userRev.getCredential() );
			final PasswordCredential credentialRev = auditReader.find( PasswordCredential.class, credentialId, 1 );
			assertNotNull( credentialRev.getAccount() );
		} );
	}

	@Entity(name = "Account")
	@Audited
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type")
	public abstract static class Account {

		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "Credential")
	@Audited
	@Table(name = "credential")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type")
	public abstract static class Credential {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "account_id", updatable = false)
		private Account account;

		protected Credential() {}

		protected Credential(Account account) {
			this.account = account;
		}

		public Integer getId() {
			return id;
		}

		public Account getAccount() {
			return account;
		}
	}

	@Entity(name = "PasswordCredential")
	@Audited
	@DiscriminatorValue("PWD")
	public static class PasswordCredential extends Credential {

		protected PasswordCredential() {}

		public PasswordCredential(Account account) {
			super( account );
		}
	}

	@Entity(name = "User")
	@Audited
	@DiscriminatorValue("U")
	public static class User extends Account {

		@OneToOne(mappedBy = "account", optional = false, cascade = CascadeType.ALL)
		private PasswordCredential credential;

		protected User() {}

		public void setCredential(PasswordCredential credential) {
			this.credential = credential;
		}

		public PasswordCredential getCredential() {
			return credential;
		}
	}
}
