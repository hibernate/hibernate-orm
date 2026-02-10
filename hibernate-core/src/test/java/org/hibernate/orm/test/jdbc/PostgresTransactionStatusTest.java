/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

import org.hibernate.JDBCException;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test that PostgreSQL transactions are marked as rollback-only when an SQLException occurs.
 */
@DomainModel
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
public class PostgresTransactionStatusTest {
	@Test
	public void testPostgresTransactionStatusOnException(SessionFactoryScope factoryScope) {
		factoryScope.inSession( session -> {
			session.getTransaction().begin();
			// Execute work that throws SQLException
			try {
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "select * from non_existent_table" );
					}
				} );
				fail( "Should have thrown JDBCException" );
			}
			catch (JDBCException e) {
				// Expected
			}

			assertEquals( TransactionStatus.MARKED_ROLLBACK,
					session.getTransaction().getStatus() );

			session.getTransaction().rollback();
		} );
	}

	@Test
	public void testPostgresTransactionStatusOnStatelessSessionException(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessSession( session -> {
			session.getTransaction().begin();
			// Execute work that throws SQLException
			try {
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "select * from non_existent_table" );
					}
				} );
				fail( "Should have thrown JDBCException" );
			}
			catch (JDBCException e) {
				// Expected
			}

			assertEquals( TransactionStatus.MARKED_ROLLBACK,
					session.getTransaction().getStatus() );

			session.getTransaction().rollback();
		} );
	}
}
