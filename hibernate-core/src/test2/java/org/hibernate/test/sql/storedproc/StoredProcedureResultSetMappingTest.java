/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

import java.util.Date;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureResultSetMappingTest extends BaseUnitTestCase {
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

	@Test
	public void testPartialResults() {
		Configuration cfg = new Configuration()
				.addAnnotatedClass( Employee.class )
				.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		cfg.addAuxiliaryDatabaseObject( new ProcedureDefinition() );
		SessionFactory sf = cfg.buildSessionFactory();
		try {
			Session session = sf.openSession();
			session.beginTransaction();

			ProcedureCall call = session.createStoredProcedureCall( "allEmployeeNames", "id-fname-lname" );
			ProcedureOutputs outputs = call.getOutputs();
			ResultSetOutput output = assertTyping( ResultSetOutput.class, outputs.getCurrent() );
			assertEquals( 3, output.getResultList().size() );
			assertTyping( Employee.class, output.getResultList().get( 0 ) );

			session.getTransaction().commit();
			session.close();
		}
		finally {
			sf.close();
		}
	}
}
