package org.hibernate.engine.internal;

import java.util.BitSet;
import java.util.function.Supplier;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.cache.InternalCache;
import org.hibernate.internal.util.cache.InternalCacheFactory;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Lazily allocated, bounded cache of otherwise-static update variants, shared across sessions.
 * Keys describe SQL structure only. Values must not retain entity values or session state.
 */
public final class FilteredUpdateCache<V> {
	private static final int MAX_VARIANTS = 32;
	private volatile InternalCache<Key, V> cache;

	/** These mappings have no value-dependent assignments or optimistic-lock predicates. */
	public static boolean supports(EntityPersister persister) {
		if ( persister.isDynamicUpdate() || !persister.isMutable()
				|| persister.optimisticLockStyle().isAllOrDirty() ) {
			return false;
		}
		for ( var generator : persister.getGenerators() ) {
			if ( generator != null && generator.generatesOnUpdate() ) {
				return false;
			}
		}
		for ( var table : persister.getTableMappings() ) {
			if ( table.getUpdateDetails().getCustomSql() != null ) {
				return false;
			}
		}
		return true;
	}

	public V resolve(Key key, SessionFactoryImplementor factory, Supplier<V> creator) {
		var current = cache;
		if ( current == null ) {
			synchronized ( this ) {
				current = cache;
				if ( current == null ) {
					current = factory.getServiceRegistry().requireService( InternalCacheFactory.class )
							.createInternalCache( MAX_VARIANTS );
					cache = current;
				}
			}
		}
		return current.computeIfAbsent( key, ignored -> creator.get() );
	}

	/** Immutable snapshots of all variable SQL-shape inputs in the eligible update paths. */
	public static final class Key {
		private final BitSet omittedSlots;
		private final BitSet assignments;
		private final BitSet tables;

		public Key(BitSet omittedSlots, BitSet assignments, BitSet tables) {
			this.omittedSlots = (BitSet) omittedSlots.clone();
			this.assignments = (BitSet) assignments.clone();
			this.tables = (BitSet) tables.clone();
		}

		@Override
		public boolean equals(Object other) {
			return this == other || other instanceof Key key
				&& omittedSlots.equals( key.omittedSlots )
				&& assignments.equals( key.assignments ) && tables.equals( key.tables );
		}

		@Override
		public int hashCode() {
			return ( omittedSlots.hashCode() * 31 + assignments.hashCode() ) * 31 + tables.hashCode();
		}
	}
}
