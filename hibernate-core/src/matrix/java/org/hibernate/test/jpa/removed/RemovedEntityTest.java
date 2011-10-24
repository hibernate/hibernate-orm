/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jpa.removed;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
public class RemovedEntityTest extends AbstractJPATest {
	@Test
	public void testRemoveThenContains() {
		Session s = openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setName( "dummy" );
		s.persist( item );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( item );
		boolean contains = s.contains( item );
		s.getTransaction().commit();
		s.close();

		assertFalse( "expecting removed entity to not be contained", contains );
	}

	@Test
	public void testRemoveThenGet() {
		Session s = openSession();
		s.beginTransaction();
		Item item = new Item();
		item.setName( "dummy" );
		s.persist( item );
		s.getTransaction().commit();
		s.close();

		Long id = item.getId();

		s = openSession();
		s.beginTransaction();
		s.delete( item );
		item = ( Item ) s.get( Item.class, id );
		s.getTransaction().commit();
		s.close();

		assertNull( "expecting removed entity to be returned as null from get()", item );
	}
}
