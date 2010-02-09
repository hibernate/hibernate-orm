package org.hibernate.test.annotations.derivedidentities.e1.a;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

import java.util.List;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentIdClassDepTest extends TestCase {

	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "emp", getCfg() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Employee e = new Employee( 1L, "Emmanuel", "Manu" );
		Dependent d = new Dependent( "Doggy", e );
		s.persist( d );
		s.persist( e );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		DependentId dId = new DependentId( d.getName(), d.getEmp().empId );
		d = (Dependent) s.get( Dependent.class, dId );
		assertEquals( e.empId, d.getEmp().empId );
		assertEquals( e.empName, d.getEmp().empName );
		assertEquals( e.nickname, d.getEmp().nickname );
		s.delete( d );
		s.delete( d.getEmp() );
		s.getTransaction().commit();
		s.close();
	}

	public void testQueryNewEntityInPC() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Employee e = new Employee( 1L, "Paula", "P" );
		Dependent d = new Dependent( "LittleP", e );
		d.setEmp(e);
		s.persist( d );
		s.persist( e );

		// the following would work
		// List depList = s.createQuery("Select d from Dependent d where d.name='LittleP'").list();

		// the following query is not finding the entity 'd' added above
		List depList = s.createQuery("Select d from Dependent d where d.name='LittleP' and d.emp.name='Paula'").list();
		Object newDependent = null;
		if (depList.size() > 0) {
			 newDependent = (Dependent) depList.get(0);
		}
		if (newDependent != d) {
			fail("PC entity instance (" + d +") does not match returned query result value (" + newDependent);
		}
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