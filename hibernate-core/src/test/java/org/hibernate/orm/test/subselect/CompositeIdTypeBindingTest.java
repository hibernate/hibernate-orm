/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Strong Liu
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-8312")
@DomainModel(annotatedClasses = { Employee.class, EmployeeGroup.class })
@SessionFactory
public class CompositeIdTypeBindingTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employeegroup = new EmployeeGroup( new EmployeeGroupId( "a", "b" ) );
			employeegroup.addEmployee( new Employee( "stliu" ) );
			employeegroup.addEmployee( new Employee( "david" ) );
			session.persist( employeegroup );

			employeegroup = new EmployeeGroup( new EmployeeGroupId( "c", "d" ) );
			employeegroup.addEmployee( new Employee( "gail" ) );
			employeegroup.addEmployee( new Employee( "steve" ) );
			session.persist( employeegroup );
		});
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testCompositeTypeBinding(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			List<EmployeeGroupId> parameters = new ArrayList<>();
			parameters.add( new EmployeeGroupId( "a", "b" ) );
			parameters.add( new EmployeeGroupId( "c", "d" ) );
			parameters.add( new EmployeeGroupId( "e", "f" ) );

			List<EmployeeGroup> result = session.createQuery( "select eg from EmployeeGroup eg where eg.id in (:employeegroupIds)", EmployeeGroup.class )
					.setParameterList( "employeegroupIds", parameters ).list();

			assertEquals( 2, result.size() );

			var employeegroup = result.get( 0 );

			assertEquals( "a", employeegroup.getId().getGroupName() );
			assertNotNull( employeegroup.getEmployees() );
			assertEquals( 2, employeegroup.getEmployees().size() );
		} );
	}
}
