package org.hibernate.orm.test.locking;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.ObjectDeletedException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.Jira;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static jakarta.persistence.PessimisticLockScope.EXTENDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Verifies acquisition options, entry-mode preservation and resource cleanup
/// without relying on a database's lock-strength or timeout substitutions.
///
/// @author Steve Ebersole
@Jira("https://hibernate.atlassian.net/browse/HHH-21030")
class PessimisticLockScopeAcquisitionTest {
	@ParameterizedTest
	@CsvSource({
			"PESSIMISTIC_READ, PESSIMISTIC_READ, PESSIMISTIC_READ",
			"PESSIMISTIC_WRITE, PESSIMISTIC_READ, PESSIMISTIC_READ",
			"PESSIMISTIC_WRITE, PESSIMISTIC_WRITE, PESSIMISTIC_WRITE",
			"PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_READ, PESSIMISTIC_READ",
			"PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_WRITE, PESSIMISTIC_WRITE",
			"PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_WRITE",
			"WRITE, PESSIMISTIC_FORCE_INCREMENT, PESSIMISTIC_WRITE"
	})
	void scopeAcquisitionUsesRequestedStrengthAndPreservesOptions(LockMode held, LockMode requested, LockMode effective) {
		final var fixture = new Fixture( held );
		final var options = new LockOptions( requested ).setLockScope( EXTENDED )
				.setTimeout( Timeout.milliseconds( 750 ) ).setFollowOnStrategy( Locking.FollowOn.DISALLOW );
		doAnswer( invocation -> {
			final var actual = invocation.getArgument( 3, LockOptions.class );
			assertThat( actual.getLockMode() ).isEqualTo( effective );
			assertThat( actual.getLockScope() ).isEqualTo( EXTENDED );
			assertThat( actual.getTimeout() ).isEqualTo( options.getTimeout() );
			assertThat( actual.getFollowOnStrategy() ).isEqualTo( Locking.FollowOn.DISALLOW );
			return null;
		} ).when( fixture.persister ).lock( eq( 1 ), eq( 7 ), same( fixture.entity ), any( LockOptions.class ), same( fixture.session ) );

		LoaderHelper.upgradeLock( fixture.entity, fixture.entry, options, fixture.session );

		assertThat( options.getLockMode() ).isEqualTo( requested );
		assertThat( options.getLockScope() ).isEqualTo( EXTENDED );
		assertThat( options.getTimeout() ).isEqualTo( Timeout.milliseconds( 750 ) );
		assertThat( options.getFollowOnStrategy() ).isEqualTo( Locking.FollowOn.DISALLOW );
		verify( fixture.persister ).lock( eq( 1 ), eq( 7 ), same( fixture.entity ), any( LockOptions.class ), same( fixture.session ) );
		verify( fixture.entry, never() ).setLockMode( any() );
		verify( fixture.entry, never() ).setVersion( any() );
		verify( fixture.monitor ).completeEntityLockEvent( fixture.event, 1, "Item", effective, true, fixture.session );
	}

	@ParameterizedTest
	@CsvSource({
			"PESSIMISTIC_READ, NORMAL", "PESSIMISTIC_WRITE, NORMAL", "PESSIMISTIC_FORCE_INCREMENT, NORMAL",
			"OPTIMISTIC, EXTENDED", "OPTIMISTIC_FORCE_INCREMENT, EXTENDED", "NONE, EXTENDED",
			"PESSIMISTIC_READ, FETCHED"
	})
	void satisfiedRequestsOutsideExtendedPessimisticScopeRemainNoOps(LockMode requested, PessimisticLockScope scope) {
		final var fixture = new Fixture( LockMode.PESSIMISTIC_FORCE_INCREMENT );
		LoaderHelper.upgradeLock( fixture.entity, fixture.entry,
				new LockOptions( requested ).setLockScope( scope ), fixture.session );
		verifyNoInteractions( fixture.persister, fixture.monitor );
		verify( fixture.entry, never() ).setLockMode( any() );
	}

