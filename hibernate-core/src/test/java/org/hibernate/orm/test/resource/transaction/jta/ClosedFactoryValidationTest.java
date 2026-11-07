package org.hibernate.orm.test.resource.transaction.jta;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionManager;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionObserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Shutdown validation must abort without running ordinary completion callbacks.
///
/// @author Steve Ebersole
class ClosedFactoryValidationTest {
	@Test
	void preservesValidationFailureWhenRollbackMarkingFails() throws Exception {
		final var owner = mock( TransactionCoordinatorOwner.class );
		final var platform = mock( JtaPlatform.class );
		final var tm = mock( TransactionManager.class );
		final var observer = mock( TransactionObserver.class );
		final var failure = new IllegalStateException( "pending changes" );
		final var markingFailure = new IllegalStateException( "rollback marking failed" );
		when( platform.retrieveTransactionManager() ).thenReturn( tm );
		when( tm.getStatus() ).thenReturn( Status.STATUS_ACTIVE );
		when( platform.canRegisterSynchronization() ).thenReturn( true );
		doThrow( failure ).when( owner ).validateBeforeCompletion();
		doThrow( markingFailure ).when( tm ).setRollbackOnly();
		final var coordinator = new JtaTransactionCoordinatorImpl(
				new JtaTransactionCoordinatorBuilderImpl(), owner, true, platform, false, false, observer );
		coordinator.getTransactionDriverControl();
		final var thrown = assertThrows( IllegalStateException.class,
				() -> coordinator.getSynchronizationCallbackCoordinator().beforeCompletion() );
		assertSame( failure, thrown );
		assertArrayEquals( new Throwable[] { markingFailure }, thrown.getSuppressed() );
		verify( tm ).setRollbackOnly();
		verify( owner, never() ).beforeTransactionCompletion();
		verify( observer, never() ).beforeCompletion();
	}
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void preservesLateFailure(boolean observerFails) throws Exception {
		final var owner = mock( TransactionCoordinatorOwner.class );
		final var platform = mock( JtaPlatform.class );
		final var tm = mock( TransactionManager.class );
		final var observer = mock( TransactionObserver.class );
		final var failure = new IllegalStateException( "factory closed after preflight" );
		final var secondary = new IllegalStateException( "secondary failure" );
		when( platform.retrieveTransactionManager() ).thenReturn( tm );
		when( platform.canRegisterSynchronization() ).thenReturn( true );
		when( tm.getStatus() ).thenReturn( Status.STATUS_ACTIVE );
		when( owner.isActive() ).thenReturn( true );
		if ( observerFails ) {
			doThrow( failure ).when( observer ).beforeCompletion();
		}
		else {
			doThrow( failure ).when( owner ).beforeTransactionCompletion();
			doThrow( secondary ).when( observer ).beforeCompletion();
		}
		final var markingFailure = new IllegalStateException( "rollback marking failed" );
		doThrow( markingFailure ).when( tm ).setRollbackOnly();
		final var coordinator = new JtaTransactionCoordinatorImpl(
				new JtaTransactionCoordinatorBuilderImpl(), owner, true, platform, false, false, observer );
		coordinator.getTransactionDriverControl();
		final var thrown = assertThrows( IllegalStateException.class,
				() -> coordinator.getSynchronizationCallbackCoordinator().beforeCompletion() );
		assertSame( failure, thrown );
		assertArrayEquals( observerFails ? new Throwable[] { markingFailure }
				: new Throwable[] { markingFailure, secondary }, thrown.getSuppressed() );
		verify( tm ).setRollbackOnly();
		verify( observer ).beforeCompletion();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void synchronizationOnlyJoining(boolean validationFails) {
		final var owner = mock( TransactionCoordinatorOwner.class );
		final var platform = mock( JtaPlatform.class );
		final var observer = mock( TransactionObserver.class );
		when( platform.canRegisterSynchronization() ).thenReturn( true );
		final var failure = new IllegalStateException( "factory closed with pending changes" );
		if ( validationFails ) {
			doThrow( failure ).when( owner ).validateBeforeCompletion();
		}
		new JtaTransactionCoordinatorImpl(
				new JtaTransactionCoordinatorBuilderImpl(), owner, true, platform, false, false, observer );
		final var synchronization = ArgumentCaptor.forClass( Synchronization.class );
		verify( platform ).registerSynchronization( synchronization.capture() );
		if ( validationFails ) {
			final var thrown = assertThrows( IllegalStateException.class,
					() -> synchronization.getValue().beforeCompletion() );
			assertSame( failure, thrown );
			assertArrayEquals( new Throwable[0], thrown.getSuppressed() );
		}
		else {
			synchronization.getValue().beforeCompletion();
		}
		verify( platform, never() ).retrieveTransactionManager();
		verify( platform, never() ).retrieveUserTransaction();
		verify( owner, never() ).beforeTransactionCompletion();
		verify( observer, never() ).beforeCompletion();
	}

	@Test
	void assertionErrorStillNotifiesCallbacks() {
		final var owner = mock( TransactionCoordinatorOwner.class );
		final var platform = mock( JtaPlatform.class );
		final var observer = mock( TransactionObserver.class );
		final var synchronization = mock( Synchronization.class );
		final var failure = new AssertionError( "owner failed" );
		when( owner.isActive() ).thenReturn( true );
		doThrow( failure ).when( owner ).beforeTransactionCompletion();
		final var coordinator = new JtaTransactionCoordinatorImpl(
				new JtaTransactionCoordinatorBuilderImpl(), owner, false, platform, false, false, observer );
		coordinator.getLocalSynchronizations().registerSynchronization( synchronization );
		assertSame( failure, assertThrows( AssertionError.class,
				() -> coordinator.getSynchronizationCallbackCoordinator().beforeCompletion() ) );
		verify( synchronization ).beforeCompletion();
		verify( observer ).beforeCompletion();
	}

}
