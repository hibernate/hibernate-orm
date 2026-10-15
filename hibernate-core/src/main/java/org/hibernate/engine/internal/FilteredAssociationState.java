package org.hibernate.engine.internal;

import java.io.Serializable;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryExtraState;
import org.hibernate.persister.entity.EntityPersister;

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

	/** Whether a hidden slot must be omitted instead of supplied as a physical value. */
	boolean omitsColumn(int slot) {
		return contains( slot );
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
	}

	private static final class Keys extends FilteredAssociationState {
		private final Object[] keys;

		private Keys(int slots) {
			keys = new Object[slots];
		}

		@Override
		boolean omitsColumn(int slot) {
			return false;
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
		Object[] physicalState(Object[] values, FilteredAssociationMapping mapping) {
			return values == null || isEmpty() ? values : mapping.physicalState( this, values, slot -> keys[slot] );
		}
	}

	public static boolean hasFilteredAssociations(EntityEntry entry) {
		final var state = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		return state != null && !state.isEmpty();
	}

	/** Returns domain state unchanged for bitmap mappings. */
	public Object[] physicalState(Object[] state, EntityPersister persister) {
		return physicalState( state, persister.getFilteredAssociationMapping() );
	}

	Object[] physicalState(Object[] values, FilteredAssociationMapping mapping) {
		return values;
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
			return type.isInstance( next )
					? type.cast( next )
					: next.getExtraState( type );
		}
	}
}
