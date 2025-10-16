/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10529")
@ServiceRegistry
@DomainModel( annotatedClasses = SQLServerNVarCharTypeTest.MyEntity.class)
@RequiresDialect(value = SQLServerDialect.class)
public class SQLServerNVarCharTypeTest {
	private SchemaExport schemaExport;

	@BeforeEach
	public void setUp() {
		schemaExport = new SchemaExport().setHaltOnError( true ).setFormat( false );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		schemaExport.drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testSchemaIsCreatedWithoutExceptions(DomainModelScope modelScope) {
		schemaExport.createOnly( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		long id;

		@Nationalized
		@Column(length = 4001)
		String name;
	}
}
