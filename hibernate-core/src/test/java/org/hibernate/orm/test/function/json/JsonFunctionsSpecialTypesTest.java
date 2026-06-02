/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests handling for special types (UUID, binary, timestamps) in JSON functions.
 * Some databases require special extraction logic using hextoraw() and regexp_replace() etc.
 */
@DomainModel(annotatedClasses = JsonFunctionsSpecialTypesTest.EntityWithSpecialTypes.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
public class JsonFunctionsSpecialTypesTest {

	private static final UUID TEST_UUID = UUID.fromString( "12345678-1234-5678-1234-567812345678" );
	private static final byte[] TEST_BINARY = new byte[] {0x01, 0x02, 0x03, 0x04};
	private static final Timestamp TEST_TIMESTAMP = Timestamp.valueOf( "2024-01-15 10:30:45.123" );
	private static final Instant TEST_INSTANT = Instant.parse( "2024-01-15T10:30:45.123Z" );
	private static final Time TEST_TIME = Time.valueOf( "10:30:45" );

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			EntityWithSpecialTypes entity = new EntityWithSpecialTypes();
			entity.id = 1L;
			entity.uuidValue = TEST_UUID;
			entity.binaryValue = TEST_BINARY;
			entity.timestampValue = TEST_TIMESTAMP;
			entity.instantValue = TEST_INSTANT;
			entity.timeValue = TEST_TIME;
			em.persist( entity );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueUuid(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create a JSON object with UUID and extract it back
			List<UUID> results = em.createQuery(
					"select json_value(json_object('uuid', e.uuidValue), '$.uuid' returning uuid) " +
					"from EntityWithSpecialTypes e",
					UUID.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertEquals( TEST_UUID, results.get( 0 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueBinary(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create a JSON object with binary data and extract it back
			List<byte[]> results = em.createQuery(
					"select json_value(json_object('binary', e.binaryValue), '$.binary' returning binary) " +
					"from EntityWithSpecialTypes e",
					byte[].class
			).getResultList();

			assertEquals( 1, results.size() );
			assertArrayEquals( TEST_BINARY, results.get( 0 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueTimestamp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create a JSON object with timestamp and extract it back
			List<Timestamp> results = em.createQuery(
					"select json_value(json_object('timestamp', e.timestampValue), '$.timestamp' returning timestamp) " +
					"from EntityWithSpecialTypes e",
					Timestamp.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertEquals( TEST_TIMESTAMP, results.get( 0 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueInstant(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create a JSON object with instant and extract it back
			List<Instant> results = em.createQuery(
					"select json_value(json_object('instant', e.instantValue), '$.instant' returning instant) " +
					"from EntityWithSpecialTypes e",
					Instant.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertEquals( TEST_INSTANT, results.get( 0 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueTime(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create a JSON object with time and extract it back
			List<Time> results = em.createQuery(
					"select json_value(json_object('time', e.timeValue), '$.time' returning time) " +
					"from EntityWithSpecialTypes e",
					Time.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertEquals( TEST_TIME, results.get( 0 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonObjectWithUuid(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create JSON object with UUID
			List<String> results = em.createQuery(
					"select json_object('uuid', e.uuidValue) from EntityWithSpecialTypes e",
					String.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertTrue( results.get( 0 ).contains( "\"" + TEST_UUID + "\"" ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonObjectWithBinary(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create JSON object with binary data
			List<String> results = em.createQuery(
					"select json_object('binary', e.binaryValue) from EntityWithSpecialTypes e",
					String.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertTrue( results.get( 0 ).contains( "\"" + PrimitiveByteArrayJavaType.INSTANCE.toString( TEST_BINARY ) + "\"" ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArray.class)
	public void testJsonArrayWithUuid(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create JSON array with UUID
			List<String> results = em.createQuery(
					"select json_array(e.uuidValue) from EntityWithSpecialTypes e",
					String.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertTrue( results.get( 0 ).contains( "\"" + TEST_UUID + "\"" ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArray.class)
	public void testJsonArrayWithBinary(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create JSON array with binary data
			List<String> results = em.createQuery(
					"select json_array(e.binaryValue) from EntityWithSpecialTypes e",
					String.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertTrue( results.get( 0 ).contains( "\"" + PrimitiveByteArrayJavaType.INSTANCE.toString( TEST_BINARY ) + "\"" ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonObjectMultipleSpecialTypes(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Create JSON object with multiple special types
			List<String> results = em.createQuery(
					"select json_object(" +
					"'uuid', e.uuidValue, " +
					"'binary', e.binaryValue, " +
					"'timestamp', e.timestampValue, " +
					"'instant', e.instantValue, " +
					"'time', e.timeValue" +
					") from EntityWithSpecialTypes e",
					String.class
			).getResultList();

			assertEquals( 1, results.size() );
			final String json = results.get( 0 );
			assertTrue( json.contains( "\"" + TEST_UUID + "\"" ) );
			assertTrue( json.contains( "\"" + PrimitiveByteArrayJavaType.INSTANCE.toString( TEST_BINARY ) + "\"" ) );
			// Some databases have trailing zeros for the nanoseconds part
			assertTrue( json.contains( "\"" + toEncodedString( TEST_TIMESTAMP ) ) );
			// Some databases (like Oracle) may include date portion, so just check for the time part
			assertTrue( json.contains( TEST_TIME + "\"" ) );
		} );
	}

	private String toEncodedString(Timestamp timestamp) {
		StringBuilderSqlAppender sqlAppender = new StringBuilderSqlAppender();
		JdbcTimestampJavaType.INSTANCE.appendEncodedString( sqlAppender, timestamp );
		return sqlAppender.toString();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonValueChainedExtraction(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			// Test extracting UUID from a nested JSON structure
			List<UUID> results = em.createQuery(
					"select json_value(" +
					"json_object('data', json_object('uuid', e.uuidValue)), " +
					"'$.data.uuid' returning uuid) " +
					"from EntityWithSpecialTypes e",
					UUID.class
			).getResultList();

			assertEquals( 1, results.size() );
			assertEquals( TEST_UUID, results.get( 0 ) );
		} );
	}

	@Entity(name = "EntityWithSpecialTypes")
	public static class EntityWithSpecialTypes {
		@Id
		Long id;

		@JdbcTypeCode(SqlTypes.UUID)
		UUID uuidValue;

		@JdbcTypeCode(SqlTypes.VARBINARY)
		byte[] binaryValue;

		Timestamp timestampValue;

		@JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
		Instant instantValue;

		Time timeValue;
	}
}
