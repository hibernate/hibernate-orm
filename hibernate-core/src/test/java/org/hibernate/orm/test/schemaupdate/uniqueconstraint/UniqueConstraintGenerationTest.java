/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.uniqueconstraint;

import java.io.IOException;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UniqueConstraintGenerationTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "schemaupdate/uniqueconstraint/TestEntity.hbm.xml" };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.HBM2DDL_AUTO, "none" );
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-11101")
	public void testUniqueConstraintIsGenerated(SchemaScope scope) throws Exception {
		scope.withSchemaExport( schemaExport ->
										schemaExport
												.create( EnumSet.of( TargetType.SCRIPT ) ) );

		if ( getDialect() instanceof DB2Dialect ) {
			assertThat(
					"The test_entity_item table unique constraint has not been generated",
					isCreateUniqueIndexGenerated( "test_entity_item", "item" ),
					is( true )
			);
		}
		else {

			assertThat(
					"The test_entity_item table unique constraint has not been generated",
					isUniqueConstraintGenerated( "test_entity_item", "item" ),
					is( true )
			);
		}

		assertThat(
				"The test_entity_children table unique constraint has not been generated",
				isUniqueConstraintGenerated( "test_entity_children", "child" ),
				is( true )
		);
	}

	private boolean isUniqueConstraintGenerated(String tableName, String columnName) throws IOException {
		boolean matches = false;
		final String regex = "alter table " + tableName + " add constraint uk_(.)* unique \\(" + columnName + "\\)";

		final String fileContent = getOutputTempScriptFileName().toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}

	private boolean isCreateUniqueIndexGenerated(String tableName, String columnName) throws IOException {
		boolean matches = false;
		String regex = "create unique index uk_(.)* on " + tableName + " \\(" + columnName + "\\)";

		final String fileContent = getOutputTempScriptFileName().toLowerCase();
		final String[] split = fileContent.split( System.lineSeparator() );
		Pattern p = Pattern.compile( regex );
		for ( String line : split ) {
			final Matcher matcher = p.matcher( line );
			if ( matcher.matches() ) {
				matches = true;
			}
		}
		return matches;
	}
}
