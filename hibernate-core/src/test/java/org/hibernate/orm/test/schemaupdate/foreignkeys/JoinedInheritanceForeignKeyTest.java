/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hamcrest.MatcherAssert;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10352")
@ServiceRegistry
@DomainModel(annotatedClasses = {
		JoinedInheritanceForeignKeyTest.Role.class,
		JoinedInheritanceForeignKeyTest.User.class,
		JoinedInheritanceForeignKeyTest.Person.class
})
public class JoinedInheritanceForeignKeyTest {
	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason="Informix has a strange syntax for 'alter table ... add constraint'")
	public void testForeignKeyHasCorrectName(
			ServiceRegistryScope registryScope,
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "script.sql" );

		createSchema( modelScope, scriptFile );

		final var jdbcEnv = registryScope.getRegistry().requireService( JdbcEnvironment.class );
		checkAlterTableStatement( scriptFile, new AlterTableStatement(
				jdbcEnv, "User",
				"FK_PERSON_ROLE",
				"USER_ID",
				"PersonRole"
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

	private void checkAlterTableStatement(File scriptFile, AlterTableStatement alterTableStatement) throws Exception {
		final String expectedAlterTableStatement = alterTableStatement.toSQL();
		final List<String> sqlLines = Files.readAllLines( scriptFile.toPath(), Charset.defaultCharset() );

		MatcherAssert.assertThat( "Expected alter table statement not found", sqlLines,
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

	@Entity(name = "PersonRole")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "PERSON_ROLE_TYPE", discriminatorType = DiscriminatorType.INTEGER)
	public static class Role {
		@Id
		@GeneratedValue
		protected Long id;
	}

	@Entity(name = "User")
	@DiscriminatorValue("8")
	@PrimaryKeyJoinColumn(name = "USER_ID", foreignKey = @ForeignKey(name = "FK_PERSON_ROLE"))
	public static class User extends Role {
	}

	@Entity(name = "Person")
	@DiscriminatorValue("8")
	@PrimaryKeyJoinColumn(name = "USER_ID")
	public static class Person extends Role {
	}

}
