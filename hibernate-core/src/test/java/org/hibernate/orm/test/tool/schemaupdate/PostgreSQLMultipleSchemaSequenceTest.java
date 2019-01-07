/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;
import org.junit.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = PostgreSQL81Dialect.class)
public class PostgreSQLMultipleSchemaSequenceTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Box.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-5538")
	public void test(SchemaScope scope) throws Exception {
		final String extraSchemaName = "extra_schema_sequence_validation";

		scope.withSchemaExport(
				schemaExport ->
						schemaExport.create( EnumSet.of( TargetType.DATABASE ) )
		);

		executeSqlStatement( String.format( "DROP SCHEMA IF EXISTS %s CASCADE", extraSchemaName ) );
		executeSqlStatement( String.format( "CREATE SCHEMA %s", extraSchemaName ) );

		executeSqlSelect( "SELECT NEXTVAL('SEQ_TEST')", resultSet -> {
			try {
				while ( resultSet.next() ) {
					Long sequenceValue = resultSet.getLong( 1 );
					assertEquals( Long.valueOf( 1L ), sequenceValue );
				}
			}
			catch (Exception e) {
				fail( e );
			}
		} );


		scope = regenerateSchemaScope( new Class[] { Box.class } );

		scope.withSchemaExport(
				schemaExport ->
						schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) ) );

		executeSqlSelect( "SELECT NEXTVAL('SEQ_TEST')", resultSet -> {
			try {
				while ( resultSet.next() ) {
					Long sequenceValue = resultSet.getLong( 1 );
					assertEquals( Long.valueOf( 1L ), sequenceValue );
				}
			}
			catch (Exception e) {
				fail( e );
			}
		} );

		executeSqlStatement( String.format( "DROP SCHEMA IF EXISTS %s CASCADE", extraSchemaName ) );
		final List<String> sqlLines = getSqlScriptOutputFileLines();
		Assert.assertEquals(
				2,
				sqlLines.stream()
						.filter( s -> s.equalsIgnoreCase( "create sequence SEQ_TEST start 1 increment 1" ) )
						.count()
		);
	}

	@Entity(name = "Box")
	@Table(name = "Box")
	public static class Box {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TEST")
		@SequenceGenerator(name = "TEST", sequenceName = "SEQ_TEST", allocationSize = 1)
		public Integer id;

	}
}
