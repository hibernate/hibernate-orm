/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class NamedQueryTransactionFailureTest extends EntityManagerFactoryBasedFunctionalTest {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Item.class,
				Distributor.class,
				Wallet.class
		};
	}

	private TransactionCoordinator transactionCoordinator;

	@SuppressWarnings( {"unchecked"})
	protected void addConfigOptions(Map options) {
		TransactionCoordinatorBuilder transactionCoordinatorBuilder = Mockito.mock( TransactionCoordinatorBuilder.class);
		when(transactionCoordinatorBuilder.getDefaultConnectionHandlingMode())
		.thenReturn( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD );

		when(transactionCoordinatorBuilder.isJta())
		.thenReturn( false );

		transactionCoordinator = Mockito.mock( TransactionCoordinator.class);

		when(transactionCoordinatorBuilder.buildTransactionCoordinator(any(), any( TransactionCoordinatorBuilder.Options.class)))
		.thenReturn( transactionCoordinator );

		when( transactionCoordinator.getTransactionCoordinatorBuilder() ).thenReturn( transactionCoordinatorBuilder );

		TransactionCoordinator.TransactionDriver transactionDriver = Mockito.mock( TransactionCoordinator.TransactionDriver.class);
		when( transactionCoordinator.getTransactionDriverControl() ).thenReturn( transactionDriver );
		when( transactionCoordinator.isActive() ).thenReturn( true );
		when( transactionDriver.isActive() ).thenReturn( false );

		doNothing().when( transactionCoordinator ).pulse();

		options.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, transactionCoordinatorBuilder );
	}

	@Override
	protected boolean exportSchema() {
		return false;
	}

	@Test
	@JiraKey( value = "HHH-11997" )
	public void testNamedQueryWithMarkForRollbackOnlyFailure() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				try {
					Mockito.reset( transactionCoordinator );
					doThrow(MarkedForRollbackException.class).when( transactionCoordinator ).pulse();

					entityManager.createNamedQuery( "NamedQuery" );
				}
				catch (Exception e) {
					assertEquals( HibernateException.class, e.getClass() );
					assertEquals( MarkedForRollbackException.class, e.getCause().getClass() );
				}
			});
		}
		catch (Exception ignore) {
		}
	}

	public static class MarkedForRollbackException extends RuntimeException {
	}
}
