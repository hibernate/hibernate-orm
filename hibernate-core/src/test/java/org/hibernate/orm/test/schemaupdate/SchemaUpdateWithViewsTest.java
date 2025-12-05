/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.hibernate.cfg.MappingSettings.DEFAULT_SCHEMA;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-1872")
@RequiresDialect(PostgreSQLDialect.class)
@ServiceRegistry(settings = {
		@Setting(name= GLOBALLY_QUOTED_IDENTIFIERS, value = "false"),
		@Setting(name = DEFAULT_SCHEMA, value = "public")
})
@DomainModel(annotatedClasses = SchemaUpdateWithViewsTest.MyEntity.class)
@SessionFactory(exportSchema = false)
public class SchemaUpdateWithViewsTest {
	@Test
	public void testUpdateSchema(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "CREATE OR REPLACE VIEW MyEntity AS SELECT 'Hello World' " );
			}
		} ) );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "DROP VIEW IF EXISTS MyEntity " );
			}
		} ) );

		System.out.println( "********* Starting SchemaExport (drop) for TEAR-DOWN *************************" );
		new SchemaExport().drop( EnumSet.of( TargetType.STDOUT, TargetType.DATABASE ), modelScope.getDomainModel() );
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
