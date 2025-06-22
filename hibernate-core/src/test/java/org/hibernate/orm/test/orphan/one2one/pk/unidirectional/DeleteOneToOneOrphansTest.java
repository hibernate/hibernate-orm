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
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 1, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
					Employee emp = (Employee) results.get( 0 );
					assertNotNull( emp.getInfo() );
					results = session.createQuery( "from Employee e, EmployeeInfo i where e.info = i", Object[].class ).list();
					assertEquals( 1, results.size() );
					Object[] result = (Object[]) results.get( 0 );
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
					List results = session.createQuery( "from EmployeeInfo" ).list();
					assertEquals( 0, results.size() );
					results = session.createQuery( "from Employee" ).list();
					assertEquals( 1, results.size() );
				}
		);
	}
}
