/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.iterate;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScrollTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "iterate/Item.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "foo" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testScroll() {
		inTransaction(
				s -> {
					Item i1 = new Item( "foo" );
					Item i2 = new Item( "bar" );
					s.persist( "Item", i1 );
					s.persist( "Item", i2 );
				}
		);

		inTransaction(
				s -> {
					ScrollableResults sr = s.getNamedQuery( "Item.nameDesc" ).scroll();
					assertTrue( sr.next() );
					Item i1 = (Item) sr.get();
					assertTrue( sr.next() );
					Item i2 = (Item) sr.get();
					assertTrue( Hibernate.isInitialized( i1 ) );
					assertTrue( Hibernate.isInitialized( i2 ) );
					assertEquals( i1.getName(), "foo" );
					assertEquals( i2.getName(), "bar" );
					assertFalse( sr.next() );
					s.delete( i1 );
					s.delete( i2 );
				}
		);

		assertEquals( sessionFactory().getStatistics().getEntityFetchCount(), 0 );
	}

}
