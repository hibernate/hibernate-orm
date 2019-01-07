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

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 is far more resistant to the reserved keyword usage. See HHH-12832.")
public class SchemaUpdateHaltOnErrorTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { From.class };
	}

	@SchemaTest
	public void testHaltOnError(SchemaScope schemaScope) {
		try {
			schemaScope.withSchemaUpdate(
					schemaUpdate ->
							schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );

			fail( "Should halt on error!" );
		}
		catch (Exception e) {
			SchemaManagementException cause = (SchemaManagementException) e;
			assertTrue( cause.getMessage().startsWith( "Halting on error : Error executing DDL" ) );
			assertTrue( cause.getMessage().endsWith( "via JDBC Statement" ) );
		}
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
