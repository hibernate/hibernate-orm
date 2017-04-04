/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jpa.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests using converters with the between construction.
 *
 * @author Etienne Miret
 */
public class ConvertBetweenTest extends AbstractJPATest {

	@Override
	public String[] getMappings() {
		return new String[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Item.class };
	}

	@Before
	public void fillData() {
		final Session s = openSession();
		s.getTransaction().begin();

		final Item i0 = new Item();
		i0.setPrice( new BigDecimal( "12.05" ) );
		i0.setQuantity( 10 );
		s.persist( i0 );

		final Item i1 = new Item();
		i1.setPrice( new BigDecimal( "5.35" ) );
		i1.setQuantity( 5 );
		s.persist( i1 );

		final Item i2 = new Item();
		i2.setPrice( new BigDecimal( "99.99" ) );
		i2.setQuantity( 15 );
		s.persist( i2 );

		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanUpData() {
		final Session s = openSession();
		s.getTransaction().begin();

		s.createQuery( "delete from Item" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9356" )
	public void testBetweenLiteral() {
		final Session s = openSession();
		s.getTransaction().begin();

		@SuppressWarnings("unchecked")
		final List<Item> result = s.createQuery( "from Item where quantity between 9 and 11" ).list();
		assertEquals( 1, result.size() );
		assertEquals( 10, result.get( 0 ).getQuantity().intValue() );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testBetweenParameters() {
		final Session s = openSession();
		s.getTransaction().begin();

		final Query query = s.createQuery( "from Item where quantity between :low and :high" );
		query.setParameter( "low", new Integer( 9 ) );
		query.setParameter( "high", new Integer( 11 ) );
		@SuppressWarnings("unchecked")
		final List<Item> result = query.list();
		assertEquals( 1, result.size() );
		assertEquals( 10, result.get( 0 ).getQuantity().intValue() );

		s.getTransaction().commit();
		s.close();
	}

}