	@ParameterizedTest
	@CsvSource({ "true", "false" })
	void scopeAcquisitionCompletesMonitoringAndReleasesCacheSoftLock(boolean fail) {
		final var fixture = new Fixture( PESSIMISTIC_WRITE );
		final var cache = mock( EntityDataAccess.class );
		final var softLock = mock( SoftLock.class );
		final var cacheKey = new Object();
		final var failure = new IllegalStateException( "acquisition failed" );
		when( fixture.persister.canWriteToCache() ).thenReturn( true );
		when( fixture.persister.getCacheAccessStrategy() ).thenReturn( cache );
		when( cache.generateCacheKey( 1, fixture.persister, null, null ) ).thenReturn( cacheKey );
		when( cache.lockItem( fixture.session, cacheKey, 7 ) ).thenReturn( softLock );
		final var options = new LockOptions( LockMode.PESSIMISTIC_READ ).setLockScope( EXTENDED );
		if ( fail ) {
			doThrow( failure ).when( fixture.persister ).lock( 1, 7, fixture.entity, options, fixture.session );
			assertThatThrownBy( () -> LoaderHelper.upgradeLock( fixture.entity, fixture.entry, options, fixture.session ) )
					.isSameAs( failure );
		}
		else {
			LoaderHelper.upgradeLock( fixture.entity, fixture.entry, options, fixture.session );
		}
		verify( cache ).unlockItem( fixture.session, cacheKey, softLock );
		verify( fixture.monitor ).completeEntityLockEvent( fixture.event, 1, "Item", LockMode.PESSIMISTIC_READ, !fail, fixture.session );
		verify( fixture.entry, never() ).setLockMode( any() );
	}

	@Test
	void deletedInstanceIsRejectedBeforeScopeAcquisition() {
		final var fixture = new Fixture( PESSIMISTIC_WRITE );
		when( fixture.entry.getStatus() ).thenReturn( Status.DELETED );
		assertThatThrownBy( () -> LoaderHelper.upgradeLock( fixture.entity, fixture.entry,
				new LockOptions( PESSIMISTIC_WRITE ).setLockScope( EXTENDED ), fixture.session ) )
				.isInstanceOf( ObjectDeletedException.class );
		verify( fixture.persister, never() ).lock( any(), any(), any(), any( LockOptions.class ), any() );
		verifyNoInteractions( fixture.monitor );
	}

	@ParameterizedTest
	@CsvSource({ "true", "false" })
	void genuineModeUpgradeIsRecordedOnlyAfterSuccessfulAcquisition(boolean fail) {
		final var fixture = new Fixture( LockMode.PESSIMISTIC_READ );
		final var options = new LockOptions( PESSIMISTIC_WRITE ).setLockScope( EXTENDED );
		if ( fail ) {
			doThrow( new IllegalStateException() ).when( fixture.persister ).lock( 1, 7, fixture.entity, options, fixture.session );
			assertThatThrownBy( () -> LoaderHelper.upgradeLock( fixture.entity, fixture.entry, options, fixture.session ) )
					.isInstanceOf( IllegalStateException.class );
			verify( fixture.entry, never() ).setLockMode( any() );
		}
		else {
			LoaderHelper.upgradeLock( fixture.entity, fixture.entry, options, fixture.session );
			verify( fixture.entry ).setLockMode( PESSIMISTIC_WRITE );
		}
	}

	private static class Fixture {
		final Object entity = new Object();
		final EntityEntry entry = mock( EntityEntry.class );
		final EntityPersister persister = mock( EntityPersister.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final EventMonitor monitor = mock( EventMonitor.class );
		final DiagnosticEvent event = mock( DiagnosticEvent.class );

		Fixture(LockMode held) {
			when( entry.getLockMode() ).thenReturn( held );
			when( entry.getPersister() ).thenReturn( persister );
			when( entry.getStatus() ).thenReturn( Status.MANAGED );
			when( entry.getId() ).thenReturn( 1 );
			when( entry.getVersion() ).thenReturn( 7 );
			when( entry.isExistsInDatabase() ).thenReturn( true );
			when( persister.getEntityName() ).thenReturn( "Item" );
			when( persister.isVersioned() ).thenReturn( true );
			when( session.getEventMonitor() ).thenReturn( monitor );
			when( monitor.beginEntityLockEvent() ).thenReturn( event );
		}
	}
}
