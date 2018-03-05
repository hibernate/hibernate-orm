/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.jpa.test;

import java.util.Map;
import javax.persistence.Query;

import org.hibernate.cfg.Environment;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.mockito.Mockito;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class NamedQueryTransactionFailureTest extends BaseEntityManagerFunctionalTestCase {
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
		when( transactionDriver.isActive( anyBoolean() ) ).thenReturn( false );

		doNothing().when( transactionCoordinator ).pulse();

		options.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, transactionCoordinatorBuilder );
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11997" )
	public void testNamedQueryWithTransactionSynchStatus() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				try {
					Mockito.reset( transactionCoordinator );
					doThrow(IllegalStateException.class).when( transactionCoordinator ).pulse();

					entityManager.createNamedQuery( "NamedQuery" );
				}
				catch (Exception e) {
					assertEquals(IllegalArgumentException.class, e.getClass());
					assertEquals(IllegalStateException.class, e.getCause().getClass());
				}
			});
		}
		catch (Exception ignore) {
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11997" )
	public void testNamedQueryWithMarkForRollbackOnlyFailure() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				try {
					Mockito.reset( transactionCoordinator );
					doNothing().
					doThrow(IllegalStateException.class).when( transactionCoordinator ).pulse();

					entityManager.createNamedQuery( "NamedQuery" );
				}
				catch (Exception e) {
					assertEquals(IllegalArgumentException.class, e.getClass());
					assertEquals(IllegalStateException.class, e.getCause().getClass());
				}
			});
		}
		catch (Exception ignore) {
		}
	}
}
