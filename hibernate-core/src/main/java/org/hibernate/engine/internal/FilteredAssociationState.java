/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Optional state for associations whose nonnull database reference was hidden during hydration.
 * Ordinary mappings retain only bits. Custom update SQL and attribute-based optimistic locking
 * retain keys, using the same slots in shared persister metadata.
 */
public abstract class FilteredAssociationState implements EntityEntryExtraState, Serializable {
	private transient EntityEntryExtraState next;

	/** A physical association key in temporary mutation state for mappings which require it. */
	public record Key(Object value) implements Serializable {
		public Key {
			assert value != null && !(value instanceof Key);
		}
	}

	public abstract boolean isEmpty();
	abstract boolean contains(int slot);
	abstract void set(int slot, Object key);
	abstract void clear(int slot);
	abstract Object key(int slot);

	public boolean retainsKeys() {
		return false;
	}

	static FilteredAssociationState create(int slots, boolean retainKeys) {
		return retainKeys ? new Keys( slots ) : slots <= Long.SIZE ? new Bits() : new BitArray( slots );
	}

	private static final class Bits extends FilteredAssociationState {
		private long bits;

		@Override
		public boolean isEmpty() {
			return bits == 0;
		}

		@Override
		boolean contains(int slot) {
			return (bits & (1L << slot)) != 0;
		}

		@Override
		void set(int slot, Object key) {
			bits |= 1L << slot;
		}

		@Override
		void clear(int slot) {
			bits &= ~(1L << slot);
		}

		@Override
		Object key(int slot) {
			throw new IllegalStateException( "Bitmap state does not retain keys" );
		}
	}

	private static final class BitArray extends FilteredAssociationState {
		private final long[] bits;

		private BitArray(int slots) {
			bits = new long[(slots + Long.SIZE - 1) / Long.SIZE];
		}

		@Override
		public boolean isEmpty() {
			for ( long word : bits ) {
				if ( word != 0 ) {
					return false;
				}
			}
			return true;
		}

		@Override
		boolean contains(int slot) {
			return (bits[slot / Long.SIZE] & (1L << slot)) != 0;
		}

		@Override
		void set(int slot, Object key) {
			bits[slot / Long.SIZE] |= 1L << slot;
		}

		@Override
		void clear(int slot) {
			bits[slot / Long.SIZE] &= ~(1L << slot);
		}

		@Override
		Object key(int slot) {
			throw new IllegalStateException( "Bitmap state does not retain keys" );
		}
	}

	private static final class Keys extends FilteredAssociationState {
		private final Object[] keys;

		private Keys(int slots) {
			keys = new Object[slots];
		}

		@Override
		public boolean retainsKeys() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			for ( Object key : keys ) {
				if ( key != null ) {
					return false;
				}
			}
			return true;
		}

		@Override
		boolean contains(int slot) {
			return keys[slot] != null;
		}

		@Override
		void set(int slot, Object key) {
			keys[slot] = key;
		}

		@Override
		void clear(int slot) {
			keys[slot] = null;
		}

