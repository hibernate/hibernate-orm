/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.util.EnumSet;
import java.util.List;
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
@TestForIssue(jiraKey = "HHH-10352")
public class JoinedInheritanceForeignKeyTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Role.class, User.class, Person.class };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testForeignKeyHasCorrectName(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport -> schemaExport
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) ) );

		checkAlterTableStatement( expectedAlterTableStatement(
				"User",
				"FK_PERSON_ROLE",
				"USER_ID",
				"PersonRole"
		) );
	}

	private void checkAlterTableStatement(String expectedAlterTableStatement)
			throws Exception {
		final List<String> sqlLines = getSqlScriptOutputFileLines();
		boolean found = false;
		for ( String line : sqlLines ) {
			if ( line.contains( expectedAlterTableStatement ) ) {
				found = true;
				return;
			}
		}
		assertThat( "Expected alter table statement not found : " + expectedAlterTableStatement, found, is( true ) );
	}

	private String expectedAlterTableStatement(
			String tableName,
			String fkConstraintName,
			String fkColumnName,
			String referenceTableName) {
		return "alter table " + tableName + " add constraint " + fkConstraintName + " foreign key (" + fkColumnName + ") references " + referenceTableName;
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
