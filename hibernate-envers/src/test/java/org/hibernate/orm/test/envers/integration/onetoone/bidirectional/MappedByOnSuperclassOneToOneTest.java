/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20582")
public class MappedByOnSuperclassOneToOneTest {
	@Test
	public void testBootstrapSucceedsWhenMappedByTargetsSuperclassProperty() {
		final Configuration cfg = new Configuration().addAnnotatedClasses(
				Account.class,
				Credential.class,
				PasswordCredential.class,
				User.class
		);
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		try (SessionFactory sessionFactory = cfg.buildSessionFactory()) {
			assertThat( sessionFactory ).isNotNull();
		}
	}

	@Entity
	@Audited
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type")
	public abstract static class Account {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
	}

	@Entity
	@Audited
	@Table(name = "credential")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type")
	public abstract static class Credential {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "account_id", updatable = false)
		private Account account;

		protected Credential() {}

		protected Credential(Account account) {
			this.account = account;
		}
	}

	@Entity
	@Audited
	@DiscriminatorValue("PWD")
	public static class PasswordCredential extends Credential {

		protected PasswordCredential() {}

		public PasswordCredential(Account account) {
			super( account );
		}
	}

	@Entity
	@Audited
	@DiscriminatorValue("U")
	public static class User extends Account {

		@OneToOne(mappedBy = "account", optional = false, cascade = CascadeType.ALL)
		private PasswordCredential credential;

		protected User() {}

		public User(PasswordCredential credential) {
			this.credential = credential;
		}
	}
}
