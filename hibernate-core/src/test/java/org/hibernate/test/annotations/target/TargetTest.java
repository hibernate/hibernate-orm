/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.target;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel
public class TargetTest extends BaseCoreFunctionalTestCase {
	@Test
	@FailureExpectedWithNewMetamodel
	public void testTargetOnEmbedded() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Luggage l = new LuggageImpl();
		l.setHeight( 12 );
		l.setWidth( 12 );
		Owner o = new OwnerImpl();
		o.setName( "Emmanuel" );
		l.setOwner( o );
		s.persist( l );
		s.flush();
		s.clear();
		l = (Luggage) s.get(LuggageImpl.class, ( (LuggageImpl) l).getId() );
		assertEquals( "Emmanuel", l.getOwner().getName() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testTargetOnMapKey() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Luggage l = new LuggageImpl();
		l.setHeight( 12 );
		l.setWidth( 12 );
		Size size = new SizeImpl();
		size.setName( "S" );
		Owner o = new OwnerImpl();
		o.setName( "Emmanuel" );
		l.setOwner( o );
		s.persist( l );
		Brand b = new Brand();
		s.persist( b );
		b.getLuggagesBySize().put( size, l );
		s.flush();
		s.clear();
		b = (Brand) s.get(Brand.class, b.getId() );
		assertEquals( "S", b.getLuggagesBySize().keySet().iterator().next().getName() );
		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testTargetOnMapKeyManyToMany() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Luggage l = new LuggageImpl();
		l.setHeight( 12 );
		l.setWidth( 12 );
		Size size = new SizeImpl();
		size.setName( "S" );
		Owner o = new OwnerImpl();
		o.setName( "Emmanuel" );
		l.setOwner( o );
		s.persist( l );
		Brand b = new Brand();
		s.persist( b );
		b.getSizePerLuggage().put( l, size );
		s.flush();
		s.clear();
		b = (Brand) s.get(Brand.class, b.getId() );
		assertEquals( 12d, b.getSizePerLuggage().keySet().iterator().next().getWidth(), 0.01 );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { LuggageImpl.class, Brand.class, OwnerImpl.class };
	}
}
