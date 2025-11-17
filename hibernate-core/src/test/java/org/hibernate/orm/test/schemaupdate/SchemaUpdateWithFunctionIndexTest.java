/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Basic;
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
 * @author Yoann Rodiere
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10191")
@RequiresDialect(PostgreSQLDialect.class)
@ServiceRegistry(settings = {
		@Setting(name= GLOBALLY_QUOTED_IDENTIFIERS, value = "false"),
		@Setting(name = DEFAULT_SCHEMA, value = "public")
})
@DomainModel(annotatedClasses = SchemaUpdateWithFunctionIndexTest.MyEntity.class)
@SessionFactory
public class SchemaUpdateWithFunctionIndexTest {

	@Test
	public void testUpdateSchema(DomainModelScope modelScope) {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;" );
				statement.execute( "DROP TABLE IF EXISTS MyEntity;" );
				statement.execute( "CREATE TABLE MyEntity(id bigint, name varchar(255));" );
				statement.execute( "CREATE UNIQUE INDEX uk_MyEntity_name_lowercase ON MyEntity (lower(name));" );
			}
		} ) );
	}

	@AfterEach
	public void tearDown(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;" );
				statement.execute( "DROP TABLE IF EXISTS MyEntity;" );
			}
		} ) );
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), modelScope.getDomainModel() );
	}

	@Entity
	@Table(name = "MyEntity", indexes = @Index(columnList = "otherInfo"))
	public static class MyEntity {

		private int id;

		private String name;

		private int otherInfo;

		@Id
		public int getId() {
			return this.id;
		}

		public void setId(final int id) {
			this.id = id;
		}

		@Basic
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Basic
		public int getOtherInfo() {
			return otherInfo;
		}

		public void setOtherInfo(int otherInfo) {
			this.otherInfo = otherInfo;
		}
	}
}
