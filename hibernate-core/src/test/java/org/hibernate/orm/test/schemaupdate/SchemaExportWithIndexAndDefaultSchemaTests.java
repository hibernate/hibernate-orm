/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-9866")
@RequiresDialect(PostgreSQLDialect.class)
@ServiceRegistry(settings = {
		@Setting(name = GLOBALLY_QUOTED_IDENTIFIERS, value = "true"),
		@Setting(name = DEFAULT_SCHEMA, value = "public")
})
@DomainModel(annotatedClasses = SchemaExportWithIndexAndDefaultSchemaTests.MyEntity.class)
public class SchemaExportWithIndexAndDefaultSchemaTests {
	@Test
	public void shouldCreateIndex(DomainModelScope modelScope) {
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		MatcherAssert.assertThat( schemaExport.getExceptions().size(), is( 0 ) );
	}

	@BeforeEach
	public void setUp(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport for START-UP *************************" );
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport for START-UP *************************" );
	}


	@AfterEach
	public void tearDown(DomainModelScope modelScope) {
		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
		System.out.println( "********* Completed SchemaExport (drop) for TEAR-DOWN *************************" );
	}


	@Entity
	@Table(name = "MyEntity", indexes = {@Index(columnList = "id", name = "user_id_hidx")})
	public static class MyEntity {
		private int id;

		@Id
		public int getId() {
			return this.id;
		}

		public void setId(final int id) {
			this.id = id;
		}
	}
}
