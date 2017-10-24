/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.tool.hbm2ddl.SchemaExport;
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
public class MappingSchemaPropertyMigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "schemaupdate/mapping2.hbm.xml" };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10678")
	@RequiresDialectFeature(value = DialectChecks.SupportSchemaCreation.class)
	public void testHibernateMappingSchemaPropertyIsNotIgnored() throws Exception {
		createSchemaExport().execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.CREATE );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( fileContent, fileContent.toLowerCase().contains( "create table schema1.version" ), is( true ) );
	}
}
