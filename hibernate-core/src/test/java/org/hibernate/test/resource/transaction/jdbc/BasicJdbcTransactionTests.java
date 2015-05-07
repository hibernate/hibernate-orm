/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.resource.transaction.jdbc;

import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;

import org.hibernate.test.resource.common.SynchronizationCollectorImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicJdbcTransactionTests {

	@Test
	public void basicUsageTest() throws Exception {
		final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
		final JdbcResourceLocalTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder =
				new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

		final TransactionCoordinator transactionCoordinator = transactionCoordinatorBuilder.buildTransactionCoordinator(
				owner,
				new TransactionCoordinatorBuilder.TransactionCoordinatorOptions() {
					@Override
					public boolean shouldAutoJoinTransaction() {
						return false;
					}
				}
		);

		SynchronizationCollectorImpl sync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( sync );

		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( 0, sync.getBeforeCompletionCount() );
		assertEquals( 0, sync.getSuccessfulCompletionCount() );
		assertEquals( 0, sync.getFailedCompletionCount() );

		transactionCoordinator.getTransactionDriverControl().commit();
		assertEquals( 1, sync.getBeforeCompletionCount() );
		assertEquals( 1, sync.getSuccessfulCompletionCount() );
		assertEquals( 0, sync.getFailedCompletionCount() );

	}
}
