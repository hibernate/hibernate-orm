/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.hhh17404;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.DialectSpecificSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.mapping.basic.JsonMappingTests;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hibernate.testing.orm.junit.DialectContext.getDialect;

/**
 * This test class is about testing that legacy schema that use BLO for JSON column
 * can be safely read even when Oracle Oson extention is in place.
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
					// force creation of a BLOB column type by creating the table ourselves
					session.createNativeQuery( session.getDialect().getDropTableString( "TEST_OSON_COMPAT" ) )
							.executeUpdate();
					session.createNativeQuery( "CREATE TABLE TEST_OSON_COMPAT (id NUMBER, jsonName " + jsonType + " CHECK (jsonName is json)  ,primary key (id))" )
							.executeUpdate();

					String insert = "INSERT INTO TEST_OSON_COMPAT (id, jsonName) VALUES(:id,:json)";
					String jsonstr = "{\"string\":\"john\"}";
					session.createNativeQuery(insert).setParameter("id",1)
							.setParameter( "json", jsonstr).executeUpdate();
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createNativeQuery( getDialect().getDropTableString( "TEST_OSON_COMPAT" ) ).executeUpdate();
				}
		);
	}

	@Test
	public void verifyReadWorks(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					JsonEntity entity = session.find( OracleOsonCompatibilityTest.JsonEntity.class, 1 );
					assertThat( entity.jsonName.getString(), is( "john" ) );
				}
		);
	}

	@Entity(name = "JsonEntity")
	@Table(name = "TEST_OSON_COMPAT")
	public static class JsonEntity {
		@Id
		private Integer id;
		@JdbcTypeCode( SqlTypes.JSON )
		private JsonMappingTests.StringNode jsonName;

		public JsonEntity() {
			super();
		}
		public JsonEntity(Integer id,  JsonMappingTests.StringNode node) {
			this.id = id;
			this.jsonName = node;
		}
	}
}
