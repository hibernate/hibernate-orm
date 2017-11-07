/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.Skip;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */

public class QuotedTableNameSchemaUpdateTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.HBM2DLL_CREATE_SCHEMAS, "true" );
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaExport( schemaExport ->
										schemaExport.setFormat( false )
												.create( EnumSet.of( TargetType.DATABASE ) ) );
	}

	@SchemaTest
	@TestForIssue(jiraKey = "HHH-10820")
	@Skip(condition = Skip.OperatingSystem.Windows.class, message = "On Windows, MySQL is case insensitive!")
	public void testSchemaUpdateWithQuotedTableName(SchemaScope scope) throws Exception {
		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate.setHaltOnError( true )
												.setFormat( false )
												.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) ) );

		final List<String> sqlLines = getSqlScriptOutputFileLines();
		assertThat( "The update should recognize the existing table", sqlLines.isEmpty(), is( true ) );
	}

	@Entity(name = "QuotedTable")
	@Table(name = "\"QuotedTable\"")
	public static class QuotedTable {
		@Id
		long id;
	}
}
