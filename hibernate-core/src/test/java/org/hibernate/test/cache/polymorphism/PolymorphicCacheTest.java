/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012-2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache.polymorphism;

import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-9028")
public class PolymorphicCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AbstractCachedItem.class, CachedItem1.class, CachedItem2.class };
	}

	@Test
	public void testPolymorphismAndCache() throws Exception {
		final CachedItem1 cachedItem1 = new CachedItem1( "name 1" );
		final CachedItem2 cachedItem2 = new CachedItem2( "name 2" );

		// create the 2 items
		Session s = openSession();
		s.beginTransaction();
		s.save( cachedItem1 );
		s.save( cachedItem2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// As the first item is supposed to be a CachedItem1, it shouldn't be returned.
		// Note that the Session API is not type safe but, when using the EntityManager.find API, you get a ClassCastException
		// if calling find returns the object.
		Object thisObjectShouldBeNull = s.get( CachedItem2.class, cachedItem1.getId() );
		assertNull( thisObjectShouldBeNull );
		s.getTransaction().commit();
		s.close();

		// cleanup
		s = openSession();
		s.beginTransaction();
		s.delete( cachedItem1 );
		s.delete( cachedItem2 );
		s.getTransaction().commit();
		s.close();
	}

}
