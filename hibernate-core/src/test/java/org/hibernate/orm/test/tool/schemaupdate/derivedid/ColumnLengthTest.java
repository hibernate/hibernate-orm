/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.derivedid;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(dialectClass = H2Dialect.class)
@TestForIssue( jiraKey= "HHH-12212")
public class ColumnLengthTest
		extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, Dependent.class };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testTheColumnsLenghtAreApplied(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport -> schemaExport
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ) ) );

		assertTrue( checkCommandIsGenerated(
				getSqlScriptOutputFileLines(),
				"create table DEPENDENT (FK1 varchar(32) not null, FK2 varchar(10) not null, name varchar(255) not null, primary key (FK1, FK2, name))"
		) );

	}

	boolean checkCommandIsGenerated(List<String> generatedCommands, String toCheck) {
		for ( String command : generatedCommands ) {
			if ( command.contains( toCheck ) ) {
				return true;
			}
		}
		return false;
	}

	@Embeddable
	public class EmployeeId implements Serializable {
		@Column(name = "first_name", length = 32)
		String firstName;
		@Column(name = "last_name", length = 10)
		String lastName;
	}

	@Entity
	@Table(name = "EMLOYEE")
	public static class Employee {
		@EmbeddedId
		EmployeeId id;
	}

	@Embeddable
	public class DependentId implements Serializable {
		String name;
		EmployeeId empPK;
	}

	@Entity
	@Table(name = "DEPENDENT")
	public static class Dependent {
		@EmbeddedId
		DependentId id;
		@MapsId("empPK")
		@JoinColumns({
				@JoinColumn(name = "FK1", referencedColumnName = "first_name"),
				@JoinColumn(name = "FK2", referencedColumnName = "last_name")
		})
		@ManyToOne
		Employee emp;
	}

}
