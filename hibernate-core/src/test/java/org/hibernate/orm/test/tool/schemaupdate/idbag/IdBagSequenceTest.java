/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.idbag;

import java.util.EnumSet;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10373")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class IdBagSequenceTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.HBM2DDL_AUTO, "none" );
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "tool/schemaupdate/idbag/Mappings.hbm.xml" };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testIdBagSequenceGeneratorIsCreated(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate
										  .setHaltOnError( true )
										  .setDelimiter( ";" )
										  .setFormat( true )
										  .execute( EnumSet.of( TargetType.SCRIPT ) ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( fileContent.toLowerCase().contains( "create sequence seq_child_id" ), is( true ) );
	}

}
