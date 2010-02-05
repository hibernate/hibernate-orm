package org.hibernate.test.annotations.derivedidentities.e1.a;

import org.hibernate.Session;
import org.hibernate.junit.FailureExpected;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class
		DerivedIdentitySimpleParentIdClassDepTest extends TestCase {

	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "emp", getCfg() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		e.nickname = "Manu";
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.name = "Doggy";
		d.emp = e;
		s.persist( d );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		DependentId dId = new DependentId( d.name, d.emp.empId );
		d = (Dependent) s.get( Dependent.class, dId );
		assertEquals( e.empId, d.emp.empId );
		assertEquals( e.empName, d.emp.empName );
		assertEquals( e.nickname, d.emp.nickname );
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