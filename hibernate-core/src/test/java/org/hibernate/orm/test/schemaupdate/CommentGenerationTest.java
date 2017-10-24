/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10635")
public class CommentGenerationTest extends BaseSchemaUnitTestCase {

	@Override
	protected String[] getHmbMappingFiles() {
		return new String[] { "schemaupdate/CommentGeneration.hbm.xml" };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.DIALECT, SupportCommentDialect.class.getName() );
	}

	@Test
	public void testSchemaUpdateScriptGeneration() throws Exception {
		createSchemaUpdate()
				.setHaltOnError( true )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ) );

		final String fileContent = getSqlScriptOutputFileContent();
		assertThat(
				"The comment was NOT CREATED",
				fileContent.contains( "comment on column version.description" ),
				is( true )
		);
	}

	public static class SupportCommentDialect extends Dialect {
		@Override
		public boolean supportsCommentOn() {
			return true;
		}
	}
}
