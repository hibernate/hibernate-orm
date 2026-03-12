/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNationalizedData.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.IsJtds.class, reverse = true,
		comment = "jTDS driver does not support setNString")
public class NationalizedJsonMappingTests {

	private static final String UNICODE_JSON = "{\"name\": \"🦑 Unicode Test 🦑\"}";

	private static void verifyMapping(SessionFactoryScope scope, Class<?> entityClass, String attributeName, boolean expectNationalized) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor( entityClass );

		final BasicAttributeMapping attribute = (BasicAttributeMapping) entityDescriptor.findAttributeMapping(
				attributeName );
		final JdbcType jdbcType = attribute.getJdbcMapping().getJdbcType();

		// skip verify when database use native JSON type
		if ( jdbcType.isString() ) {
			final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			final int expectedTypeCode = expectNationalized || dialect.getNationalizationSupport() == NationalizationSupport.EXPLICIT
					? SqlTypes.NVARCHAR
					: SqlTypes.VARCHAR;

			assertThat( "Nationalized flag mismatch for " + entityClass.getSimpleName(),
					jdbcType.getJdbcTypeCode(), is( expectedTypeCode ) );
		}
	}

	private static void verifyJson(String actual, String expected) {
		assertThat( "JSON should not be null", actual, notNullValue() );
		try {
			final ObjectMapper mapper = new ObjectMapper();
			final JsonNode actualNode = mapper.readTree( actual );
			final JsonNode expectedNode = mapper.readTree( expected );
			assertThat( actualNode, is( expectedNode ) );
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}

	@Test
	@DomainModel(annotatedClasses = {PlainJsonEntity.class})
	@SessionFactory
	public void testDefaultJsonIntegrity(SessionFactoryScope scope) {
		verifyMapping( scope, PlainJsonEntity.class, "jsonData", false );

		scope.inTransaction( session -> {
			PlainJsonEntity entity = new PlainJsonEntity();
			entity.id = 1;
			entity.jsonData = UNICODE_JSON;
			session.persist( entity );
		} );

		scope.inSession( session -> {
			PlainJsonEntity retrieved = session.find( PlainJsonEntity.class, 1 );

			verifyJson( retrieved.jsonData, UNICODE_JSON );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {NationalizedJsonEntity.class})
	@SessionFactory
	public void testNationalizedJsonIntegrity(SessionFactoryScope scope) {
		verifyMapping( scope, NationalizedJsonEntity.class, "jsonData", true );

		scope.inTransaction( session -> {
			NationalizedJsonEntity entity = new NationalizedJsonEntity();
			entity.id = 1;
			entity.jsonData = UNICODE_JSON;
			session.persist( entity );
		} );

		scope.inSession( session -> {
			NationalizedJsonEntity retrieved = session.find( NationalizedJsonEntity.class, 1 );
			verifyJson( retrieved.jsonData, UNICODE_JSON );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {JsonEntity.class})
	@ServiceRegistry(settings = {
			@Setting(name = AvailableSettings.USE_NATIONALIZED_CHARACTER_DATA, value = "true")
	})
	@SessionFactory
	public void testGlobalNationalizedJsonIntegrity(SessionFactoryScope scope) {
		verifyMapping( scope, JsonEntity.class, "jsonData", true );

		scope.inTransaction( session -> {
			JsonEntity entity = new JsonEntity();
			entity.id = 1;
			entity.jsonData = UNICODE_JSON;
			session.persist( entity );
		} );

		scope.inSession( session -> {
			JsonEntity retrieved = session.find( JsonEntity.class, 1 );
			verifyJson( retrieved.jsonData, UNICODE_JSON );
		} );
	}

	@Entity(name = "PlainJsonEntity")
	public static class PlainJsonEntity {
		@Id
		Integer id;
		@JdbcTypeCode(SqlTypes.JSON)
		String jsonData;
	}

	@Entity(name = "NationalizedJsonEntity")
	public static class NationalizedJsonEntity {
		@Id
		Integer id;
		@JdbcTypeCode(SqlTypes.JSON)
		@Nationalized
		String jsonData;
	}

	@Entity(name = "JsonEntity")
	public static class JsonEntity {
		@Id
		Integer id;
		@JdbcTypeCode(SqlTypes.JSON)
		String jsonData;
	}
}
