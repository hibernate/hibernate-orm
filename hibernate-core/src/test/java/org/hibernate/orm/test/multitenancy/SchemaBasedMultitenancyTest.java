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
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.relational.SchemaManager;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

@Jpa(annotatedClasses = {SchemaBasedMultitenancyTest.Person.class},
		integrationSettings =
				{@Setting(name = MULTI_TENANT_SCHEMA_MAPPER,
						value = "org.hibernate.orm.test.multitenancy.SchemaBasedMultitenancyTest$MyMapper"),
				@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
						value = "org.hibernate.orm.test.multitenancy.SchemaBasedMultitenancyTest$MyResolver")})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "Warning: setSchema is a no-op in this driver version")
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "getSchema() method not implemented by jTDS")
@JiraKey("HHH-19559")
public class SchemaBasedMultitenancyTest {
	private static String currentTenantIdentifier;

	@Test void test(EntityManagerFactoryScope scope) {
		var schemaManager = (SchemaManager) scope.getEntityManagerFactory().getSchemaManager();
		createSchema( schemaManager, "HELLO" );
		createSchema( schemaManager, "GOODBYE" );
		currentTenantIdentifier = "hello";
		scope.inTransaction( session -> {
			Person person = new Person();
			person.ssn = "123456789";
			person.name = "Gavin";
			session.persist( person );
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.find( Person.class, "123456789" ) );
		} );
		currentTenantIdentifier = "goodbye";
		scope.inTransaction( session -> {
			assertNull( session.find( Person.class, "123456789" ) );
		} );
	}

	private static void createSchema(SchemaManager schemaManager, String schemaName) {
		SchemaManager managerForTenantSchema = schemaManager.forSchema( schemaName );
		managerForTenantSchema.drop(true);
		managerForTenantSchema.create( true );
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

	public static class MyMapper implements TenantSchemaMapper<String> {
		@Override
		public @NonNull String schemaName(@NonNull String tenantIdentifier) {
			return tenantIdentifier;
		}
	}

}
