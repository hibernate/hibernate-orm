/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.relational.SchemaManager;
import org.hibernate.annotations.TenantId;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER;

@Jpa(annotatedClasses = {SchemaBasedMultitenancyTest.Person.class},
		integrationSettings =
				{@Setting(name = MULTI_TENANT_SCHEMA_MAPPER,
						value = "org.hibernate.orm.test.multitenancy.SchemaBasedMultitenancyTest$MyMapper"),
				@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
						value = "org.hibernate.orm.test.multitenancy.SchemaBasedMultitenancyTest$MyResolver")})
public class SchemaBasedMultitenancyTest {
	@Test void test(EntityManagerFactoryScope scope) {
		var schemaManager = (SchemaManager) scope.getEntityManagerFactory().getSchemaManager();
		SchemaManager managerForTenantSchema = schemaManager.forSchema( "HELLO" );
		managerForTenantSchema.drop(true);
		managerForTenantSchema.create( true );
		scope.inTransaction( session -> {
			Person person = new Person();
			person.ssn = "123456789";
			person.tenantId = "hello";
			person.name = "Gavin";
			session.persist( person );
		} );
	}

	@Entity(name = "PersonForTenant")
	static class Person {
		@Id
		String ssn;
		@TenantId
		String tenantId;
		private String name;
	}

	public static class MyResolver implements CurrentTenantIdentifierResolver<String> {
		@Override
		public @NonNull String resolveCurrentTenantIdentifier() {
			return "hello";
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}
	}

	public static class MyMapper implements TenantSchemaMapper<String> {
		@Override
		public @NonNull String schemaName(@NonNull String tenantIdentifier) {
			return tenantIdentifier;
		}
	}

}
