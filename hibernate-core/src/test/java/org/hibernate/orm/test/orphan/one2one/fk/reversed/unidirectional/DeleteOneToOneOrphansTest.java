/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.fk.reversed.unidirectional;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/one2one/fk/reversed/unidirectional/Mapping.hbm.xml"
)
@SessionFactory
public class DeleteOneToOneOrphansTest {

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee emp = new Employee();
					emp.setInfo( new EmployeeInfo() );
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
	@JiraKey("HHH-5267")
	public void testOrphanedWhileDetached(SessionFactoryScope scope) {

		Employee e = scope.fromTransaction(
				session -> {
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
					Employee emp = (Employee) results.get( 0 );
					assertNotNull( emp.getInfo() );

					//only fails if the object is detached
					return emp;
				}
		);

		scope.inTransaction(
				session -> {
					e.setInfo( null );

					//save using the new session (this used to work prior to 3.5.x)
					session.merge( e );
				}
		);

		scope.inTransaction(
				session -> {
					Employee emp = session.get( Employee.class, e.getId() );
					assertNull( emp.getInfo() );
					// TODO: If merge was used instead of saveOrUpdate, this would work.
					// However, re-attachment does not currently support handling orphans.
					// See HHH-3795
//		results = session.createQuery( "from EmployeeInfo" ).list();
//		assertEquals( 0, results.size() );
					List results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	@JiraKey("HHH-6484")
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
					emp.setInfo( new EmployeeInfo() );
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