		@Override
		Object key(int slot) {
			return keys[slot];
		}
	}

	/** Builds shared assembler metadata once, returning null for unaffected result graphs. */
	public static int[][] assemblerIndexes(DomainResultAssembler<?>[][] assemblers) {
		int[][] result = null;
		for ( int subtype = 0; subtype < assemblers.length; subtype++ ) {
			final var row = assemblers[subtype];
			if ( row != null ) {
				int[] indexes = null;
				int count = 0;
				for ( int i = 0; i < row.length; i++ ) {
					if ( row[i] != null ) {
						final var initializer = row[i].getInitializer();
						// An @Any initializer resolves its target type from each row's discriminator.
						// Only ordinary to-one mappings have a static target descriptor to inspect here.
						final boolean affected =
								initializer instanceof EntityInitializer<?> entity
										? hasFilterOrRestriction( entity )
										: hasRestrictedAssociations( initializer );
						if ( affected ) {
							if ( indexes == null ) {
								indexes = new int[row.length];
							}
							indexes[count++] = i;
						}
					}
				}
				if ( count != 0 ) {
					if ( result == null ) {
						result = new int[assemblers.length][];
					}
					result[subtype] = Arrays.copyOf( indexes, count );
				}
			}
		}
		return result;
	}

	private static boolean hasFilterOrRestriction(EntityInitializer<?> entity) {
		return entity.getInitializedPart() instanceof ToOneAttributeMapping
			&& ( entity.getEntityDescriptor().hasWhereRestrictions()
				|| entity.getEntityDescriptor().hasFilterForLoadByKey() );
	}

	private static boolean hasRestrictedAssociations(Initializer<?> initializer) {
		return initializer instanceof EmbeddableInitializer<?> embedded
			&& hasRestrictedAssociations( embedded.getInitializedPart().getEmbeddableTypeDescriptor() );
	}

	private static boolean hasRestrictedAssociations(ManagedMappingType mapping) {
		for ( int i = 0; i < mapping.getNumberOfAttributeMappings(); i++ ) {
			final var attribute = mapping.getAttributeMapping( i );
			if ( attribute instanceof ToOneAttributeMapping toOne ) {
				final var target = toOne.getEntityMappingType().getEntityPersister();
				if ( target.hasWhereRestrictions() || target.hasFilterForLoadByKey() ) {
					return true;
				}
			}
			else if ( attribute instanceof EmbeddableValuedModelPart embedded
					&& hasRestrictedAssociations( embedded.getEmbeddableTypeDescriptor() ) ) {
				return true;
			}
		}
		return false;
	}

	public static FilteredAssociationState collect(
			FilteredAssociationState state, FilteredAssociationMapping mapping,
			DomainResultAssembler<?>[] assemblers, int[] indexes, Object[] values,
			RowProcessingState rowProcessingState) {
		if ( indexes != null ) {
			for ( int i : indexes ) {
				final var initializer = assemblers[i].getInitializer();
				if ( values[i] == null && initializer instanceof EntityInitializer<?> entityInitializer ) {
					final Object key = entityInitializer.getFilteredAssociationKey( rowProcessingState );
					if ( key != null ) {
						state = mapping.record( state, (ToOneAttributeMapping) entityInitializer.getInitializedPart(), key );
					}
				}
				else if ( initializer instanceof EmbeddableInitializer<?> embedded ) {
					state = embedded.collectFilteredAssociations( rowProcessingState, state, mapping );
				}
			}
		}
		return state;
	}

	public static void register(
			EntityEntry entry, DomainResultAssembler<?>[] assemblers, int[] indexes,
			Object[] values, RowProcessingState rowProcessingState) {
		final var existing = entry.getExtraState( FilteredAssociationState.class );
		final var state = collect( existing, entry.getPersister().getFilteredAssociationMapping(),
				assemblers, indexes, values, rowProcessingState );
		if ( existing == null && state != null ) {
			entry.addExtraState( state );
		}
	}

	public static boolean hasFilteredAssociations(EntityEntry entry) {
		final var state = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		return state != null && !state.isEmpty();
	}

	/** Returns domain state unchanged for bitmap mappings. */
	public Object[] physicalState(Object[] state, EntityPersister persister) {
		return state == null || isEmpty() || !retainsKeys() ? state
				: persister.getFilteredAssociationMapping().physicalState( this, state );
	}

	public void afterUpdate(Object[] state, EntityPersister persister) {
		persister.getFilteredAssociationMapping().afterUpdate( this, state );
	}

	@Override
	public void addExtraState(EntityEntryExtraState extraState) {
		if ( next == null ) {
			next = extraState;
		}
		else {
			next.addExtraState( extraState );
		}
	}

	@Override
	public <T extends EntityEntryExtraState> T getExtraState(Class<T> type) {
		if ( next == null ) {
			return null;
		}
		else {
			return type.isInstance( next ) ? type.cast( next ) : next.getExtraState( type );
		}
	}
}
