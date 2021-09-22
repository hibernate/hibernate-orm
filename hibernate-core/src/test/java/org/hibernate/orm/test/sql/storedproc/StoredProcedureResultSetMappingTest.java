/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.storedproc;

import java.util.Date;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.NotImplementedYet;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
@NotImplementedYet(reason = "org.hibernate.procedure.internal.ProcedureCallImpl.buildOutputs not yet implemented")
public class StoredProcedureResultSetMappingTest extends BaseSessionFactoryFunctionalTest {
	@Entity( name = "Employee" )
	@Table( name = "EMP" )
	// ignore the questionable-ness of constructing a partial entity
	@SqlResultSetMapping(
			name = "id-fname-lname",
			classes = {
					@ConstructorResult(
							targetClass = Employee.class,
							columns = {
									@ColumnResult( name = "ID" ),
									@ColumnResult( name = "FIRSTNAME" ),
									@ColumnResult( name = "LASTNAME" )
							}
					)
			}
	)
	public static class Employee {
		@Id
		private int id;
		private String userName;
		private String firstName;
		private String lastName;
		@Temporal( TemporalType.DATE )
		private Date hireDate;

		public Employee() {
		}

		public Employee(Integer id, String firstName, String lastName) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	public static class ProcedureDefinition implements AuxiliaryDatabaseObject {

		public ProcedureDefinition() {
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return true;
		}

		@Override
		public boolean beforeTablesOnCreation() {
			return false;
		}

		@Override
		public String[] sqlCreateStrings(Dialect dialect) {
			return new String[] {
					"CREATE ALIAS allEmployeeNames AS $$\n" +
							"import org.h2.tools.SimpleResultSet;\n" +
							"import java.sql.*;\n" +
							"@CODE\n" +
							"ResultSet allEmployeeNames() {\n" +
							"    SimpleResultSet rs = new SimpleResultSet();\n" +
							"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
							"    rs.addColumn(\"FIRSTNAME\", Types.VARCHAR, 255, 0);\n" +
							"    rs.addColumn(\"LASTNAME\", Types.VARCHAR, 255, 0);\n" +
							"    rs.addRow(1, \"Steve\", \"Ebersole\");\n" +
							"    rs.addRow(1, \"Jane\", \"Doe\");\n" +
							"    rs.addRow(1, \"John\", \"Doe\");\n" +
							"    return rs;\n" +
							"}\n" +
							"$$"
			};
		}

		@Override
		public String[] sqlDropStrings(Dialect dialect) {
			return new String[] {"DROP ALIAS allEmployeeNames IF EXISTS"};
		}

		@Override
		public String getExportIdentifier() {
			return "alias:allEmployeeNames";
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class
		};
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.applyMetadataBuilder( metadataBuilder );
		metadataBuilder.applyAuxiliaryDatabaseObject( new ProcedureDefinition() );
	}

	@Test
	public void testPartialResults() {
		inTransaction(
				session -> {
					ProcedureCall call = session.createStoredProcedureCall( "allEmployeeNames", "id-fname-lname" );
					ProcedureOutputs outputs = call.getOutputs();
					ResultSetOutput output = assertTyping( ResultSetOutput.class, outputs.getCurrent() );
					assertEquals( 3, output.getResultList().size() );
					assertTyping( Employee.class, output.getResultList().get( 0 ) );
				}
		);
	}
}
