/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.idgenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.TableGenerators;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(value = H2Dialect.class)
public class TableGeneratorsTest extends BaseSchemaCreationTestCase {
	private static final int INITIAL_VALUE = 5;
	private static final int EXPECTED_DB_INSERTED_VALUE = INITIAL_VALUE;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@SchemaTest
	public void testTableGeneratorIsGenerated(SchemaScope schemaScope) throws Exception {
		assertThatActionIsGenerated( "CREATE TABLE TEST_ENTITY \\(ID .*, PRIMARY KEY \\(ID\\)\\)" );

		assertThatActionIsGenerated( "CREATE TABLE ID_TABLE_GENERATOR \\(PK .*, VALUE .*, PRIMARY KEY \\(PK\\)\\)" );

		assertThatActionIsGenerated( "INSERT INTO ID_TABLE_GENERATOR\\(PK, VALUE\\) VALUES \\('TEST_ENTITY_ID'," + EXPECTED_DB_INSERTED_VALUE + "\\)" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	@TableGenerators({
			@TableGenerator(name = "tableGenerator",
					table = "ID_TABLE_GENERATOR",
					pkColumnName = "PK",
					pkColumnValue = "TEST_ENTITY_ID",
					valueColumnName = "VALUE",
					allocationSize = 3,
					initialValue = INITIAL_VALUE)
	}
	)
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "tableGenerator")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
