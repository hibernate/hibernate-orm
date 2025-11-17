/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import org.hibernate.resource.transaction.backend.jta.internal.JtaPlatformInaccessibleException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class InaccessibleJtaPlatformTests {
	private final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
	private JtaTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder = new JtaTransactionCoordinatorBuilderImpl();

	@Test
	public void testInaccessibleTransactionManagerHandling() {
		// first, have JtaPlatform throw an exception
		try {
			final JtaPlatformInaccessibleImpl jtaPlatform = new JtaPlatformInaccessibleImpl( true );
			final TransactionCoordinator transactionCoordinator = new JtaTransactionCoordinatorImpl(
					transactionCoordinatorBuilder,
					owner,
					true,
					jtaPlatform,
					false,
					false
			);

			transactionCoordinator.getTransactionDriverControl().begin();

			fail( "Expecting JtaPlatformInaccessibleException, but call succeeded" );
		}
		catch (JtaPlatformInaccessibleException expected) {
			// expected condition
		}
		catch (Exception e) {
			fail( "Expecting JtaPlatformInaccessibleException, but got " + e.getClass().getName() );
		}


		// then, have it return null
		try {
			final JtaPlatformInaccessibleImpl jtaPlatform = new JtaPlatformInaccessibleImpl( false );
			final TransactionCoordinator transactionCoordinator = new JtaTransactionCoordinatorImpl(
					transactionCoordinatorBuilder,
					owner,
					true,
					jtaPlatform,
					false,
					false
			);

			transactionCoordinator.getTransactionDriverControl().begin();

			fail( "Expecting JtaPlatformInaccessibleException, but call succeeded" );
		}
		catch (JtaPlatformInaccessibleException expected) {
			// expected condition
		}
		catch (Exception e) {
			fail( "Expecting JtaPlatformInaccessibleException, but got " + e.getClass().getName() );
		}
	}

	@Test
	public void testInaccessibleUserTransactionHandling() {
		// first, have JtaPlatform throw an exception
		try {
			final JtaPlatformInaccessibleImpl jtaPlatform = new JtaPlatformInaccessibleImpl( true );
			final TransactionCoordinator transactionCoordinator = new JtaTransactionCoordinatorImpl(
					transactionCoordinatorBuilder,
					owner,
					true,
					jtaPlatform,
					false,
					false
			);

			transactionCoordinator.getTransactionDriverControl().begin();

			fail( "Expecting JtaPlatformInaccessibleException, but call succeeded" );
		}
		catch (JtaPlatformInaccessibleException expected) {
			// expected condition
		}
		catch (Exception e) {
			fail( "Expecting JtaPlatformInaccessibleException, but got " + e.getClass().getName() );
		}


		// then, have it return null
		try {
			final JtaPlatformInaccessibleImpl jtaPlatform = new JtaPlatformInaccessibleImpl( false );
			final TransactionCoordinator transactionCoordinator = new JtaTransactionCoordinatorImpl(
					transactionCoordinatorBuilder,
					owner,
					true,
					jtaPlatform,
					false,
					false
			);

			transactionCoordinator.getTransactionDriverControl().begin();

			fail( "Expecting JtaPlatformInaccessibleException, but call succeeded" );
		}
		catch (JtaPlatformInaccessibleException expected) {
			// expected condition
		}
		catch (Exception e) {
			fail( "Expecting JtaPlatformInaccessibleException, but got " + e.getClass().getName() );
		}
	}
}
