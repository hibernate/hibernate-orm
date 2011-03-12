package org.hibernate.test.annotations.derivedidentities.e1.b;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class
		DerivedIdentitySimpleParentEmbeddedIdDepTest extends TestCase {

	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "empPK", getCfg() ) );
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		Session s = openSession(  );
		s.getTransaction().begin();

		Dependent d = new Dependent();
		d.emp = e;
		d.id = new DependentId();
		d.id.name = "Doggy";
		s.persist( d );
		s.persist( e );
		s.flush();
		s.clear();
		d = (Dependent) s.get( Dependent.class, d.id );
		assertEquals( d.id.empPK, d.emp.empId );
		s.getTransaction().rollback();
		s.close();
	}

	public void testOneToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "ExclusiveDependent", "FK", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "ExclusiveDependent", "empPK", getCfg() ) );
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		ExclusiveDependent d = new ExclusiveDependent();
		d.emp = e;
		d.id = new DependentId();
		d.id.name = "Doggy";
		//d.id.empPK = e.empId; //FIXME not needed when foreign is enabled
		s.persist( d );
		s.flush();
		s.clear();
		d = (ExclusiveDependent) s.get( ExclusiveDependent.class, d.id );
		assertEquals( d.id.empPK, d.emp.empId );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Dependent.class,
				Employee.class,
				ExclusiveDependent.class
		};
	}
}
