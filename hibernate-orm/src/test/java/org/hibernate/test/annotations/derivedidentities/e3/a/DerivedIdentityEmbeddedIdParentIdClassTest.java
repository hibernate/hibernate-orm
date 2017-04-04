/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e3.a;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityEmbeddedIdParentIdClassTest  extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "dep_name", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "firstName", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "lastName", metadata() ) );

		Employee e = new Employee();
		e.empId = new EmployeeId();
		e.empId.firstName = "Emmanuel";
		e.empId.lastName = "Bernard";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.name = "Doggy";
		DependentId dId = new DependentId();
		dId.emp = new EmployeeId();
		dId.emp.firstName = e.empId.firstName;
		dId.emp.lastName = e.empId.lastName;
		dId.name = d.name;
		s.persist( d );
		s.flush();
		s.clear();
		d = (Dependent) s.get( Dependent.class, dId );
		assertNotNull( d.emp );
		assertEquals( e.empId.firstName, d.emp.empId.firstName );
		s.delete( d );
		s.delete( d.emp );
		s.getTransaction().commit();
		s.close();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Dependent.class,
				Employee.class
		};
	}
}
