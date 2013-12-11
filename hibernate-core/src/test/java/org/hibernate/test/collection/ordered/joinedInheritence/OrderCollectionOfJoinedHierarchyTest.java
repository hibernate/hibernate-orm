/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.ordered.joinedInheritence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class OrderCollectionOfJoinedHierarchyTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Animal.class, Lion.class, Tiger.class, Zoo.class };
	}

	@Test
	public void testQuerySyntaxCheck() {
		Session session = openSession();
		session.beginTransaction();
		session.get( Zoo.class, 1L );
		session.getTransaction().commit();
		session.close();
	}
	
	@Test
	public void testOrdering() {
		Zoo zoo = new Zoo();
		Lion lion1 = new Lion();
		lion1.setWeight( 2 );
		Lion lion2 = new Lion();
		lion2.setWeight( 1 );
		zoo.getLions().add( lion1 );
		zoo.getLions().add( lion2 );
		zoo.getAnimalsById().add( lion1 );
		zoo.getAnimalsById().add( lion2 );
		
		Session session = openSession();
		session.beginTransaction();
		session.persist( lion1 );
		session.persist( lion2 );
		session.persist( zoo );
		session.getTransaction().commit();
		session.clear();
		
		session.beginTransaction();
		zoo = (Zoo) session.get( Zoo.class, zoo.getId() );
		zoo.getLions().size();
		zoo.getLions().size();
		zoo.getAnimalsById().size();
		session.getTransaction().commit();
		session.close();
		
		assertNotNull( zoo );
		assertTrue( CollectionHelper.isNotEmpty( zoo.getLions() ) && zoo.getLions().size() == 2 );
		assertTrue( CollectionHelper.isNotEmpty( zoo.getAnimalsById() ) && zoo.getAnimalsById().size() == 2 );
		assertEquals( zoo.getLions().iterator().next().getId(), lion2.getId() );
		assertEquals( zoo.getAnimalsById().iterator().next().getId(), lion1.getId() );
	}
}
