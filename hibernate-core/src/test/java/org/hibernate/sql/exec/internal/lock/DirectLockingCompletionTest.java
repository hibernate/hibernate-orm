package org.hibernate.sql.exec.internal.lock;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/// Completion units deduplicate identities, bind instances and fail without replay.
///
/// @author Steve Ebersole
class DirectLockingCompletionTest {
	@Test
	void equivalentKeysIncrementOnceAfterAcquisition() {
		final var f = new Fixture();
		final var entity = f.register( 1 );
		f.completion.register( new EntityKey( 1, f.persister ), entity, f.entry );
		final List<String> events = new ArrayList<>();
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			helper.when( () -> OptimisticLockHelper.forceVersionIncrement( entity, f.entry, f.session ) )
					.thenAnswer( call -> { events.add( "increment" ); return null; } );
			f.completion.complete( () -> events.add( "acquire" ), f.session );
			f.completion.complete( () -> events.add( "next unit" ), f.session );
			assertThat( events ).containsExactly( "acquire", "increment", "next unit" );
		}
	}

	@Test
	void acquisitionErrorDiscardsAllWorkWithoutReplay() {
		final var f = new Fixture();
		f.register( 1 );
		f.register( 2 );
		final var error = new AssertionError( "acquisition" );
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			assertThatThrownBy( () -> f.completion.complete( () -> { throw error; }, f.session ) ).isSameAs( error );
			assertThatThrownBy( () -> f.completion.complete( () -> { throw new AssertionError( "replayed acquisition" ); }, f.session ) )
					.isInstanceOf( HibernateException.class ).hasMessageContaining( "Cannot resume" );
			helper.verifyNoInteractions();
		}
	}

	@Test
	void replacementInstanceCannotReceivePendingIncrement() {
		final var f = new Fixture();
		f.register( 1 );
		when( f.session.getPersistenceContextInternal().getEntity( new EntityKey( 1, f.persister ) ) ).thenReturn( new Object() );
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			assertThatThrownBy( () -> f.completion.complete( () -> { throw new AssertionError( "acquired for replaced target" ); }, f.session ) )
					.isInstanceOf( HibernateException.class ).hasMessageContaining( "original instance is no longer managed" );
			helper.verifyNoInteractions();
		}
	}

	@Test
	void completedStrongerModeDoesNotRepeatIncrement() {
		final var f = new Fixture();
		f.register( 1 );
		when( f.entry.getLockMode() ).thenReturn( LockMode.WRITE );
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			f.completion.complete( () -> {}, f.session );
			helper.verifyNoInteractions();
		}
	}

	@Test
	void incrementFailureDoesNotReplayOrContinueRemainingTargets() {
		final var f = new Fixture();
		final var first = f.register( 1 );
		f.register( 2 );
		final var failure = new HibernateException( "increment" );
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			helper.when( () -> OptimisticLockHelper.forceVersionIncrement( first, f.entry, f.session ) ).thenThrow( failure );
			assertThatThrownBy( () -> f.completion.complete( () -> {}, f.session ) ).isSameAs( failure );
			helper.verify( () -> OptimisticLockHelper.forceVersionIncrement( first, f.entry, f.session ) );
			helper.verifyNoMoreInteractions();
		}
	}

	private static class Fixture {
		final DirectLockingCompletion completion = new DirectLockingCompletion();
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final EntityEntry entry = mock( EntityEntry.class );
		final EntityPersister persister = mock( EntityPersister.class );

		Fixture() {
			when( persister.getRootEntityName() ).thenReturn( "Item" );
			when( persister.getIdentifierType() ).thenReturn( mock( Type.class ) );
			when( persister.isVersioned() ).thenReturn( true );
			when( entry.getPersister() ).thenReturn( persister );
			when( entry.getLockMode() ).thenReturn( LockMode.PESSIMISTIC_WRITE );
		}

		Object register(int id) {
			final Object entity = new Object();
			final var key = new EntityKey( id, persister );
			when( session.getPersistenceContextInternal().getEntry( entity ) ).thenReturn( entry );
			when( session.getPersistenceContextInternal().getEntity( key ) ).thenReturn( entity );
			completion.register( key, entity, entry );
			return entity;
		}
	}

	@Test
	void laterIncrementFailureDoesNotCompensateOrReplayEarlierIncrement() {
		final var f = new Fixture();
		final var first = f.register( 1 );
		final var second = f.register( 2 );
		f.register( 3 );
		final var failure = new HibernateException( "second increment" );
		try ( var helper = mockStatic( OptimisticLockHelper.class ) ) {
			helper.when( () -> OptimisticLockHelper.forceVersionIncrement( second, f.entry, f.session ) ).thenThrow( failure );
			assertThatThrownBy( () -> f.completion.complete( () -> {}, f.session ) ).isSameAs( failure );
			assertThatThrownBy( () -> f.completion.complete( () -> {}, f.session ) ).isInstanceOf( HibernateException.class );
			helper.verify( () -> OptimisticLockHelper.forceVersionIncrement( first, f.entry, f.session ) );
			helper.verify( () -> OptimisticLockHelper.forceVersionIncrement( second, f.entry, f.session ) );
			helper.verifyNoMoreInteractions();
		}
	}
}
