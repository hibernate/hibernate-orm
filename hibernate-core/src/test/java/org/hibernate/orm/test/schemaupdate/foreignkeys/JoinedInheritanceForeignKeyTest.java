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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10352")
public class JoinedInheritanceForeignKeyTest extends BaseUnitTestCase {
	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

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
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason="Informix has a strange syntax for 'alter table ... add constraint'")
	public void testForeignKeyHasCorrectName() throws Exception {
		createSchema( new Class[] {Role.class, User.class, Person.class} );
		checkAlterTableStatement( new AlterTableStatement(
				ssr, "User",
				"FK_PERSON_ROLE",
				"USER_ID",
				"PersonRole"
		) );
	}

	private void checkAlterTableStatement(AlterTableStatement alterTableStatement)
			throws Exception {
		final String expectedAlterTableStatement = alterTableStatement.toSQL();
		final List<String> sqlLines = Files.readAllLines( output.toPath(), Charset.defaultCharset() );

		assertThat( "Expected alter table statement not found", sqlLines, hasItem( containsString( expectedAlterTableStatement ) ) );
	}

	private static class AlterTableStatement {
		final StandardServiceRegistry ssr;
		final String tableName;
		final String fkConstraintName;
		final String fkColumnName;
		final String referenceTableName;

		public AlterTableStatement(
				StandardServiceRegistry ssr, String tableName,
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
			JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
			Dialect dialect = jdbcEnvironment.getDialect();
			IdentifierHelper identifierHelper = jdbcEnvironment.getIdentifierHelper();
			UnaryOperator<String> asIdentifier = identifier -> identifierHelper.toIdentifier( identifier ).render( dialect );
			return dialect.getAlterTableString( asIdentifier.apply( tableName ) )
					+ " add constraint " + asIdentifier.apply( fkConstraintName )
					+ " foreign key (" + asIdentifier.apply( fkColumnName ) + ") references " + asIdentifier.apply( referenceTableName );
		}
	}

	private void createSchema(Class[] annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
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
