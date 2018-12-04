/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.dialect.MariaDB53Dialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 *
 * MySQL supports catalogs but not schemas, so the schema name specified in the @Table annotation have to be ignored
 *
 * @author Chris Cranford
 */
@RequiresDialect(value = MySQL5Dialect.class)
@RequiresDialect(value = MariaDB53Dialect.class)
@TestForIssue(jiraKey = "HHH-11455")
public class SchemaUpdateSchemaNameTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleFirst.class };
	}

	@Override
	protected void afterEach(SchemaScope scope) {
		super.afterEach( scope );
		scope.withSchemaDropper(
				null,
				schemaDropper ->
						schemaDropper.doDrop(
								this,
								getSourceDescriptor(),
								getDatabaseTargetDescriptor( EnumSet.of( TargetType.DATABASE ) )
						)
		);
	}

	@SchemaTest
	public void testSqlAlterIgnoresTheTableSchemaName(SchemaScope scope) {
		scope.withSchemaUpdate(
				schemaUpdate -> schemaUpdate.execute( EnumSet.of( TargetType.DATABASE ) )
		);
		SchemaScope schemaScope = regenerateSchemaScope( new Class[] { SimpleNext.class } );
		schemaScope.withSchemaUpdate(
				schemaUpdate -> schemaUpdate.execute( EnumSet.of( TargetType.DATABASE ) )
		);
		executeSqlStatement( "select data from Simple" );
	}

	@MappedSuperclass
	public static abstract class AbstractSimple {
		@Id
		private Integer id;
		private Integer value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity(name = "Simple")
	@Table(name = "Simple", schema = "test")
	public static class SimpleFirst extends AbstractSimple {

	}

	@Entity(name = "Simple")
	@Table(name = "Simple", schema = "test")
	public static class SimpleNext extends AbstractSimple {
		private String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

}
