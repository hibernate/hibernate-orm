/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
@Jpa(annotatedClasses = {StoredProcedureResultSetMappingTest.Employee.class})
public class StoredProcedureResultSetMappingTest {
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

	@BeforeEach
	protected void setup(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Session s = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).openSession();
					s.doWork(
							connection -> connection.createStatement().execute(
									"""
											CREATE ALIAS allEmployeeNames AS $$
												import org.h2.tools.SimpleResultSet;
												import java.sql.*;
												@CODE
												ResultSet allEmployeeNames() {
													SimpleResultSet rs = new SimpleResultSet();
													rs.addColumn("ID", Types.INTEGER, 10, 0);
													rs.addColumn("FIRSTNAME", Types.VARCHAR, 255, 0);
													rs.addColumn("LASTNAME", Types.VARCHAR, 255, 0);
													rs.addRow(1, "Steve", "Ebersole");
													rs.addRow(1, "Jane", "Doe");
													rs.addRow(1, "John", "Doe");
													return rs;
												}
											$$"""
							)
					);
					s.close();
				}
		);
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.inEntityManager(entityManager -> {
			Session s = scope.getEntityManagerFactory().unwrap( SessionFactory.class ).openSession();
			s.doWork(
					connection -> connection.createStatement().execute( "DROP ALIAS allEmployeeNames IF EXISTS" )
			);
			s.close();
		});
	}

	@Test
	public void testPartialResults(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			StoredProcedureQuery query = em.createStoredProcedureQuery( "allEmployeeNames", "id-fname-lname" );
			List<?> results = query.getResultList();
			assertEquals( 3, results.size() );
		} );
	}
}
