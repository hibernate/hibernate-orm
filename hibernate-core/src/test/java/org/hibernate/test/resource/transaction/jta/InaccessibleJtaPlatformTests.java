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
package org.hibernate.test.resource.transaction.jta;

import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.backend.jta.internal.JtaPlatformInaccessibleException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

import org.junit.Test;

import static org.junit.Assert.fail;

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
