/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */

public class ForeignKeyGenerationTest extends BaseUnitTestCase {
	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9591")
	public void oneToOneTest() throws Exception {
		createSchema( new Class[] {User.class, UserSetting.class, Group.class} );

		/*
		The generated SQL for the foreign keys should be:
		alter table USERS add constraint FK_TO_USER_SETTING foreign key (USER_SETTING_ID) references USER_SETTING
		alter table USER_SETTING add constraint FK_TO_USER foreign key (USERS_ID) references USERS
		*/
		checkAlterTableStatement( new AlterTableStatement(
				"USERS",
				"FK_TO_USER_SETTING",
				"USER_SETTING_ID",
				"USER_SETTING"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				"USER_SETTING",
				"FK_TO_USER",
				"USER_ID",
				"USERS"
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10396")
	public void oneToManyTest() throws Exception {
		createSchema( new Class[] {User.class, UserSetting.class, Group.class} );

		/*
		The generated SQL for the foreign keys should be:
		alter table GROUP add constraint FK_USER_GROUP foreign key (USER_ID) references USERS
		*/
		checkAlterTableStatement( new AlterTableStatement(
				"GROUP",
				"FK_USER_GROUP",
				"USER_ID",
				"USERS"
		) );
	}

	private void createSchema(Class[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
		final SchemaExport schemaExport = new SchemaExport( metadata )
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false );
		schemaExport.create( true, false );
	}

	private void checkAlterTableStatement(AlterTableStatement alterTableStatement)
			throws Exception {
		final String expectedAlterTableStatement = alterTableStatement.toSQL();
		final List<String> sqlLines = Files.readAllLines( output.toPath() );
		boolean found = false;
		for ( String line : sqlLines ) {
			if ( line.contains( expectedAlterTableStatement ) ) {
				found = true;
				return;
			}
		}
		assertThat( "Expected alter table statement not found : " + expectedAlterTableStatement, found, is( true ) );
	}

	private static class AlterTableStatement {
		final String tableName;
		final String fkConstraintName;
		final String fkColumnName;
		final String referenceTableName;

		public AlterTableStatement(
				String tableName,
				String fkConstraintName,
				String fkColumnName,
				String referenceTableName) {
			this.tableName = tableName;
			this.fkConstraintName = fkConstraintName;
			this.fkColumnName = fkColumnName;
			this.referenceTableName = referenceTableName;
		}

		public String toSQL() {
			return "alter table " + tableName + " add constraint " + fkConstraintName + " foreign key (" + fkColumnName + ") references " + referenceTableName;
		}
	}

}
