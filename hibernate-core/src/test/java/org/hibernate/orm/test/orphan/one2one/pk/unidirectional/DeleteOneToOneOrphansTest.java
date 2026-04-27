/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.pk.unidirectional;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/one2one/pk/unidirectional/Mapping.hbm.xml"
)
@SessionFactory
public class DeleteOneToOneOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee emp = new Employee();
					session.persist( emp );
					emp.setInfo( new EmployeeInfo( emp.getId() ) );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOrphanedWhileManaged(SessionFactoryScope scope) {

		Employee e = scope.fromTransaction(
				session -> {
					assertEquals( 1, session.createQuery( "from EmployeeInfo", EmployeeInfo.class ).list().size() );
					List<Employee> employees = session.createQuery( "from Employee", Employee.class ).list();
					assertEquals( 1, employees.size() );
					Employee emp = employees.get( 0 );
					assertNotNull( emp.getInfo() );
					List<Object[]> results = session.createQuery( "from Employee e, EmployeeInfo i where e.info = i", Object[].class ).list();
					assertEquals( 1, results.size() );
					Object[] result = results.get( 0 );
					emp = (Employee) result[0];
					assertNotNull( result[1] );
					assertSame( emp.getInfo(), result[1] );
					emp.setInfo( null );
					return emp;
				}
		);

		scope.inTransaction(
				session -> {
					Employee emp = session.get( Employee.class, e.getId() );
					assertNull( emp.getInfo() );
					assertEquals( 0, session.createQuery( "from EmployeeInfo", EmployeeInfo.class ).list().size() );
					assertEquals( 1, session.createQuery( "from Employee", Employee.class ).list().size() );
				}
		);
	}
}
