package org.hibernate.collection.spi;

import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/// Represents the complete set of changes between a collection's snapshot state
/// and its current state. Used for planning collection update operations.
/// The four structural lists are defensively copied so graph planning always
/// observes the comparison result frozen by the collection interpreter.
///
/// @author Steve Ebersole
/// @since 8.0
///
@Incubating(since = "8.0", group = "collection-delta")
public record CollectionChangeSet(
		List<Removal> removals,
		List<Addition> additions,
		List<Shift> shifts,
		List<ValueChange> valueChanges) {
	public static final CollectionChangeSet EMPTY = new CollectionChangeSet(List.of(), List.of(), List.of(), List.of());

	public CollectionChangeSet {
		removals = List.copyOf( removals );
		additions = List.copyOf( additions );
		shifts = List.copyOf( shifts );
		valueChanges = List.copyOf( valueChanges );
	}

	public boolean isEmpty() {
		return removals.isEmpty() && additions.isEmpty() && shifts.isEmpty() && valueChanges.isEmpty();
	}

	/// Represents an element that was removed from the collection.
	///
	/// @param element The removed element
	/// @param snapshotIndex The position/key where the element existed in the snapshot
	public record Removal(Object element, Object snapshotIndex) implements SnapshotIndexed<Object> {}

	/// Represents an element that was added to the collection.
	///
	/// @param element The added element
	/// @param rowKey The list index, map key, or collection identifier, possibly deferred for a new row
	/// @param entryPosition The position passed to the collection when binding the entry, or -1 if unused
	public record Addition(Object element, @Nullable Object rowKey, int entryPosition) {
		public Addition(Object element, Object index) {
			this( element, index, positionFromIndex( index ) );
		}

		/// The row key exposed by the original change-set API.
		public Object index() {
			return rowKey;
		}

		/// The entry position, resolving a deferred list index at binding time.
		public int bindingPosition() {
			return resolvePosition( entryPosition, rowKey );
		}
	}

	/// Represents an element that changed position within the collection.
	///
	/// @param element The shifted element
	/// @param snapshotIndex The index where the element existed in the snapshot
	/// @param currentIndex The index where the element exists in the current collection
	public record Shift(Object element, Object snapshotIndex, Object currentIndex) implements SnapshotIndexed<Object> {}

	/// Represents a value replaced in an existing collection row.
	///
	/// @param oldValue The value in the snapshot
	/// @param newValue The value in the current collection
	/// @param rowKey The list index, map key, or collection identifier of the existing row
	/// @param entryPosition The position passed to the collection when binding the entry, or -1 if unused
	public record ValueChange(Object oldValue, Object newValue, @Nullable Object rowKey, int entryPosition) {
		public ValueChange(Object oldValue, Object newValue, Object index) {
			this( oldValue, newValue, index, positionFromIndex( index ) );
		}

		/// The row key exposed by the original change-set API.
		public Object index() {
			return rowKey;
		}

		/// The entry position, resolving a deferred list index at binding time.
		public int bindingPosition() {
			return resolvePosition( entryPosition, rowKey );
		}

		public Removal replacedRow() {
			return new Removal( oldValue,
					rowKey instanceof DeferredCollectionIdentifier identifier ? identifier.resolve() : rowKey );
		}
	}

	// Older producers supply a single index for both the logical key and the binding position.
	private static int positionFromIndex(Object index) {
		if ( index instanceof DeferredCollectionIdentifier identifier ) {
			return identifier.entryPosition();
		}
		return index instanceof Number number ? number.intValue() : -1;
	}

	private static int resolvePosition(int position, Object key) {
		return position == -1 && key instanceof DeferredCollectionPosition deferred ? deferred.resolve() : position;
	}
}
