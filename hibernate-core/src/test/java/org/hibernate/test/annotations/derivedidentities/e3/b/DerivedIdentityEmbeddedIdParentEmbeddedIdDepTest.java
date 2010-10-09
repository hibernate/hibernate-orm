package org.hibernate.test.annotations.derivedidentities.e3.b;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityEmbeddedIdParentEmbeddedIdDepTest extends TestCase {
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK2", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "dep_name", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "firstName", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "lastName", getCfg() ) );
		Employee e = new Employee();
		e.empId = new EmployeeId();
		e.empId.firstName = "Emmanuel";
		e.empId.lastName = "Bernard";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.id = new DependentId();
		d.id.name = "Doggy";
		s.persist( d );
		s.flush();
		s.clear();
		d = (Dependent) s.get( Dependent.class, d.id );
		assertNotNull( d.emp );
		assertEquals( e.empId.firstName, d.emp.empId.firstName );
		s.getTransaction().rollback();
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
