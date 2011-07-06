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
package org.hibernate.test.annotations.inheritance.joined;

import java.math.BigInteger;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassAndSecondaryTable extends BaseCoreFunctionalTestCase {
	@Test
	public void testSecondaryTableAndJoined() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		SwimmingPool sp = new SwimmingPool();
		s.persist( sp );
		s.flush();
		s.clear();

		BigInteger rowCount = getTableRowCount( s );
		assertEquals(
				"The address table is marked as optional. For null values no database row should be created",
				BigInteger.valueOf( 0 ),
				rowCount
		);

		SwimmingPool sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		assertEquals( sp.getAddress(), null );

		PoolAddress address = new PoolAddress();
		address.setAddress( "Park Avenue" );
		sp2.setAddress( address );

		s.flush();
		s.clear();

		sp2 = (SwimmingPool) s.get( SwimmingPool.class, sp.getId() );
		rowCount = getTableRowCount( s );
		assertEquals(
				"Now we should have a row in the pool address table ",
				BigInteger.valueOf( 1 ),
				rowCount
		);
		assertFalse( sp2.getAddress() == null );
		assertEquals( sp2.getAddress().getAddress(), "Park Avenue" );

		tx.rollback();
		s.close();
	}

	private BigInteger getTableRowCount(Session s) {
		return (BigInteger) s.createSQLQuery( "select count(*) from POOL_ADDRESS" ).uniqueResult();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Pool.class, SwimmingPool.class };
	}
}
