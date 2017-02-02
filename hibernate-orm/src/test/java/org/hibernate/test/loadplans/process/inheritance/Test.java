/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process.inheritance;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class Test extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Department.class, User.class, Employee.class, Contractor.class };
	}

	@org.junit.Test
	public void basicTest() {
		Session s = openSession();
		s.beginTransaction();
		User user = new User( 2 );
		s.save( user );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		assertNotNull( s.get( User.class, 2 ) );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete User" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
