package org.hibernate.sql.exec.internal.lock;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.annotation.Nullable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;

/// Invocation-owned pessimistic increments which must follow collection acquisition.
/// No state here belongs to a cached plan or to optimistic transaction callbacks.
///
/// @author Steve Ebersole
public final class DirectLockingCompletion {
	private final Map<EntityKey, Object> pending = new LinkedHashMap<>();
	private Runnable cursorAcquisition;
	private boolean failed;

	public static @Nullable DirectLockingCompletion from(@Nullable LoadedValuesCollector collector) {
		return collector instanceof LoadedValuesCollectorImpl standard
				? standard.getDirectLockingCompletion() : null;
	}

	public void register(EntityKey key, Object entity, EntityEntry entry) {
		checkActive();
		if ( !entry.getPersister().isVersioned() ) {
			throw new HibernateException( "Entity '" + entry.getEntityName()
					+ "' has no version and may not be locked at level " + LockMode.PESSIMISTIC_FORCE_INCREMENT );
		}
		final var previous = pending.putIfAbsent( key, entity );
		if ( previous != null && previous != entity ) {
			throw new HibernateException( "Different instances registered for pessimistic force increment: " + key );
		}
	}

	/// Called after every post-load notification for a cursor's logical result.
	public void finishLogicalResult() {
		checkActive();
		if ( cursorAcquisition != null ) {
			cursorAcquisition.run();
		}
	}

	public void registerCursorAcquisition(Runnable acquisition) {
		cursorAcquisition = acquisition;
	}

	/// Complete the whole acquisition unit before starting any owned increment.
	/// Integrations retain their existing acquisition and cache helper extension points.
	public void complete(Runnable acquisition, SharedSessionContractImplementor session) {
		checkActive();
		try {
			pending.forEach( (key, entity) -> managedEntry( key, entity, session ) );
			acquisition.run();
			pending.forEach( (key, entity) -> {
				final var entry = managedEntry( key, entity, session );
				if ( entry.getLockMode().lessThan( LockMode.PESSIMISTIC_FORCE_INCREMENT ) ) {
					OptimisticLockHelper.forceVersionIncrement( entity, entry, session );
				}
			} );
			pending.clear();
		}
		catch (RuntimeException | Error failure) {
			fail();
			throw failure;
		}
	}

	public void fail() {
		failed = true;
		pending.clear();
	}

	public void checkActive() {
		if ( failed ) {
			throw new HibernateException( "Cannot resume failed direct-locking completion" );
		}
	}

	private static EntityEntry managedEntry(EntityKey key, Object entity, SharedSessionContractImplementor session) {
		final var context = session.getPersistenceContextInternal();
		final var entry = context.getEntry( entity );
		if ( entry == null || context.getEntity( key ) != entity ) {
			throw new HibernateException( "Cannot complete pessimistic force increment for " + key
					+ ": the original instance is no longer managed" );
		}
		return entry;
	}
}
