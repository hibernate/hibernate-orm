package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;

/// Relational state already prepared or extracted for a particular managed entity.
/// Keys are mapping instances, never Java classes or ambient session precision.
/// Values use the relational mutability plan's cache form so subsequent application
/// mutation cannot change the recorded JDBC value. This state is private to the
/// entry and cannot publish a cache entry independently of a successful mutation.
///
/// @author Steve Ebersole
public final class TemporalPreparedState implements EntityEntryExtraState {
	private final Map<TemporalPrecisionBasicType<?>, Serializable> values = new IdentityHashMap<>();
	private final Map<TemporalPrecisionBasicType<?>, Serializable> candidates = new IdentityHashMap<>();
	private long generation;
	private EntityEntryExtraState next;

	public static Object prepare(Object value, JdbcMapping mapping, Object entity, SharedSessionContractImplementor session) {
		final var temporal = TemporalPrecisionBasicType.find( mapping );
		if ( temporal == null ) {
			return value;
		}
		final var prepared = temporal.prepareRelational( value );
		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		if ( entry != null && entry.getPersister().canWriteToCache() ) {
			state( entry ).recordCandidate( temporal, prepared, session );
		}
		return prepared;
	}

	public static void record(EntityEntry entry, TemporalPrecisionBasicType<?> mapping,
			Object relationalValue, SharedSessionContractImplementor session) {
		if ( entry != null && entry.getPersister().canWriteToCache() ) {
			state( entry ).record( mapping, relationalValue, session );
		}
	}

	private static TemporalPreparedState state(EntityEntry entry) {
		var state = entry.getExtraState( TemporalPreparedState.class );
		if ( state == null ) {
			state = new TemporalPreparedState();
			entry.addExtraState( state );
		}
		return state;
	}

	/// Retains a bind candidate separately from the state available to cache reads.
	public void recordCandidate(TemporalPrecisionBasicType<?> mapping, Object value,
			SharedSessionContractImplementor session) {
		candidates.put( mapping, mapping.disassemblePrepared( value, session ) );
	}

	/// Advance at existing action completion, not at conversion/binding. Transaction
	/// commit and rollback still belong to the cache access strategy; this is only
	/// the latest successful action snapshot, never proof of transaction commit.
	public void actionCompleted() {
		if ( !candidates.isEmpty() ) {
			values.putAll( candidates );
			candidates.clear();
			generation++;
		}
	}

	public static void actionCompleted(EntityEntry entry) {
		final var state = entry.getExtraState( TemporalPreparedState.class );
		if ( state != null ) {
			state.actionCompleted();
		}
	}

	/// Cache loading replaces its temporary entry after assembly. Transfer only
	/// this lifecycle's relational snapshots, without retaining the temporary entry.
	public static void transfer(EntityEntry source, EntityEntry target) {
		final var state = source == null ? null : source.getExtraState( TemporalPreparedState.class );
		if ( state != null ) {
			final var copy = new TemporalPreparedState();
			copy.values.putAll( state.values );
			copy.generation = state.generation;
			target.addExtraState( copy );
		}
	}

	public void record(TemporalPrecisionBasicType<?> mapping, Object relationalValue,
			SharedSessionContractImplementor session) {
		values.put( mapping, mapping.disassemblePrepared( relationalValue, session ) );
		candidates.remove( mapping );
	}

	public void copyFrom(TemporalPreparedState source) {
		if ( source != null ) {
			values.putAll( source.values );
		}
	}

	public static void attach(EntityEntry entry, TemporalPreparedState source) {
		if ( entry != null && entry.getPersister().canWriteToCache() && source != null && !source.isEmpty() ) {
			state( entry ).copyFrom( source );
		}
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public boolean contains(TemporalPrecisionBasicType<?> mapping) {
		return values.containsKey( mapping );
	}

	public Serializable get(TemporalPrecisionBasicType<?> mapping) {
		return values.get( mapping );
	}

	@Override
	public void addExtraState(EntityEntryExtraState state) {
		if ( next == null ) {
			next = state;
		}
		else {
			next.addExtraState( state );
		}
	}

	@Override
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> type) {
		return next == null ? null : type.isInstance( next ) ? type.cast( next ) : next.getExtraState( type );
	}
}
