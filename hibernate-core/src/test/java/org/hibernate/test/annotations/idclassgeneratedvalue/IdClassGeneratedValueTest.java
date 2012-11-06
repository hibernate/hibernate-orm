// $Id$
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
package org.hibernate.test.annotations.idclassgeneratedvalue;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * A test.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
public class IdClassGeneratedValueTest extends BaseCoreFunctionalTestCase {
	@Test
	@SuppressWarnings({ "unchecked" })
	public void testBaseLine() {
		Session s = openSession();
		s.beginTransaction();
		Simple s1 = new Simple( 1L, 2L, 10 );
		s.persist( s1 );
		Simple s2 = new Simple( 2L, 3L, 20 );
		s.persist( s2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Simple> simpleList = s.createQuery( "select s from Simple s" ).list();
		assertEquals( simpleList.size(), 2 );
		s1 = ( Simple ) s.load( Simple.class, new SimplePK( 1L, 2L ) );
		assertEquals( s1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Simple" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	@FailureExpectedWithNewMetamodel
	public void testSingleGeneratedValue() {
		Session s = openSession();
		s.beginTransaction();
		Simple2 s1 = new Simple2( 200L, 10 );
		s.persist( s1 );
		Long s1Id1 = s1.getId1();
		Simple2 s2 = new Simple2( 300L, 20 );
		s.persist( s2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Simple2> simpleList = s.createQuery( "select s from Simple2 s" ).list();
		assertEquals( simpleList.size(), 2 );
		s1 = ( Simple2 ) s.load( Simple2.class, new SimplePK( s1Id1, 200L ) );
		assertEquals( s1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Simple2" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	@FailureExpectedWithNewMetamodel
	public void testMultipleGeneratedValue() {
		Session s = openSession();
		s.beginTransaction();
		Multiple m1 = new Multiple( 1000L, 10 );
		s.persist( m1 );
		Long m1Id1 = m1.getId1();
		Long m1Id2 = m1.getId2();
		Multiple m2 = new Multiple( 2000L, 20 );
		s.persist( m2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		List<Multiple> simpleList = s.createQuery( "select m from Multiple m" ).list();
		assertEquals( simpleList.size(), 2 );
		m1 = ( Multiple ) s.load( Multiple.class, new MultiplePK( m1Id1, m1Id2, 1000L ) );
		assertEquals( m1.getQuantity(), 10 );
		s.clear();
		s.createQuery( "delete Multiple" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Simple.class,
				Simple2.class,
				Multiple.class

		};
	}
}
