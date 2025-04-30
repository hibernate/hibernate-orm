/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh17404;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.DialectSpecificSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * This test class is about testing that legacy schema that use BLOB or CLOB for JSON columns
 * can be safely read even when Oracle Oson extension is in place.
 * In Such a situation, the JSON type will expect JSON a JSON column and should
 * silently fall back to String deserialization.
 *
 * @author Emmanuel Jannetti
 */
@DomainModel(annotatedClasses = OracleOsonCompatibilityTest.JsonEntity.class)
@SessionFactory(exportSchema = false)
@RequiresDialect( value = OracleDialect.class, majorVersion = 23 )
public abstract class OracleOsonCompatibilityTest {

	@ServiceRegistry(settings = @Setting(name = DialectSpecificSettings.ORACLE_OSON_DISABLED, value = "true"))
	public static class OracleOsonAsUtf8CompatibilityTest extends OracleOsonCompatibilityTest {
		public OracleOsonAsUtf8CompatibilityTest() {
			super( "JSON" );
		}
	}

	public static class OracleBlobAsOsonCompatibilityTest extends OracleOsonCompatibilityTest {
		public OracleBlobAsOsonCompatibilityTest() {
			super( "BLOB" );
		}
	}

	public static class OracleClobAsOsonCompatibilityTest extends OracleOsonCompatibilityTest {
		public OracleClobAsOsonCompatibilityTest() {
			super( "CLOB" );
		}
	}


	private final String jsonType;

	public OracleOsonCompatibilityTest(String jsonType) {
		this.jsonType = jsonType;
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// force creation of a column type by creating the table ourselves
					session.createNativeMutationQuery( session.getDialect().getDropTableString( "TEST_OSON_COMPAT" ) )
							.executeUpdate();
					StringBuilder create = new StringBuilder();
					create.append("CREATE TABLE TEST_OSON_COMPAT (");
					create.append( "id NUMBER").append(',');
					create.append( "payload ").append(jsonType).append(',');
					create.append( "primary key (id))");
					session.createNativeMutationQuery(create.toString()).executeUpdate();

					String insert = "INSERT INTO TEST_OSON_COMPAT (id, payload) VALUES(:id,:json)";

					LocalDateTime theLocalDateTime = LocalDateTime.of( 2000, 1, 1, 0, 0, 0 );
					LocalDate theLocalDate = LocalDate.of( 2000, 1, 1 );
					LocalTime theLocalTime = LocalTime.of( 1, 0, 0 );
					UUID uuid = UUID.fromString("53886a8a-7082-4879-b430-25cb94415be8");
					String theString = "john";

					StringBuilder j = new StringBuilder();
					j.append( "{" );
					j.append( "\"jsonString\":\"").append(theString).append("\"," );
					j.append( "\"theUuid\":\"").append(uuid).append("\"," );
					j.append( "\"theLocalDateTime\":\"").append(theLocalDateTime).append("\"," );
					j.append( "\"theLocalDate\":\"").append(theLocalDate).append("\"," );
					j.append( "\"theLocalTime\":\"").append(theLocalTime).append("\"" );
					j.append( "}" );

					final Object json = jsonType.equals( "BLOB" ) ? j.toString().getBytes() : j.toString();
					session.createNativeMutationQuery( insert )
							.setParameter( "id", 1 )
							.setParameter( "json", json )
							.executeUpdate();
				}
		);
	}

	@Test
	public void verifyReadWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					JsonEntity entity = session.find( OracleOsonCompatibilityTest.JsonEntity.class, 1 );
					assertThat( entity.payload.jsonString, is( "john" ) );
					assertThat( entity.payload.theUuid.toString(), is( "53886a8a-7082-4879-b430-25cb94415be8" ) );
				}
		);
	}

	@Entity(name = "JsonEntity")
	@Table(name = "TEST_OSON_COMPAT")
	public static class JsonEntity {
		@Id
		private Integer id;

		@JdbcTypeCode( SqlTypes.JSON )
		private JsonEntityPayload payload;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public JsonEntityPayload getPayload() {
			return payload;
		}

		public void setPayload(JsonEntityPayload payload) {
			this.payload = payload;
		}
	}

	@Embeddable
	public static class JsonEntityPayload {
		private String jsonString;
		private UUID theUuid;
		private LocalDateTime theLocalDateTime;
		private LocalDate theLocalDate;
		private LocalTime theLocalTime;
	}
}
