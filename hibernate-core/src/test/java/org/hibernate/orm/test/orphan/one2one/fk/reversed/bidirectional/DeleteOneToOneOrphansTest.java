/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.reversed.bidirectional;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/one2one/fk/reversed/bidirectional/Mapping.hbm.xml"
)
@SessionFactory
public class DeleteOneToOneOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee emp = new Employee();
					emp.setInfo( new EmployeeInfo( emp ) );
					session.persist( emp );
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
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
					Employee emp = (Employee) results.get( 0 );
					assertNotNull( emp.getInfo() );
					emp.setInfo( null );
					return emp;
				}
		);

		scope.inTransaction(
				session -> {
					Employee emp = session.get( Employee.class, e.getId() );
					assertNull( emp.getInfo() );
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
				}
		);

	}

	@Test
	@JiraKey(value = "HHH-6484")
	public void testReplacedWhileManaged(SessionFactoryScope scope) {

		Employee e = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
					Employee emp = (Employee) results.get( 0 );
					assertNotNull( emp.getInfo() );

					// Replace with a new EmployeeInfo instance
					emp.setInfo( new EmployeeInfo( emp ) );
					return emp;
				}
		);

		scope.inTransaction(
				session -> {
					Employee emp = session.get( Employee.class, e.getId() );
					assertNotNull( emp.getInfo() );
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
				}
		);

	}
}
