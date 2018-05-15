/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.IOException;
import java.util.EnumSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
public class SchemaExportTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/mapping.hbm.xml" };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testCreateAndDropOnlyType(SchemaScope schemaScope) {
		schemaScope.withSchemaExport( schemaExport -> {
			// create w/o dropping first; (OK because tables don't exist yet
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE );
			assertEquals( 0, schemaExport.getExceptions().size() );

			// create w/o dropping again; should cause an exception because the tables exist already
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.CREATE );
			assertEquals( 1, schemaExport.getExceptions().size() );

			// drop tables only
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
			assertEquals( 0, schemaExport.getExceptions().size() );
		} );
	}

	@SchemaTest
	public void testBothType(SchemaScope schemaScope) {
		schemaScope.withSchemaExport( schemaExport -> {
			// drop beforeQuery create (nothing to drop yeT)
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
			if ( doesDialectSupportDropTableIfExist() ) {
				assertEquals( 0, schemaExport.getExceptions().size() );
			}
			else {
				assertEquals( 1, schemaExport.getExceptions().size() );
			}

			// drop beforeQuery create again (this time drops the tables beforeQuery re-creating)
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );
			int exceptionCount = schemaExport.getExceptions().size();
			if ( doesDialectSupportDropTableIfExist() ) {
				assertEquals( 0, exceptionCount );
			}

			// drop tables
			schemaExport.execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.DROP );
			assertEquals( 0, schemaExport.getExceptions().size() );
		} );
	}

	@SchemaTest
	public void testGenerateDdlToFile(SchemaScope schemaScope) throws IOException {
		schemaScope.withSchemaExport( schemaExport -> {
			// do not script to console or export to database
			schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.DROP );
			if ( doesDialectSupportDropTableIfExist() && schemaExport.getExceptions().size() > 0 ) {
				assertEquals( 2, schemaExport.getExceptions().size() );
			}
		} );
		//check file is not empty
		assertTrue(
				StringHelper.isNotEmpty( getSqlScriptOutputFileContent() ),
				"The drop ddl script has not been created "
		);
	}

	@SchemaTest
	public void testCreateAndDrop(SchemaScope schemaScope) {
		schemaScope.withSchemaExport( schemaExport -> {
			// should drop beforeQuery creating, but tables don't exist yet
			schemaExport.create( EnumSet.of( TargetType.DATABASE ) );
			if ( doesDialectSupportDropTableIfExist() ) {
				assertEquals( 0, schemaExport.getExceptions().size() );
			}
			else {
				assertEquals( 1, schemaExport.getExceptions().size() );
			}

			// call create again; it should drop tables beforeQuery re-creating
			schemaExport.create( EnumSet.of( TargetType.DATABASE ) );
			assertEquals( 0, schemaExport.getExceptions().size() );

			// drop the tables
			schemaExport.drop( EnumSet.of( TargetType.DATABASE ) );
			assertEquals( 0, schemaExport.getExceptions().size() );
		} );
	}

	private boolean doesDialectSupportDropTableIfExist() {
		return Dialect.getDialect().supportsIfExistsAfterTableName() || Dialect.getDialect()
				.supportsIfExistsBeforeTableName();
	}
}
