/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

//$Id$
package org.hibernate.test.annotations.various;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;

/**
 * Test for the @OptimisticLock annotation.
 *
 * @author Emmanuel Bernard
 */
public class OptimisticLockAnnotationTest extends TestCase {

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

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Conductor.class
		};
	}
}
