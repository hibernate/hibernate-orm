package org.hibernate.test.annotations.derivedidentities.e2.a;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentIdClassDepTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testManytoOne() {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", configuration() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", configuration() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", configuration() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "firstName", configuration() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "lastName", configuration() ) );
		Employee e = new Employee();
		e.firstName = "Emmanuel";
		e.lastName = "Bernard";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.name = "Doggy";
		s.persist( d );
		s.flush();
		s.clear();
		DependentId dId = new DependentId();
		EmployeeId eId = new EmployeeId();
		dId.name = d.name;
		dId.emp = eId;
		eId.firstName = e.firstName;
		eId.lastName = e.lastName;
		d = (Dependent) s.get( Dependent.class, dId );
		assertNotNull( d.emp );
		assertEquals( e.firstName, d.emp.firstName );
		s.delete( d );
		s.delete( d.emp );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Employee.class,
				Dependent.class
		};
	}
}
