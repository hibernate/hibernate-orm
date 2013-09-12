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
package org.hibernate.jpa.test.procedure;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

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
