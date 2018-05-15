/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e2.b;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentEmbeddedIdDepTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testManyToOne() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_firstName", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_lastName", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "name", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "firstName", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "Dependent", "lastName", metadata() ) );
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
