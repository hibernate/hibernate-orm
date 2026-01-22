/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {CredentialsBasedMultitenancyTest.Person.class},
		integrationSettings =
				{@Setting(name = MULTI_TENANT_CREDENTIALS_MAPPER,
						value = "org.hibernate.orm.test.multitenancy.CredentialsBasedMultitenancyTest$MyMapper"),
				@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
						value = "org.hibernate.orm.test.multitenancy.CredentialsBasedMultitenancyTest$MyResolver"),
				@Setting(name = CONNECTION_PROVIDER,
						value = "org.hibernate.orm.test.multitenancy.CredentialsBasedMultitenancyTest$MyConnectionProvider")})
@JiraKey("HHH-19559")
public class CredentialsBasedMultitenancyTest {
	private static String currentTenantIdentifier;
	private static int tenantConnectionsAcquired;

	@Test void test(EntityManagerFactoryScope scope) {
		currentTenantIdentifier = "hello";
		scope.inTransaction( session -> {
			Person person = new Person();
			person.ssn = "123456789";
			person.name = "Gavin";
			session.persist( person );
		} );
		assertEquals( 1, tenantConnectionsAcquired );
		currentTenantIdentifier = "goodbye";
		scope.inTransaction( session -> {
			Person person = new Person();
			person.ssn = "987654321";
			person.name = "Steve";
			session.persist( person );
		} );
		assertEquals( 2, tenantConnectionsAcquired );
	}

	@Entity(name = "PersonForTenant")
	static class Person {
		@Id
		String ssn;
		private String name;
	}

	public static class MyResolver implements CurrentTenantIdentifierResolver<String> {
		@Override
		public @NonNull String resolveCurrentTenantIdentifier() {
			return currentTenantIdentifier;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}
	}

	public static class MyMapper implements TenantCredentialsMapper<String> {
		@Override
		public String user(String tenantIdentifier) {
			return tenantIdentifier;
		}

		@Override
		public String password(String tenantIdentifier) {
			return tenantIdentifier;
		}
	}

	public static class MyConnectionProvider extends DriverManagerConnectionProvider {
		@Override
		public Connection getConnection(String user, String password) throws SQLException {
			assertEquals( user, currentTenantIdentifier );
			assertEquals( password, currentTenantIdentifier );
			tenantConnectionsAcquired ++;
			return getConnection();
		}
	}

}
