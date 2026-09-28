/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * @author Sebastian Götz
 */
@DomainModel(annotatedClasses = TupleTransformerTest.Employee.class)
@SessionFactory
public class TupleTransformerTest {
	// Add your tests, using standard JUnit 5.
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String hql = "select empId, empName from Employee";
			Query<EmployeeWrapper> query = session.createQuery( hql, EmployeeWrapper.class )
					.setTupleTransformer( (t,a) -> {
						EmployeeWrapper ew = new EmployeeWrapper();
						ew.setEmpId( (Long) t[0] );
						ew.setEmpName( (String) t[1] );
						return ew;
					});
			assertDoesNotThrow( () -> query.uniqueResult() );
		} );
	}
	@Entity(name = "Employee")
	public static class Employee {
		long empId;
		String empName;
		public Employee() {
		}
		public Employee(long empId, String empName) {
			this.empId = empId;
			this.empName = empName;
		}

		@Id
		public long getEmpId() {
			return empId;
		}
		public void setEmpId(long empId) {
			this.empId = empId;
		}
		public String getEmpName() {
			return empName;
		}
		public void setEmpName(String empName) {
			this.empName = empName;
		}
	}

	public static class EmployeeWrapper {
		long empId;
		String empName;
		public long getEmpId() {
			return empId;
		}
		public void setEmpId(long empId) {
			this.empId = empId;
		}
		public String getEmpName() {
			return empName;
		}
		public void setEmpName(String empName) {
			this.empName = empName;
		}
	}
}
