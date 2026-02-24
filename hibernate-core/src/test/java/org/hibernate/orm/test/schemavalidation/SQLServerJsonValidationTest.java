/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey("HHH-20092")
@RequiresDialect(SQLServerDialect.class)
@ServiceRegistry(
		settings = @org.hibernate.testing.orm.junit.Setting(
				name = "hibernate.use_nationalized_character_data",
				value = "true"
		)
)
public class SQLServerJsonValidationTest {

	@BeforeEach
	void setUp(ServiceRegistryScope registryScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				try {
					statement.execute( "drop table Foo" );
				}
				catch (Exception ignore) {
				}
				statement.execute(
						"""
							create table Foo (
								id integer not null,
								jsonField nvarchar(max),
								primary key (id)
							)
							"""
				);
			}
		} );
	}

	@AfterEach
	void tearDown(ServiceRegistryScope registryScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "drop table Foo" );
			}
		} );
	}

	@Test
	@DomainModel(annotatedClasses = SQLServerJsonValidationTest.Foo.class)
	public void testSchemaValidation(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {
		@Id
		public Integer id;

		@JdbcTypeCode(SqlTypes.JSON)
		public String jsonField;
	}
}
