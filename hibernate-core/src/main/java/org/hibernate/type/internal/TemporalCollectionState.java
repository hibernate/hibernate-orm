package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;

import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentList;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.collection.CollectionPersister;

/// Relational snapshots owned by one persistent collection. Each materialized or
/// mutation-snapshot occurrence has its own state, including duplicate references
/// in a bag. A new mutation snapshot consumes each prior occurrence at most once;
/// indexed lists retain row-position correspondence until the actual SET bindings
/// replace it. Domain membership and equality remain the collection's concern.
///
/// The enclosing collection releases this transient state on detachment. Cache
/// publication still uses the existing collection cache transaction protocol.
///
/// @author Steve Ebersole
public final class TemporalCollectionState {
	private List<Occurrence> occurrences = new ArrayList<>();
	private boolean mutation;

	private record Occurrence(Object domain, TemporalPreparedState state) {
	}

	public static TemporalCollectionState get(PersistentCollection<?> collection, CollectionPersister persister) {
		return (collection instanceof PersistentList<?> || collection instanceof PersistentSet<?> || collection instanceof PersistentBag<?>)
				&& persister.hasCache() && TemporalPrecisionBasicType.contains( persister.getElementType() )
				&& collection instanceof AbstractPersistentCollection<?> persistent
				? persistent.getTemporalPreparedState() : null;
	}

	public static Object prepare(Object value, JdbcMapping mapping, PersistentCollection<?> collection,
			CollectionPersister persister, int position, SharedSessionContractImplementor session) {
		final var temporal = TemporalPrecisionBasicType.find( mapping );
		if ( temporal == null ) {
			return value;
		}
		final Object prepared = temporal.prepareRelational( value );
		final var state = get( collection, persister );
		if ( state != null ) {
			if ( !state.mutation ) {
				state.snapshot( collection, persister );
				state.mutation = true;
			}
			state.occurrences.get( position ).state().recordCandidate( temporal, prepared, session );
		}
		return prepared;
	}

	/// Capture a row before read conversion loses its relational representation.
	public TemporalPreparedState loading(int position, Object domain) {
		while ( occurrences.size() <= position ) {
			occurrences.add( new Occurrence( null, new TemporalPreparedState() ) );
		}
		final var state = new TemporalPreparedState();
		occurrences.set( position, new Occurrence( domain, state ) );
		return state;
	}

	/// Reconcile iteration order without collapsing equal or identical occurrences.
	public void snapshot(PersistentCollection<?> collection, CollectionPersister persister) {
		final var next = new ArrayList<Occurrence>();
		final boolean[] consumed = new boolean[occurrences.size()];
		final var byIdentity = new IdentityHashMap<Object, ArrayDeque<Integer>>();
		if ( !(collection instanceof PersistentList<?>) ) {
			for ( int i = 0; i < occurrences.size(); i++ ) {
				byIdentity.computeIfAbsent( occurrences.get( i ).domain(), key -> new ArrayDeque<>() ).add( i );
			}
		}
		final var entries = collection.entries( persister );
		while ( entries.hasNext() ) {
			final Object domain = collection.getElement( entries.next() );
			TemporalPreparedState retained = null;
			if ( collection instanceof PersistentList<?> && next.size() < occurrences.size() ) {
				retained = occurrences.get( next.size() ).state();
			}
			else {
				final var matches = byIdentity.get( domain );
				while ( matches != null && !matches.isEmpty() && consumed[matches.peek()] ) {
					matches.remove();
				}
				if ( matches != null && !matches.isEmpty() ) {
					final int index = matches.remove();
					consumed[index] = true;
					retained = occurrences.get( index ).state();
				}
				else {
					// A replacement equal under the existing domain type can remain
					// unbound. Consume one prior occurrence, never a shared last value.
					for ( int i = 0; i < occurrences.size(); i++ ) {
						if ( !consumed[i] && persister.getElementType().isEqual( occurrences.get( i ).domain(), domain ) ) {
							consumed[i] = true;
							retained = occurrences.get( i ).state();
							break;
						}
					}
				}
			}
			next.add( new Occurrence( domain, retained == null ? new TemporalPreparedState() : retained ) );
		}
		occurrences = next;
	}

	public void afterAction() {
		for ( var occurrence : occurrences ) {
			occurrence.state().actionCompleted();
		}
		mutation = false;
	}

	public Serializable disassemble(int position, Object domain, CollectionPersister persister,
			SharedSessionContractImplementor session) {
		// An absent indexed row is not a converted SQL null. Do not invent a
		// write conversion for holes which the collection never inserted.
		if ( domain == null && occurrences.get( position ).state().isEmpty() ) {
			return null;
		}
		return TemporalCacheAccess.disassemble( persister.getElementType(), domain, session, null,
				occurrences.get( position ).state() );
	}

	public Object assemble(int position, Serializable cached, CollectionPersister persister,
			SharedSessionContractImplementor session, Object owner) {
		final var retained = loading( position, null );
		// A null slot denotes an absent collection row, not a converted SQL null.
		if ( cached == null ) {
			return null;
		}
		final Object domain = TemporalCacheAccess.assemble( persister.getElementType(), cached, session, owner, retained );
		occurrences.set( position, new Occurrence( domain, retained ) );
		return domain;
	}
}
