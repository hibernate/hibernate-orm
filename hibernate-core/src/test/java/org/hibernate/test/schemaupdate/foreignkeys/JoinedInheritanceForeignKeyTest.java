/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
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
import org.hibernate.tool.schema.TargetType;

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
@TestForIssue(jiraKey = "HHH-10352")
public class JoinedInheritanceForeignKeyTest extends BaseUnitTestCase {
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
	public void testForeignKeyHasCorrectName() throws Exception {
		createSchema( new Class[] {Role.class, User.class, Person.class} );
		checkAlterTableStatement( new AlterTableStatement(
				"User",
				"FK_PERSON_ROLE",
				"USER_ID",
				"PersonRole"
		) );
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

	@Entity(name = "User")
	@DiscriminatorValue("8")
	@PrimaryKeyJoinColumn(name = "USER_ID")
	public static class Person extends Role {
	}

}
