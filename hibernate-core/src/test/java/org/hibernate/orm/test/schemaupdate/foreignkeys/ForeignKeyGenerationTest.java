/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Andrea Boriero
 */
@SkipForDialect(dialectClass = InformixDialect.class,
		reason = "Informix has a strange syntax for 'alter table add constraint'")
public class ForeignKeyGenerationTest extends BaseUnitTestCase {
	private File output;
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@JiraKey(value = "HHH-9591")
	public void oneToOneTest() throws Exception {
		createSchema( new Class[] {User.class, UserSetting.class, Group.class} );

		/*
		The generated SQL for the foreign keys should be:
		alter table USERS add constraint FK_TO_USER_SETTING foreign key (USER_SETTING_ID) references USER_SETTING
		alter table USER_SETTING add constraint FK_TO_USER foreign key (USERS_ID) references USERS
		*/
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"USERS",
				"FK_TO_USER_SETTING",
				"USER_SETTING_ID",
				"USER_SETTING"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"USER_SETTING",
				"FK_TO_USER",
				"USER_ID",
				"USERS"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10396")
	public void oneToManyTest() throws Exception {
		createSchema( new Class[] {User.class, UserSetting.class, Group.class} );

		/*
		The generated SQL for the foreign keys should be:
		alter table GROUP add constraint FK_USER_GROUP foreign key (USER_ID) references USERS
		*/
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"GROUP",
				"FK_USER_GROUP",
				"USER_ID",
				"USERS"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10385")
	public void oneToManyWithJoinTableTest() throws Exception {
		createSchema( new Class[] {Person.class, Phone.class} );

		/*
			The generated SQL for the foreign keys should be:
			alter table PERSON_PHONE add constraint PERSON_ID_FK foreign key (PERSON_ID) references PERSON
			alter table PERSON_PHONE add constraint PHONE_ID_FK foreign key (PHONE_ID) references PHONE
		*/
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"PERSON_PHONE",
				"PERSON_ID_FK",
				"PERSON_ID",
				"PERSON"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"PERSON_PHONE",
				"PHONE_ID_FK",
				"PHONE_ID",
				"PHONE"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10386")
	public void manyToManyTest() throws Exception {
		createSchema( new Class[] {Project.class, Employee.class} );

				/*
				The generated SQL for the foreign keys should be:
				alter table EMPLOYEE_PROJECT add constraint FK_EMPLOYEE foreign key (EMPLOYEE_ID) references EMPLOYEE
				alter table EMPLOYEE_PROJECT add constraint FK_PROJECT foreign key (PROJECT_ID) references PROJECT
				*/
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"EMPLOYEE_PROJECT",
				"FK_EMPLOYEE",
				"EMPLOYEE_ID",
				"EMPLOYEE"
		) );
		checkAlterTableStatement( new AlterTableStatement(
				ssr,
				"EMPLOYEE_PROJECT",
				"FK_PROJECT",
				"PROJECT_ID",
				"PROJECT"
		) );
	}

	private void createSchema(Class<?>[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class<?> c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		final MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
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

		assertThat( "Expected alter table statement not found", sqlLines,
				hasItem( containsString( expectedAlterTableStatement ) ) );
	}

	private static class AlterTableStatement {
		final StandardServiceRegistry ssr;
		final String tableName;
		final String fkConstraintName;
		final String fkColumnName;
		final String referenceTableName;

		public AlterTableStatement(
				StandardServiceRegistry ssr,
				String tableName,
				String fkConstraintName,
				String fkColumnName,
				String referenceTableName) {
			this.ssr = ssr;
			this.tableName = tableName;
			this.fkConstraintName = fkConstraintName;
			this.fkColumnName = fkColumnName;
			this.referenceTableName = referenceTableName;
		}

		public String toSQL() {
			JdbcEnvironment jdbcEnvironment = ssr.requireService( JdbcEnvironment.class );
			Dialect dialect = jdbcEnvironment.getDialect();
			IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();
			UnaryOperator<String> asIdentifier = identifier -> identifierHelper.toIdentifier( identifier ).render( dialect );
			return dialect.getAlterTableString( asIdentifier.apply( tableName ) )
					+ " add constraint " + asIdentifier.apply( fkConstraintName )
					+ " foreign key (" + asIdentifier.apply( fkColumnName ) + ") references " + asIdentifier.apply( referenceTableName );
		}
	}

}
