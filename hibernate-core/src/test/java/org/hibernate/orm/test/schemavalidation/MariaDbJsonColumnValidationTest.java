/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-18869")
@RequiresDialect(value = MariaDBDialect.class)
@ServiceRegistry
public class MariaDbJsonColumnValidationTest {

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
								bigDecimals json,
								primary key (id)
							) engine=InnoDB
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
	@DomainModel(annotatedClasses =  Foo.class)
	public void testSchemaValidation(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	@Entity(name = "Foo")
	@Table(name = "Foo")
	public static class Foo {
		@Id
		public Integer id;
		public BigDecimal[] bigDecimals;
	}
}
