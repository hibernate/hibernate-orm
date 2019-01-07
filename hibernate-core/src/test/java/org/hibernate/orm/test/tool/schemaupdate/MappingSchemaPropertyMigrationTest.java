/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import org.junit.Assert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class MappingSchemaPropertyMigrationTest extends BaseSchemaUnitTestCase {

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/mapping2.hbm.xml" };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-10678")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
	public void testHibernateMappingSchemaPropertyIsNotIgnored(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport.execute( EnumSet.of( TargetType.SCRIPT ), SchemaExport.Action.CREATE ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( fileContent, fileContent.toLowerCase().contains( "create table schema1.version" ), is( true ) );

		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table schema1.version" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		Assert.assertThat( fileContent, fileContentMatcher.find(), is( true ) );
	}
}
