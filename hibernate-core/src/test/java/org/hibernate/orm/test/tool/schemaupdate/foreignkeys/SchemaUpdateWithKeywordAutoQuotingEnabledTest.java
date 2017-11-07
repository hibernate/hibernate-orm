/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11061")
public class SchemaUpdateWithKeywordAutoQuotingEnabledTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting(
				org.hibernate.cfg.AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
				"true"
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Match.class };
	}


	@SchemaTest
	public void testUpdate(SchemaScope schemaScope) throws Exception {
		//First create the schema
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport.setHaltOnError( true )
										  .setFormat( false )
										  .createOnly( EnumSet.of( TargetType.DATABASE ) ) );

		//Then try to update the schema
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate
										  .setHaltOnError( true )
										  .setFormat( false )
										  .execute( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ) ) );

		final String sqlScriptOutputFileContent = getSqlScriptOutputFileContent();
		assertThat(
				"The schema update script should be empty but is " + sqlScriptOutputFileContent,
				StringHelper.isEmpty(
						sqlScriptOutputFileContent ),
				is( true )
		);
	}

	@Entity(name = "Match")
	@Table(name = "MATCH")
	public static class Match {
		@Id
		private Long id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable
		private Map<Integer, Integer> timeline = new TreeMap<>();
	}
}
