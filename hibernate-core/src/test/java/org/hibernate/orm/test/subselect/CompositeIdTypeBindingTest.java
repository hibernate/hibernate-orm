/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Strong Liu
 */
@JiraKey( value = "HHH-8312")
public class CompositeIdTypeBindingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, EmployeeGroup.class };
	}

	@Test
	public void testCompositeTypeBinding() {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// prepare test data
		Session session = openSession();
		session.beginTransaction();

		EmployeeGroup employeegroup = new EmployeeGroup( new EmployeeGroupId( "a", "b" ) );
		employeegroup.addEmployee( new Employee( "stliu" ) );
		employeegroup.addEmployee( new Employee( "david" ) );
		session.persist( employeegroup );

		employeegroup = new EmployeeGroup( new EmployeeGroupId( "c", "d" ) );
		employeegroup.addEmployee( new Employee( "gail" ) );
		employeegroup.addEmployee( new Employee( "steve" ) );
		session.persist( employeegroup );

		session.getTransaction().commit();
		session.close();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Perform the test
		session = openSession();
		session.beginTransaction();

		List<EmployeeGroupId> parameters = new ArrayList<EmployeeGroupId>();
		parameters.add( new EmployeeGroupId( "a", "b" ) );
		parameters.add( new EmployeeGroupId( "c", "d" ) );
		parameters.add( new EmployeeGroupId( "e", "f" ) );

		List result = session.createQuery( "select eg from EmployeeGroup eg where eg.id in (:employeegroupIds)" )
				.setParameterList( "employeegroupIds", parameters ).list();

		Assert.assertEquals( 2, result.size() );

		employeegroup = (EmployeeGroup) result.get( 0 );

		Assert.assertEquals( "a", employeegroup.getId().getGroupName() );
		Assert.assertNotNull( employeegroup.getEmployees() );
		Assert.assertEquals( 2, employeegroup.getEmployees().size() );
		session.getTransaction().commit();
		session.close();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// clean up test data
		session = openSession();
		session.beginTransaction();
		List<EmployeeGroup> egs = session.createQuery( "from EmployeeGroup" ).list();
		for ( EmployeeGroup eg : egs ) {
			eg.getEmployees().clear();
		}
		session.flush();
		session.createQuery( "delete from EmployeeGroup" ).executeUpdate();
		session.createQuery( "delete from Employee" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
