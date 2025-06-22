/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.RequiresDialect;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureResultSetMappingTest extends BaseEntityManagerFunctionalTestCase {
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

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		super.afterEntityManagerFactoryBuilt();

		Session s = entityManagerFactory().unwrap( SessionFactory.class ).openSession();
		s.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						connection.createStatement().execute(
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
						);
					}
				}
		);
		s.close();
	}

	@Override
	public void releaseResources() {
		Session s = entityManagerFactory().unwrap( SessionFactory.class ).openSession();
		s.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						connection.createStatement().execute( "DROP ALIAS allEmployeeNames IF EXISTS" );
					}
				}
		);
		s.close();

		super.releaseResources();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Test
	public void testPartialResults() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		StoredProcedureQuery query = em.createStoredProcedureQuery( "allEmployeeNames", "id-fname-lname" );
		List results = query.getResultList();
		assertEquals( 3, results.size() );
		em.getTransaction().commit();
		em.close();
	}
}
