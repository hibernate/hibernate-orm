/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Conservative delta producer which adapts the existing [PersistentCollection]
/// comparison contract for initialized custom wrappers.
///
/// @since 8.0
/// @author Steve Ebersole
final class LegacyCompatibleCollectionDeltaProducer implements CollectionDeltaProducer {
	static final LegacyCompatibleCollectionDeltaProducer INSTANCE =
			new LegacyCompatibleCollectionDeltaProducer();

	@Override
	public CollectionDeltaProduction produceDelta(CollectionDeltaProductionContext context) {
		final var collection = context.collection();
		if ( !collection.wasInitialized() ) {
			return CollectionDeltaProduction.initializationRequired();
		}

		final List<CollectionChange> changes;
		final var existingChangeSet = collection.getChangeSet( context.persister() );
		if ( existingChangeSet != null ) {
			changes = adaptChangeSet( existingChangeSet );
		}
		else if ( context.baseline() == CollectionBaseline.EMPTY ) {
			changes = collectCreateChanges( context );
		}
		else {
			changes = collectLegacyComparisonChanges( context );
		}

		return CollectionDeltaProduction.produced( new CollectionDelta(
				context.baseline(),
				DeltaCoverage.COMPLETE,
				changes,
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		) );
	}

	private static List<CollectionChange> adaptChangeSet(CollectionChangeSet changeSet) {
		if ( changeSet.isEmpty() ) {
			return List.of();
		}
		final var changes = new ArrayList<CollectionChange>(
				changeSet.removals().size()
						+ changeSet.additions().size()
						+ changeSet.shifts().size()
						+ changeSet.valueChanges().size()
		);
		for ( var removal : changeSet.removals() ) {
			changes.add( new CollectionChange.Removal( removal.element(), removal.snapshotIndex() ) );
		}
		for ( var addition : changeSet.additions() ) {
			changes.add( new CollectionChange.Addition( addition.element(), addition.index() ) );
		}
		for ( var shift : changeSet.shifts() ) {
			changes.add( new CollectionChange.PositionChange(
					shift.element(),
					shift.snapshotIndex(),
					shift.currentIndex()
			) );
		}
		for ( var valueChange : changeSet.valueChanges() ) {
			changes.add( new CollectionChange.Replacement(
					valueChange.oldValue(),
					valueChange.newValue(),
					valueChange.index(),
					valueChange.index()
			) );
		}
		return changes;
	}

	private static List<CollectionChange> collectCreateChanges(
			CollectionDeltaProductionContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final var entries = collection.entries( persister );
		final var changes = new ArrayList<CollectionChange>();
		int position = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			if ( collection.includeInRecreate(
					entry,
					position,
					collection,
					persister.getAttributeMapping() ) ) {
				changes.add( new CollectionChange.Addition(
						collection.getElement( entry ),
						persister.hasIndex() ? collection.getIndex( entry, position, persister ) : null
				) );
			}
			position++;
		}
		return changes;
	}

	private static List<CollectionChange> collectLegacyComparisonChanges(
			CollectionDeltaProductionContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final var changes = new ArrayList<CollectionChange>();

		// Request snapshot elements rather than row-restriction keys. Specialized
		// producers retain collection identifiers and positions independently.
		final var deletions = collection.getDeletes( persister, true );
		while ( deletions.hasNext() ) {
			changes.add( new CollectionChange.Removal( deletions.next(), null ) );
		}

		final var entries = collection.entries( persister );
		int position = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			final Object currentPosition = persister.hasIndex()
					? collection.getIndex( entry, position, persister )
					: null;
			if ( collection.includeInInsert(
					entry,
					position,
					collection,
					persister.getAttributeMapping() ) ) {
				changes.add( new CollectionChange.Addition(
						collection.getElement( entry ),
						currentPosition
				) );
			}
			else if ( collection.needsUpdating( entry, position, persister.getAttributeMapping() ) ) {
				changes.add( new CollectionChange.Replacement(
						collection.getSnapshotElement( entry, position ),
						collection.getElement( entry ),
						currentPosition,
						currentPosition
				) );
			}
			position++;
		}
		return changes;
	}

	private LegacyCompatibleCollectionDeltaProducer() {
	}
}
