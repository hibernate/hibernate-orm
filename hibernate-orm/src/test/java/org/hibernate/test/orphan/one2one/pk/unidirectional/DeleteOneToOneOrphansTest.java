/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one.pk.unidirectional;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class DeleteOneToOneOrphansTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "orphan/one2one/pk/unidirectional/Mapping.hbm.xml" };
	}

	private void createData() {
		Session session = openSession();
		session.beginTransaction();
		Employee emp = new Employee();
		session.save( emp );
		emp.setInfo( new EmployeeInfo( emp.getId() ) );
		session.getTransaction().commit();
		session.close();
	}

	private void cleanupData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete EmployeeInfo" ).executeUpdate();
		session.createQuery( "delete Employee" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testOrphanedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from EmployeeInfo" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Employee" ).list();
		assertEquals( 1, results.size() );
		Employee emp = ( Employee ) results.get( 0 );
		assertNotNull( emp.getInfo() );
		results = session.createQuery( "from Employee e, EmployeeInfo i where e.info = i" ).list();
		assertEquals( 1, results.size() );
		Object [] result = ( Object [] ) results.get( 0 );
		emp = ( Employee ) result[ 0 ];
		assertNotNull( result[ 1 ] );
		assertSame( emp.getInfo(), result[ 1 ] );
		emp.setInfo( null );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		emp = ( Employee ) session.get( Employee.class, emp.getId() );
		assertNull( emp.getInfo() );
		results = session.createQuery( "from EmployeeInfo" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Employee" ).list();
		assertEquals( 1, results.size() );
		session.getTransaction().commit();
		session.close();

		cleanupData();
	}
}
