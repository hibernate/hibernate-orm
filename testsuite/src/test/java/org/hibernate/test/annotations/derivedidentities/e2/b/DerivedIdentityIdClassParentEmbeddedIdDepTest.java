package org.hibernate.test.annotations.derivedidentities.e2.b;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentEmbeddedIdDepTest extends TestCase {

	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_firstName", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_lastName", getCfg() ) );
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
		d.id = new DependentId();
		d.id.name = "Doggy";
		s.persist( d );
		s.flush();
		s.clear();
		d = (Dependent) s.get( Dependent.class, d.id );
		assertNotNull( d.emp );
		assertEquals( e.firstName, d.emp.firstName );
		s.getTransaction().rollback();
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
