/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Pool.class,
				SwimmingPool.class
		}
)
@SessionFactory
public class JoinedSubclassAndSecondaryTable {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSecondaryTableAndJoined(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SwimmingPool sp = new SwimmingPool();
					session.persist( sp );
					session.flush();
					session.clear();

//					long rowCount = getTableRowCount( session, "POOL_ADDRESS" );
//					assertEquals(
//
//							0,
//							rowCount,
//							"The address table is marked as optional. For null values no database row should be created"
//					);

					assertNumberOfRows(
							session,
							"POOL_ADDRESS",
							0,
							"The address table is marked as optional. For null values no database row should be created"
					);

					SwimmingPool sp2 = session.get( SwimmingPool.class, sp.getId() );
					assertNull( sp.getAddress() );

					PoolAddress address = new PoolAddress();
					address.setAddress( "Park Avenue" );
					sp2.setAddress( address );

					session.flush();
					session.clear();

					sp2 = session.get( SwimmingPool.class, sp.getId() );
//					rowCount = getTableRowCount( session, "POOL_ADDRESS" );
//					assertEquals(
//
//							1,
//							rowCount,
//							"Now we should have a row in the pool address table "
//
//					);
					assertNumberOfRows(
							session,
							"POOL_ADDRESS",
							1,
							"Now we should have a row in the pool address table"
					);
					assertNotNull( sp2.getAddress() );
					assertEquals( "Park Avenue", sp2.getAddress().getAddress() );
				}
		);
	}

	@Test
	public void testSecondaryTableAndJoinedInverse(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SwimmingPool sp = new SwimmingPool();
					session.persist( sp );
					session.flush();
					session.clear();

//					long rowCount = getTableRowCount( session, "POOL_ADDRESS_2" );
//					assertEquals(
//							0,
//							rowCount,
//							"The address table is marked as optional. For null values no database row should be created"
//					);
					assertNumberOfRows(
							session,
							"POOL_ADDRESS_2",
							0,
							"The address table is marked as optional. For null values no database row should be created"
					);

					SwimmingPool sp2 = session.get( SwimmingPool.class, sp.getId() );
					assertNull( sp.getSecondaryAddress() );

					PoolAddress address = new PoolAddress();
					address.setAddress( "Park Avenue" );
					sp2.setSecondaryAddress( address );

					session.flush();
					session.clear();

					sp2 = session.get( SwimmingPool.class, sp.getId() );
//					rowCount = getTableRowCount( session, "POOL_ADDRESS_2" );
//					assertEquals(
//
//							0,
//							rowCount,
//							"Now we should have a row in the pool address table "
//					);
					assertNumberOfRows( session,
										"POOL_ADDRESS_2",
										0,
										"Now we should not have a row in the pool address table " );
					assertNull( sp2.getSecondaryAddress() );
				}
		);
	}

	private void assertNumberOfRows(Session s, String tableName, int expectedNumberOfRows, String message) {
		s.doWork(
				work -> {
					try (PreparedStatement preparedStatement = work.prepareStatement( "select count(*) as count_of_rows from " + tableName )) {
						preparedStatement.execute();
						ResultSet resultSet = preparedStatement.getResultSet();
						resultSet.next();
						long rowCount = resultSet.getLong( "count_of_rows" );
						assertEquals(
								expectedNumberOfRows,
								rowCount,
								message
						);
					}
				}
		);
	}

	private long getTableRowCount(Session s, String tableName) {
		// the type returned for count(*) in a native query depends on the dialect
		// Oracle returns Types.NUMERIC, which is mapped to BigDecimal;
		// H2 returns Types.BIGINT, which is mapped to BigInteger;
		Object retVal = s.createNativeQuery( "select count(*) from " + tableName ).uniqueResult();
		assertTrue( Number.class.isInstance( retVal ) );
		return ( (Number) retVal ).longValue();
	}
}
