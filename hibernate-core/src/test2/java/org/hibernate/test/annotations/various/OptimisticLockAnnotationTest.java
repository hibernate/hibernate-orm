/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Test for the @OptimisticLock annotation.
 *
 * @author Emmanuel Bernard
 */
public class OptimisticLockAnnotationTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOptimisticLockExcludeOnNameProperty() throws Exception {
		Conductor c = new Conductor();
		c.setName( "Bob" );
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( c );
		s.flush();

		s.clear();

		c = ( Conductor ) s.get( Conductor.class, c.getId() );
		Long version = c.getVersion();
		c.setName( "Don" );
		s.flush();

		s.clear();

		c = ( Conductor ) s.get( Conductor.class, c.getId() );
		assertEquals( version, c.getVersion() );

		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Conductor.class
		};
	}
}
