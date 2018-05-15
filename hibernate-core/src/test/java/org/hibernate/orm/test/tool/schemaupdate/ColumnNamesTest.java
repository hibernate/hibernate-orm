/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class ColumnNamesTest extends BaseSchemaUnitTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{Employee.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" );
	}

	@SchemaTest
	public void testSchemaUpdateWithQuotedColumnNames(SchemaScope schemaScope) throws Exception {
		//first create the schema
		schemaScope.withSchemaExport( schemaExport -> schemaExport.create( EnumSet.of( TargetType.DATABASE ) ) );

		// try to update the schema
		schemaScope.withSchemaUpdate( schemaUpdate -> schemaUpdate.setHaltOnError( true )
				.execute( EnumSet.of( TargetType.SCRIPT ) )
		);

		// the schema update script shouls be empty
		final String fileContent = getSqlScriptOutputFileContent();
		assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@Id
		private long id;

		@Column(name = "`Age`")
		public String age;

		@Column(name = "Name")
		private String name;

		private String match;

		private String birthday;

		private String homeAddress;
	}
}
