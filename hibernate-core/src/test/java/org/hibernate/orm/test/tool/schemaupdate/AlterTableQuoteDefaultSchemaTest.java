/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-12939")
@RequiresDialect(value = H2Dialect.class)
@RequiresDialect(value = PostgreSQL82Dialect.class)
@RequiresDialect(value = SQLServer2012Dialect.class)
public class AlterTableQuoteDefaultSchemaTest extends AbstractAlterTableQuoteSchemaTest {
	public static final String SCHEMA_NAME = "default-schema";

	@BeforeEach
	protected void setUp() throws Exception {
		super.setUp( SCHEMA_NAME );
	}

	@AfterEach
	protected void tearDown() {
		super.tearDown( SCHEMA_NAME );
	}

	@Test
	public void testDefaultSchema() throws Exception {

		executeSchemaUpdate( MyEntity.class );

		String createTableCommand = "create table " + regexpQuote( SCHEMA_NAME, "my_entity" );
		assertThatSqlCommandIsExecuted( createTableCommand );

		executeSchemaUpdate( MyEntityUpdated.class );

		String alterTableCommand = "alter table.* " + regexpQuote( SCHEMA_NAME, "my_entity" );
		assertThatSqlCommandIsExecuted( alterTableCommand );
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		public Integer id;
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntityUpdated {
		@Id
		public Integer id;

		private String title;
	}
}
