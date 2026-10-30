package org.hibernate.orm.test.locking;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.UnstructuredCacheEntry;
import org.hibernate.engine.internal.EntityEntryImpl;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.Jira;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_FORCE_INCREMENT;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Verifies acquisition-before-increment ordering, snapshot handling and the
/// transfer of cache soft-lock ownership to transaction completion.
///
/// @author Steve Ebersole
@Jira("https://hibernate.atlassian.net/browse/HHH-21031")
class ForceIncrementExecutionTest {
	@ParameterizedTest
	@EnumSource(value = LockMode.class, names = { "READ", "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	void extendedAcquisitionPrecedesIncrementWithoutChangingCallerOptions(LockMode held) {
		final var f = new Fixture( false );
		when( f.entry.getLockMode() ).thenReturn( held );
		final var options = new LockOptions( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED )
				.setTimeout( Timeout.milliseconds( 750 ) ).setFollowOnStrategy( Locking.FollowOn.DISALLOW );
		doAnswer( invocation -> {
			final var actual = invocation.getArgument( 3, LockOptions.class );
			assertThat( actual ).isNotSameAs( options );
			assertThat( actual.getLockMode() ).isEqualTo( PESSIMISTIC_WRITE );
			assertThat( actual.getLockScope() ).isEqualTo( EXTENDED );
			assertThat( actual.getTimeout() ).isEqualTo( Timeout.milliseconds( 750 ) );
			assertThat( actual.getFollowOnStrategy() ).isEqualTo( Locking.FollowOn.DISALLOW );
			verify( f.entry, never() ).setLockMode( any() );
			verify( f.persister, never() ).forceVersionIncrement( any(), any(), any() );
			return null;
		} ).when( f.persister ).lock( eq( 1 ), eq( 0 ), same( f.entity ), any( LockOptions.class ), same( f.session ) );

		LoaderHelper.upgradeLock( f.entity, f.entry, options, f.session );

		assertThat( options.getLockMode() ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
		assertThat( options.getLockScope() ).isEqualTo( EXTENDED );
		assertThat( options.getTimeout() ).isEqualTo( Timeout.milliseconds( 750 ) );
		assertThat( options.getFollowOnStrategy() ).isEqualTo( Locking.FollowOn.DISALLOW );
		final var order = inOrder( f.persister, f.entry, f.monitor );
		order.verify( f.persister ).lock( eq( 1 ), eq( 0 ), same( f.entity ), any( LockOptions.class ), same( f.session ) );
		order.verify( f.monitor ).completeEntityLockEvent( null, 1, "Item", PESSIMISTIC_WRITE, true, f.session );
		order.verify( f.persister ).forceVersionIncrement( 1, 0, f.session );
		order.verify( f.entry ).forceLocked( f.entity, 1 );
		order.verify( f.entry ).setLockMode( PESSIMISTIC_FORCE_INCREMENT );
	}

	@ParameterizedTest
	@CsvSource({ "NORMAL, true", "EXTENDED, false" })
	void normalScopeAndPendingInsertKeepTheirExistingIncrementPath(PessimisticLockScope scope, boolean exists) {
		final var f = new Fixture( false );
		when( f.entry.isExistsInDatabase() ).thenReturn( exists );
		LoaderHelper.upgradeLock( f.entity, f.entry,
				new LockOptions( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( scope ), f.session );
		verify( f.persister ).forceVersionIncrement( 1, 0, f.session );
		verify( f.persister, never() ).lock( any(), any(), any(), any( LockOptions.class ), any() );
		verifyNoInteractions( f.monitor );
	}

	@Test
	void unsupportedForceIncrementIsNotNormalizedToWrite() {
		final var f = new Fixture( false );
		when( f.persister.isVersioned() ).thenReturn( false );
		final var options = new LockOptions( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED );
		final var failure = new HibernateException( "unversioned" );
		doThrow( failure ).when( f.persister ).lock( 1, 0, f.entity, options, f.session );
		assertThatThrownBy( () -> LoaderHelper.upgradeLock( f.entity, f.entry, options, f.session ) ).isSameAs( failure );
		verify( f.persister, never() ).forceVersionIncrement( any(), any(), any() );
	}

	@Test
	void failedScopeAcquisitionDoesNotStartIncrement() {
		final var f = new Fixture( false );
		final var failure = new IllegalStateException( "acquisition failure" );
		doThrow( failure ).when( f.persister ).lock( any(), any(), any(), any( LockOptions.class ), any() );
		assertThatThrownBy( () -> LoaderHelper.upgradeLock( f.entity, f.entry,
				new LockOptions( PESSIMISTIC_FORCE_INCREMENT ).setLockScope( EXTENDED ), f.session ) ).isSameAs( failure );
		verify( f.persister, never() ).forceVersionIncrement( any(), any(), any() );
		verify( f.entry, never() ).forceLocked( any(), any() );
		verify( f.entry, never() ).setLockMode( any() );
		verify( f.monitor ).completeEntityLockEvent( null, 1, "Item", PESSIMISTIC_WRITE, false, f.session );
	}

	@ParameterizedTest
	@EnumSource(value = Status.class, names = { "MANAGED", "READ_ONLY" })
	void forceLockedUpdatesVersionWithoutCreatingASnapshot(Status status) {
		final var persister = mock( EntityPersister.class );
		when( persister.getVersionPropertyIndex() ).thenReturn( 1 );
		when( persister.isMutable() ).thenReturn( true );
		final var snapshot = new Object[] { "loaded", 0 };
		final var entry = new EntityEntryImpl( status, snapshot, null, 1, 0, LockMode.READ, true, persister, null );
		final var entity = new Object();
		entry.forceLocked( entity, 1 );
		assertThat( entry.getVersion() ).isEqualTo( 1 );
		assertThat( entry.getLockMode() ).isEqualTo( PESSIMISTIC_FORCE_INCREMENT );
		assertThat( entry.getStatus() ).isEqualTo( status );
		if ( status == Status.READ_ONLY ) {
			assertThat( entry.getLoadedState() ).isNull();
		}
		else {
			assertThat( entry.getLoadedState() ).isSameAs( snapshot ).containsExactly( "loaded", 1 );
		}
		verify( persister ).setValue( entity, 1, 1 );
	}

	enum FailureStage { INCREMENT, STATE, UPDATE, INVALIDATE, REGISTRATION }

	@ParameterizedTest
	@EnumSource(FailureStage.class)
	void preHandoffFailureReleasesOnlyTheOwnedCacheLock(FailureStage stage) {
		final var f = new Fixture( true );
		final var failure = new IllegalStateException( stage.name() );
		switch ( stage ) {
			case INCREMENT -> when( f.persister.forceVersionIncrement( 1, 0, f.session ) ).thenThrow( failure );
			case STATE -> doThrow( failure ).when( f.entry ).forceLocked( f.entity, 1 );
			case UPDATE -> doThrow( failure ).when( f.cache ).update( f.session, f.key, f.payload, 1, 0 );
			case INVALIDATE -> {
				when( f.entry.getStatus() ).thenReturn( Status.READ_ONLY );
				doThrow( failure ).when( f.cache ).remove( f.session, f.key );
			}
			case REGISTRATION -> doThrow( failure ).when( f.callbacks ).registerCallback( any( TransactionCompletionCallbacks.AfterCompletionCallback.class ) );
		}
		assertThatThrownBy( () -> OptimisticLockHelper.forceVersionIncrement( f.entity, f.entry, f.session ) ).isSameAs( failure );
		verify( f.cache ).unlockItem( f.session, f.key, f.softLock );
		verify( f.cache, never() ).afterUpdate( any(), any(), any(), any(), any(), any() );
		if ( stage == FailureStage.INCREMENT ) {
			verify( f.entry, never() ).forceLocked( any(), any() );
		}
		if ( stage != FailureStage.REGISTRATION ) {
			verifyNoInteractions( f.callbacks );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void cleanupFailureDoesNotReplaceTheIncrementError(boolean sameFailure) {
		final var f = new Fixture( true );
		final var failure = new AssertionError( "increment error" );
		final Throwable cleanup = sameFailure ? failure : new IllegalStateException( "unlock failure" );
		when( f.persister.forceVersionIncrement( 1, 0, f.session ) ).thenThrow( failure );
		doThrow( cleanup ).when( f.cache ).unlockItem( f.session, f.key, f.softLock );
		assertThatThrownBy( () -> OptimisticLockHelper.forceVersionIncrement( f.entity, f.entry, f.session ) )
				.isSameAs( failure ).satisfies( thrown -> assertThat( thrown.getSuppressed() )
						.containsExactly( sameFailure ? new Throwable[0] : new Throwable[] { cleanup } ) );
		verify( f.cache ).unlockItem( f.session, f.key, f.softLock );
	}

	@ParameterizedTest
	@CsvSource({
			"MANAGED, NORMAL, false, true", "MANAGED, NORMAL, false, false",
			"MANAGED, NORMAL, true, true", "MANAGED, GET, false, true",
			"READ_ONLY, NORMAL, false, true", "READ_ONLY, NORMAL, false, false"
	})
	void successfulHandoffPreservesCacheOrderingAndCompletionPolicy(Status status, CacheMode mode, boolean invalidate, boolean commit) {
		final var f = new Fixture( true );
		when( f.entry.getStatus() ).thenReturn( status );
		when( f.persister.isCacheInvalidationRequired() ).thenReturn( invalidate );
		when( f.session.getCacheMode() ).thenReturn( mode );
		OptimisticLockHelper.forceVersionIncrement( f.entity, f.entry, f.session );
		final boolean update = status == Status.MANAGED && mode.isPutEnabled() && !invalidate;
		final var callback = ArgumentCaptor.forClass( TransactionCompletionCallbacks.AfterCompletionCallback.class );
		final var order = inOrder( f.cache, f.persister, f.entry, f.callbacks );
		order.verify( f.cache ).lockItem( f.session, f.key, 0 );
		order.verify( f.persister ).forceVersionIncrement( 1, 0, f.session );
		order.verify( f.entry ).forceLocked( f.entity, 1 );
		if ( update ) {
			order.verify( f.cache ).update( f.session, f.key, f.payload, 1, 0 );
		}
		else {
			order.verify( f.cache ).remove( f.session, f.key );
		}
		order.verify( f.callbacks ).registerCallback( callback.capture() );
		verify( f.cache, never() ).unlockItem( any(), any(), any() );
		// A later status change cannot turn invalidation into publication of a null value.
		when( f.entry.getStatus() ).thenReturn( Status.MANAGED );
		callback.getValue().doAfterTransactionCompletion( commit, f.session );
		if ( update && commit ) {
			verify( f.cache ).afterUpdate( f.session, f.key, f.payload, 1, 0, f.softLock );
			verify( f.cache, never() ).unlockItem( any(), any(), any() );
		}
		else {
			verify( f.cache ).unlockItem( f.session, f.key, f.softLock );
			verify( f.cache, never() ).afterUpdate( any(), any(), any(), any(), any(), any() );
		}
	}

	private static class Fixture {
		final Object entity = new Object();
		final EntityEntry entry = mock( EntityEntry.class );
		final EntityPersister persister = mock( EntityPersister.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final EventMonitor monitor = mock( EventMonitor.class );
		final TransactionCompletionCallbacks callbacks = mock( TransactionCompletionCallbacks.class );
		final EntityDataAccess cache = mock( EntityDataAccess.class );
		final SoftLock softLock = mock( SoftLock.class );
		final CacheEntry payload = mock( CacheEntry.class );
		final Object key = new Object();

		Fixture(boolean cached) {
			final var factory = mock( SessionFactoryImplementor.class );
			when( session.getFactory() ).thenReturn( factory );
			when( factory.getStatistics() ).thenReturn( mock( StatisticsImplementor.class ) );
			when( session.getEventMonitor() ).thenReturn( monitor );
			when( session.getEventListenerManager() ).thenReturn( mock( SessionEventListenerManager.class ) );
			when( session.getTransactionCompletionCallbacks() ).thenReturn( callbacks );
			when( session.getCacheMode() ).thenReturn( CacheMode.NORMAL );
			when( entry.getPersister() ).thenReturn( persister );
			when( entry.getId() ).thenReturn( 1 );
			when( entry.getVersion() ).thenReturn( 0 );
			when( entry.getLockMode() ).thenReturn( LockMode.READ );
			when( entry.getStatus() ).thenReturn( Status.MANAGED );
			when( entry.getLoadedState() ).thenReturn( new Object[] { "loaded", 0 } );
			when( entry.isExistsInDatabase() ).thenReturn( true );
			when( persister.isVersioned() ).thenReturn( true );
			when( persister.getEntityName() ).thenReturn( "Item" );
			when( persister.forceVersionIncrement( 1, 0, session ) ).thenReturn( 1 );
			when( persister.canWriteToCache() ).thenReturn( cached );
			when( persister.getCacheAccessStrategy() ).thenReturn( cache );
			when( cache.generateCacheKey( 1, persister, factory, null ) ).thenReturn( key );
			when( cache.lockItem( session, key, 0 ) ).thenReturn( softLock );
			when( persister.getCacheEntryStructure() ).thenReturn( UnstructuredCacheEntry.INSTANCE );
			when( persister.buildCacheEntry( same( entity ), any(), eq( 1 ), same( session ) ) ).thenReturn( payload );
		}
	}
}
