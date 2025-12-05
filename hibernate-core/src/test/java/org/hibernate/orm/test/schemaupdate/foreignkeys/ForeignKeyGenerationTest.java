/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.function.UnaryOperator;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = InformixDialect.class,
		reason = "Informix has a strange syntax for 'alter table add constraint'")
@ServiceRegistry
public class ForeignKeyGenerationTest {
	@Test
	@JiraKey(value = "HHH-9591")
	@DomainModel(annotatedClasses = {User.class, UserSetting.class, Group.class})
	public void oneToOneTest(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "one-to-one.sql" );
		createSchema( modelScope, scriptFile );

		var jdbcEnv = registryScope.getRegistry().requireService( JdbcEnvironment.class );

		/*
		The generated SQL for the foreign keys should be:
		alter table USERS add constraint FK_TO_USER_SETTING foreign key (USER_SETTING_ID) references USER_SETTING
		alter table USER_SETTING add constraint FK_TO_USER foreign key (USERS_ID) references USERS
		*/
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"USERS",
				"FK_TO_USER_SETTING",
				"USER_SETTING_ID",
				"USER_SETTING"
		) );
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"USER_SETTING",
				"FK_TO_USER",
				"USER_ID",
				"USERS"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10396")
	@DomainModel(annotatedClasses = {User.class, UserSetting.class, Group.class})
	public void oneToManyTest(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "one-to-many.sql" );
		createSchema( modelScope, scriptFile );

		var jdbcEnv = registryScope.getRegistry().requireService( JdbcEnvironment.class );

		/*
		The generated SQL for the foreign keys should be:
		alter table GROUP add constraint FK_USER_GROUP foreign key (USER_ID) references USERS
		*/
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"GROUP",
				"FK_USER_GROUP",
				"USER_ID",
				"USERS"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10385")
	@DomainModel(annotatedClasses = {Person.class, Phone.class})
	public void oneToManyWithJoinTableTest(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "one-to-many-join.sql" );
		createSchema( modelScope, scriptFile );

		var jdbcEnv = registryScope.getRegistry().requireService( JdbcEnvironment.class );

		/*
			The generated SQL for the foreign keys should be:
			alter table PERSON_PHONE add constraint PERSON_ID_FK foreign key (PERSON_ID) references PERSON
			alter table PERSON_PHONE add constraint PHONE_ID_FK foreign key (PHONE_ID) references PHONE
		*/
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"PERSON_PHONE",
				"PERSON_ID_FK",
				"PERSON_ID",
				"PERSON"
		) );
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"PERSON_PHONE",
				"PHONE_ID_FK",
				"PHONE_ID",
				"PHONE"
		) );
	}

	@Test
	@JiraKey(value = "HHH-10386")
	@DomainModel(annotatedClasses = {Project.class, Employee.class})
	public void manyToManyTest(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var scriptFile = new File( tmpDir, "many-to-many.sql" );
		createSchema( modelScope, scriptFile );

		var jdbcEnv = registryScope.getRegistry().requireService( JdbcEnvironment.class );

		/*
		The generated SQL for the foreign keys should be:
		alter table EMPLOYEE_PROJECT add constraint FK_EMPLOYEE foreign key (EMPLOYEE_ID) references EMPLOYEE
		alter table EMPLOYEE_PROJECT add constraint FK_PROJECT foreign key (PROJECT_ID) references PROJECT
		*/
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"EMPLOYEE_PROJECT",
				"FK_EMPLOYEE",
				"EMPLOYEE_ID",
				"EMPLOYEE"
		) );
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv,
				"EMPLOYEE_PROJECT",
				"FK_PROJECT",
				"PROJECT_ID",
				"PROJECT"
		) );
	}

	private void createSchema(DomainModelScope modelScope, File scriptFile) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	private void checkAlterTableStatement(
			File scriptFile,
			AlterTableStatement alterTableStatement) throws Exception {
		final String expectedAlterTableStatement = alterTableStatement.toSQL();
		final List<String> sqlLines = Files.readAllLines( scriptFile.toPath(), Charset.defaultCharset() );

		assertThat( "Expected alter table statement not found", sqlLines,
				hasItem( containsString( expectedAlterTableStatement ) ) );
	}

	private record AlterTableStatement(
			JdbcEnvironment jdbcEnv,
			String tableName,
			String fkConstraintName,
			String fkColumnName,
			String referenceTableName) {
		public String toSQL() {
				Dialect dialect = jdbcEnv.getDialect();
				IdentifierHelper identifierHelper = jdbcEnv.getIdentifierHelper();
				UnaryOperator<String> asIdentifier = identifier -> identifierHelper.toIdentifier( identifier )
						.render( dialect );
				return dialect.getAlterTableString( asIdentifier.apply( tableName ) )
					+ " add constraint " + asIdentifier.apply( fkConstraintName )
					+ " foreign key (" + asIdentifier.apply( fkColumnName ) + ") references " + asIdentifier.apply(
						referenceTableName );
			}
		}

}
