/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SqlServerNationalizedMappingTests {

	private static final String SQL_SERVER_COMPATIBILITY_SETTING = "hibernate.dialect.sqlserver.compatibility_level";
	private static final String UNICODE_JSON = "{\"name\":\"🦑 Unicode Test 🦀\"}";

	@Nested
	@DomainModel(annotatedClasses = { PlainJsonEntity.class })
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.dialect.SQLServerDialect"),
			@Setting(name = SQL_SERVER_COMPATIBILITY_SETTING, value = "150")
	})
	@SessionFactory
	public class PlainJsonTests {

		@Test
		public void testDefaultJsonIsNvarchar(SessionFactoryScope scope) {
			verifyMapping( scope, PlainJsonEntity.class, "jsonData", SqlTypes.NVARCHAR );
		}
	}

	@Nested
	@DomainModel(annotatedClasses = { NationalizedJsonEntity.class })
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.dialect.SQLServerDialect"),
			@Setting(name = SQL_SERVER_COMPATIBILITY_SETTING, value = "150")
	})
	@SessionFactory
	public class NationalizedAnnotationTests {

		@Test
		public void testNationalizedJsonMappingAndIntegrity(SessionFactoryScope scope) {
			verifyMapping( scope, NationalizedJsonEntity.class, "jsonData", SqlTypes.NVARCHAR );

			scope.inTransaction( session -> {
				NationalizedJsonEntity entity = new NationalizedJsonEntity();
				entity.id = 1;
				entity.jsonData = UNICODE_JSON;
				session.persist( entity );
			} );

			scope.inSession( session -> {
				NationalizedJsonEntity retrieved = session.find( NationalizedJsonEntity.class, 1 );
				assertThat( retrieved.jsonData, is( UNICODE_JSON ) );
			} );
		}
	}

	@Nested
	@DomainModel(annotatedClasses = { JsonEntity.class })
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.DIALECT, value = "org.hibernate.dialect.SQLServerDialect"),
			@Setting(name = SQL_SERVER_COMPATIBILITY_SETTING, value = "150"),
			@Setting(name = AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, value = "true")
	})
	@SessionFactory
	public class GlobalNationalizedSettingsTests {

		@Test
		public void testGlobalNationalizedJsonMappingAndIntegrity(SessionFactoryScope scope) {
			verifyMapping( scope, JsonEntity.class, "jsonData", SqlTypes.NVARCHAR );

			scope.inTransaction( session -> {
				JsonEntity entity = new JsonEntity();
				entity.id = 1;
				entity.jsonData = UNICODE_JSON;
				session.persist( entity );
			} );

			scope.inSession( session -> {
				JsonEntity retrieved = session.find( JsonEntity.class, 1 );
				assertThat( retrieved.jsonData, is( UNICODE_JSON ) );
			} );
		}
	}

	private static void verifyMapping(SessionFactoryScope scope, Class<?> entityClass, String attributeName, int expectedTypeCode) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( entityClass );

		final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping( attributeName );
		final JdbcMapping jdbcMapping = attribute.getJdbcMapping();

		assertThat( "JDBC Type code should be " + expectedTypeCode,
				jdbcMapping.getJdbcType().getJdbcTypeCode(), is( expectedTypeCode ) );
	}

	@Entity(name = "PlainJsonEntity")
	public static class PlainJsonEntity {
		@Id Integer id;
		@JdbcTypeCode(SqlTypes.JSON) String jsonData;
	}

	@Entity(name = "NationalizedJsonEntity")
	public static class NationalizedJsonEntity {
		@Id Integer id;
		@JdbcTypeCode(SqlTypes.JSON) @Nationalized String jsonData;
	}

	@Entity(name = "JsonEntity")
	public static class JsonEntity {
		@Id Integer id;
		@JdbcTypeCode(SqlTypes.JSON) String jsonData;
	}
}
