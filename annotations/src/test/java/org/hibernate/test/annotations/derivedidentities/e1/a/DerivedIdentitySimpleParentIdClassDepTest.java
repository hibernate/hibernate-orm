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

	@FailureExpected( jiraKey = "HHH-4848" )
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "emp", getCfg() ) );
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		e.nickname = "Manu";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		d.name = "Doggy";
		d.emp = e;
		s.persist( d );
		s.flush();
		s.clear();
		DependentId dId = new DependentId();
		dId.name = d.name;
		dId.emp = d.emp.empId;
		d = (Dependent) s.get( Dependent.class, dId );
		assertEquals( e.empId, d.emp.empId );
		assertEquals( e.empName, d.emp.empName );
		assertEquals( e.nickname, d.emp.nickname );
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