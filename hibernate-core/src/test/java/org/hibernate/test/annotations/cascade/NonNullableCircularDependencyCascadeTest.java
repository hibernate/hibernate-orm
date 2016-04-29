/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.TransientPropertyValueException;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Jeff Schnitzer
 * @author Gail Badner
 */
@SuppressWarnings("unchecked")
public class NonNullableCircularDependencyCascadeTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testIdClassInSuperclass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Parent p = new Parent();
		p.setChildren( new HashSet<Child>() );

		Child ch = new Child(p);
		p.getChildren().add(ch);
		p.setDefaultChild(ch);

		try {
			s.persist(p);
			s.flush();
			fail( "should have failed because of transient entities have non-nullable, circular dependency." );
		}
		catch (IllegalStateException ex) {
			// expected
			assertThat( ex.getCause(), instanceOf( TransientPropertyValueException.class ) );
		}
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Child.class,
				Parent.class
		};
	}
}
