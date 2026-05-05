/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.queue.constraint.UniqueConstraint;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.SnapshotIndexed;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.collection.CollectionPersister;

import java.util.HashMap;
import java.util.Map;

/**
 * Extracts collection row values in unique-constraint column order.
 */
final class CollectionUniqueKeyValueExtractor {
	private CollectionUniqueKeyValueExtractor() {
	}

	static Object[] extractValues(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object key,
			Object entry,
			int entryIndex,
			UniqueConstraint constraint,
			SharedSessionContractImplementor session) {
		final var attributeMapping = persister.getAttributeMapping();
		final Map<ColumnKey, Object> valuesByColumn = new HashMap<>();

		collectValues( attributeMapping.getKeyDescriptor().getKeyPart(), key, valuesByColumn, session );

		final Object element = extractElement( collection, entry );

		final var identifierDescriptor = attributeMapping.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			final Object identifier = extractIdentifier( collection, entry, entryIndex );
			if ( identifier != null ) {
				collectValues( identifierDescriptor, identifier, valuesByColumn, session );
			}
		}
		else {
			final var indexDescriptor = attributeMapping.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				final Object index = extractIndex( persister, collection, entry, entryIndex );
				if ( index != null ) {
					collectValues(
							indexDescriptor,
							incrementIndexByBase( attributeMapping.getIndexMetadata().getListIndexBase(), index ),
							valuesByColumn,
							session
					);
				}
			}
		}

		collectValues( attributeMapping.getElementDescriptor(), element, valuesByColumn, session );

		final Object[] values = new Object[constraint.columns().getJdbcTypeCount()];
		for ( int i = 0; i < constraint.columns().getJdbcTypeCount(); i++ ) {
			final SelectableMapping selectable = constraint.columns().getSelectable( i );
			final ColumnKey columnKey = ColumnKey.from( selectable );
			if ( !valuesByColumn.containsKey( columnKey ) ) {
				return null;
			}
			values[i] = valuesByColumn.get( columnKey );
		}
		return values;
	}

	private static void collectValues(
			ModelPart modelPart,
			Object domainValue,
			Map<ColumnKey, Object> valuesByColumn,
			SharedSessionContractImplementor session) {
		try {
			modelPart.decompose(
					domainValue,
					(valueIndex, value, selectable) -> valuesByColumn.put( ColumnKey.from( selectable ), value ),
					session
			);
		}
		catch (RuntimeException ignored) {
			// Missing decomposition for one collection part should simply make slot extraction incomplete.
		}
	}

	private static Object extractIdentifier(PersistentCollection<?> collection, Object entry, int entryIndex) {
		if ( entryIndex < 0 ) {
			return null;
		}
		try {
			return collection.getIdentifier( entry, entryIndex );
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private static Object extractIndex(
			CollectionPersister persister,
			PersistentCollection<?> collection,
			Object entry,
			int entryIndex) {
		if ( entry instanceof SnapshotIndexed<?> snapshotIndexed ) {
			return snapshotIndexed.snapshotIndex();
		}
		if ( entryIndex < 0 ) {
			return null;
		}
		try {
			return collection.getIndex( entry, entryIndex, persister );
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private static Object extractElement(PersistentCollection<?> collection, Object entry) {
		if ( entry instanceof CollectionChangeSet.Removal removal ) {
			return removal.element();
		}
		try {
			return collection.getElement( entry );
		}
		catch (RuntimeException ignored) {
			return entry;
		}
	}

	private static Object incrementIndexByBase(int baseIndex, Object index) {
		return baseIndex > 0 ? baseIndex + (Integer) index : index;
	}

	private record ColumnKey(String tableName, String columnName) {
		static ColumnKey from(SelectableMapping selectableMapping) {
			return new ColumnKey(
					selectableMapping.getContainingTableExpression(),
					selectableMapping.getSelectableName()
			);
		}
	}
}
