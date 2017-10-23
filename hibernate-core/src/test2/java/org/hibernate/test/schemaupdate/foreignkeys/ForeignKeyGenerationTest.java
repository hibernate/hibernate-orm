/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.TargetTypeHelper;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	@Test
	@TestForIssue(jiraKey = "HHH-10385")
	public void oneToManyWithJoinTableTest() throws Exception {
		createSchema( new Class[] {Person.class, Phone.class} );

		/*
			The generated SQL for the foreign keys should be:
            alter table PERSON_PHONE add constraint PERSON_ID_FK foreign key (PERSON_ID) references PERSON
            alter table PERSON_PHONE add constraint PHONE_ID_FK foreign key (PHONE_ID) references PHONE
        */
		checkAlterTableStatement( new AlterTableStatement(
				"PERSON_PHONE",
				"PERSON_ID_FK",
				"PERSON_ID",
				"PERSON"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				"PERSON_PHONE",
				"PHONE_ID_FK",
				"PHONE_ID",
				"PHONE"
		) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10386")
	public void manyToManyTest() throws Exception {
		createSchema( new Class[] {Project.class, Employee.class} );

                /*
				The generated SQL for the foreign keys should be:
                alter table EMPLOYEE_PROJECT add constraint FK_EMPLOYEE foreign key (EMPLOYEE_ID) references EMPLOYEE
                alter table EMPLOYEE_PROJECT add constraint FK_PROJECT foreign key (PROJECT_ID) references PROJECT
                */
		checkAlterTableStatement( new AlterTableStatement(
				"EMPLOYEE_PROJECT",
				"FK_EMPLOYEE",
				"EMPLOYEE_ID",
				"EMPLOYEE"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				"EMPLOYEE_PROJECT",
				"FK_PROJECT",
				"PROJECT_ID",
				"PROJECT"
		) );
	}

	private void createSchema(Class[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	private void checkAlterTableStatement(AlterTableStatement alterTableStatement)
			throws Exception {
		final String expectedAlterTableStatement = alterTableStatement.toSQL();
		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );
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
