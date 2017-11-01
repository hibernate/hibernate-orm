/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.idbag;

import java.util.EnumSet;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
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
@TestForIssue(jiraKey = "HHH-10373")
@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
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
		return new String[]{"schemaupdate/idbag/Mappings.hbm.xml"};
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Test
	public void testIdBagSequenceGeneratorIsCreated() throws Exception {

		createSchemaUpdate()
				.setHaltOnError( true )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( fileContent.toLowerCase().contains( "create sequence seq_child_id" ), is( true ) );
	}

}
