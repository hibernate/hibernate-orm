/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.sql.storedproc;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;

import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

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
		public void addDialectScope(String dialectName) {
		}

		@Override
		public boolean appliesToDialect(Dialect dialect) {
			return true;
		}

		@Override
		public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
			return "CREATE ALIAS allEmployeeNames AS $$\n" +
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
					"$$";
		}

		@Override
		public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
			return "DROP ALIAS allEmployeeNames IF EXISTS";
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testPartialResults() {
		// metamodel : seems AuxiliaryDatabaseObjects are not exported

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
