package org.hibernate.test.annotations.derivedidentities.e2.a;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentIdClassDepTest extends TestCase {

	public void testManytoOne() {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "firstName", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "lastName", getCfg() ) );
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
