/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.target;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class TargetTest extends BaseCoreFunctionalTestCase {
	@Test
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
		return new Class[] { LuggageImpl.class, Brand.class };
	}
}
