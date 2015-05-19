/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import org.hibernate.Session;

import org.hibernate.testing.Skip;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@Skip( condition = Skip.AlwaysSkip.class,message = "sdf")
public class DerivedIdentityWithBidirectionalAssociationTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testBidirectionalAssociation() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata() ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata() ) );
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";

		Session s = openSession();
		s.getTransaction().begin();
		s.persist( e );
		Dependent d = new Dependent();
		d.emp = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = getDerivedClassById( e, s, Dependent.class );
		assertEquals( e.empId, d.emp.empId );
		s.getTransaction().rollback();
		s.close();
	}

	private <T> T getDerivedClassById(Employee e, Session s, Class<T> clazz) {
		return clazz.cast( s.createQuery( "from " + clazz.getName() + " d where d.emp.empId = :empId" )
						.setParameter( "empId", e.empId ).uniqueResult() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Dependent.class,
				Employee.class
		};
	}
}
