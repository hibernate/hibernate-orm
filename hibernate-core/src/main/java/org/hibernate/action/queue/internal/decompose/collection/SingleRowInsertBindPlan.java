package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.decompose.collection.CollectionJdbcOperations;

import org.hibernate.action.queue.internal.constraint.UniqueConstraint;
import org.hibernate.action.queue.internal.decompose.KeyLookup;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/// Bind plan for a single collection row insert.
///
/// @author Steve Ebersole
public class SingleRowInsertBindPlan implements BindPlan {
	private final CollectionPersister persister;
	private final CollectionJdbcOperations.Values values;

	private final PersistentCollection<?> collection;
	private final Object key;
	private final Object entry;
	private final int entryIndex;
	private final KeyLookup.Reference[] keyReferences;

	public SingleRowInsertBindPlan(
			CollectionPersister persister,
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex) {
		this( persister, values, collection, key, entry, entryIndex, null );
	}

	/// @param keyReferences the rows from which the statement reads foreign keys,
	/// or `null` if the given values bind every key
	public SingleRowInsertBindPlan(
			CollectionPersister persister,
			CollectionJdbcOperations.Values values,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex,
			KeyLookup.Reference[] keyReferences) {
		this.keyReferences = keyReferences;
		this.persister = persister;
		this.values = values;
		this.collection = collection;
		this.key = key;
		this.entry = entry;
		this.entryIndex = entryIndex;
	}

	@Override
	public Object getEntityInstance() {
		// Collection operations don't represent entity operations - they represent
		// the collection relationship (FK updates for one-to-many, join table rows for many-to-many).
		// Returning the element creates artificial dependencies that fragment batches.
		return null;
	}

	@Override
	public boolean requiresMatchedRow() {
		return keyReferences != null;
	}

	@Override
	public OperationResultChecker getOperationResultChecker() {
		return keyReferences == null ? null
				: (rowCount, statement, position, sql, factory) -> KeyLookup.checkMatched( keyReferences, rowCount, sql );
	}

	@Override
	public Object[] getUniqueConstraintValues(
			UniqueConstraint constraint,
			SharedSessionContractImplementor session) {
		return CollectionUniqueKeyValueExtractor.extractValues(
				persister,
				collection,
				key,
				entry,
				entryIndex,
				constraint,
				session
		);
	}

	@Override
	public void bindValues(
			JdbcValueBindings jdbcValueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		if ( key == null ) {
			throw new IllegalArgumentException( "null key for collection: " + persister.getNavigableRole().getFullPath() );
		}

		values.applyValues( collection, key, entry, entryIndex, session, jdbcValueBindings );
	}
}
