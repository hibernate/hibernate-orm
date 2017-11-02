/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.crossschema;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature(value = DialectChecks.SupportSchemaCreation.class)
public class CrossSchemaForeignKeyGenerationTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SchemaOneEntity.class, SchemaTwoEntity.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.HBM2DLL_CREATE_SCHEMAS, "true" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10420")
	public void testSchemaExportForeignKeysAreGeneratedAfterAllTheTablesAreCreated() throws Exception {

		createSchemaExport().setHaltOnError( true )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ) );

		final List<String> sqlLines = getSqlScriptOutputFileLines();
		assertThat(
				"Expected alter table SCHEMA1.Child add constraint but is : " + sqlLines.get( sqlLines.size() - 1 ),
				sqlLines.get( sqlLines.size() - 1 ).startsWith( "alter table " ),
				is( true )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10802")
	public void testSchemaUpdateDoesNotFailResolvingCrossSchemaForeignKey() {

		createSchemaExport().setHaltOnError( true )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.DATABASE ) );

		createSchemaUpdate().setHaltOnError( true )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE ) );
	}
}
