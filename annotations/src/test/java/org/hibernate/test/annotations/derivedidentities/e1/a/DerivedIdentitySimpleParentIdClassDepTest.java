package org.hibernate.test.annotations.derivedidentities.e1.a;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class
		DerivedIdentitySimpleParentIdClassDepTest extends TestCase {

	public void testManyToOne() throws Exception {
//		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "FK", getCfg() ) );
//		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "empPK", getCfg() ) );
//		Employee e = new Employee();
//		e.empId = 1;
//		e.empName = "Emmanuel";
//		Session s = openSession(  );
//		s.getTransaction().begin();
//		s.persist( e );
//		Dependent d = new Dependent();
//		d.emp = e;
//		d.name = "Doggy";
//		d.emp = e;
//		s.persist( d );
//		s.flush();
//		s.clear();
//		DependentId dId = new DependentId();
//		dId.name = d.name;
//		dId.empPK = d.emp.empId;
//		d = (Dependent) s.get( Dependent.class, dId );
//		assertEquals( e.empId, d.emp.empId );
//		s.getTransaction().rollback();
//		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				//Dependent.class,
				//Employee.class
		};
	}
}