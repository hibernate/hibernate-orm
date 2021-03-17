/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.joined;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassAndSecondaryTable extends BaseCoreFunctionalTestCase {

	@Test
	public void testSecondaryTableAndJoined() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		SwimmingPool sp = new SwimmingPool();
		s.persist( sp );
		s.flush();
		s.clear();

		long rowCount = getTableRowCount( s, "POOL_ADDRESS" );
		assertEquals(
				"The address table is marked as optional. For null values no database row should be created",
				0,
				rowCount
		);

		SwimmingPool sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		assertNull( sp.getAddress() );

		PoolAddress address = new PoolAddress();
		address.setAddress( "Park Avenue" );
		sp2.setAddress( address );

		s.flush();
		s.clear();

		sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		rowCount = getTableRowCount( s, "POOL_ADDRESS" );
		assertEquals(
				"Now we should have a row in the pool address table ",
				1,
				rowCount
		);
		assertNotNull( sp2.getAddress() );
		assertEquals( sp2.getAddress().getAddress(), "Park Avenue" );

		tx.rollback();
		s.close();
	}

	@Test
	public void testSecondaryTableAndJoinedInverse() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		SwimmingPool sp = new SwimmingPool();
		s.persist( sp );
		s.flush();
		s.clear();

		long rowCount = getTableRowCount( s, "POOL_ADDRESS_2" );
		assertEquals(
				"The address table is marked as optional. For null values no database row should be created",
				0,
				rowCount
		);

		SwimmingPool sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		assertNull( sp.getSecondaryAddress() );

		PoolAddress address = new PoolAddress();
		address.setAddress( "Park Avenue" );
		sp2.setSecondaryAddress( address );

		s.flush();
		s.clear();

		sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		rowCount = getTableRowCount( s, "POOL_ADDRESS_2" );
		assertEquals(
				"Now we should have a row in the pool address table ",
				0,
				rowCount
		);
		assertNull( sp2.getSecondaryAddress()  );

		tx.rollback();
		s.close();
	}

	private long getTableRowCount(Session s, String tableName) {
		// the type returned for count(*) in a native query depends on the dialect
		// Oracle returns Types.NUMERIC, which is mapped to BigDecimal;
		// H2 returns Types.BIGINT, which is mapped to BigInteger;
		Object retVal = s.createSQLQuery( "select count(*) from " + tableName ).uniqueResult();
		assertTrue( Number.class.isInstance( retVal ) );
		return ( ( Number ) retVal ).longValue();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Pool.class, SwimmingPool.class };
	}
}
