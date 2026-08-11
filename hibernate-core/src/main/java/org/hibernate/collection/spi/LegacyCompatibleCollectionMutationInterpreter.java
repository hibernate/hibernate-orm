/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.action.queue.spi.CollectionTransition;

/// Compatibility interpreter for custom collection semantics.
///
/// @since 8.0
/// @author Steve Ebersole
final class LegacyCompatibleCollectionMutationInterpreter implements CollectionMutationInterpreter {
	static final LegacyCompatibleCollectionMutationInterpreter INSTANCE =
			new LegacyCompatibleCollectionMutationInterpreter();

	@Override
	public CollectionInterpretationProduction interpret(CollectionInterpretationContext context) {
		final var collection = context.collection();
		final var transition = context.transition();
		if ( requiresCurrentState( transition ) && !collection.wasInitialized() ) {
			if ( !collection.hasQueuedOperations() ) {
				return CollectionInterpretationProduction.initializationRequired();
			}
			return interpretQueuedOperations( context );
		}

		final SemanticCollectionChange semanticChange = context.semanticDeltaRequired()
				? semanticDelta( context )
				: transition == CollectionTransition.REMOVE
						? SemanticCollectionChange.bulkRemoval()
						: SemanticCollectionChange.none();
		final PhysicalCollectionMutation physicalMutation = switch ( transition ) {
			case NONE -> PhysicalCollectionMutation.noWork();
			case REMOVE -> new PhysicalCollectionMutation.RemoveAll( removalMode( context ) );
			case CREATE -> new PhysicalCollectionMutation.CreateAll( currentRows( context ) );
			case UPDATE -> interpretUpdate( context );
			case REMOVE_AND_CREATE -> throw new IllegalArgumentException(
					"REMOVE_AND_CREATE must be split before collection interpretation"
			);
		};
		return CollectionInterpretationProduction.produced( new CollectionMutationInterpretation(
				transition,
				semanticChange,
				physicalMutation,
				collection.getMutationGeneration()
		) );
	}

	private static CollectionInterpretationProduction interpretQueuedOperations(
			CollectionInterpretationContext context) {
		return CollectionInterpretationProduction.initializationRequired();
	}

	private static PhysicalCollectionMutation interpretUpdate(CollectionInterpretationContext context) {
		final var collection = context.collection();
		if ( collection.empty() ) {
			return context.emptySnapshot()
					? PhysicalCollectionMutation.noWork()
					: new PhysicalCollectionMutation.RemoveAll( PhysicalCollectionMutation.RemovalMode.EXECUTE );
		}
		if ( collection.needsRecreate( context.persister() ) ) {
			return new PhysicalCollectionMutation.RemoveAllAndCreateAll(
					context.emptySnapshot()
							? PhysicalCollectionMutation.RemovalMode.SKIP
							: PhysicalCollectionMutation.RemovalMode.EXECUTE,
					currentRows( context )
			);
		}

		final var existing = collection.getChangeSet( context.persister() );
		if ( existing != null ) {
			return new PhysicalCollectionMutation.RowChanges( existing );
		}
		final var delta = produceDelta( context );
		return new PhysicalCollectionMutation.RowChanges( toChangeSet( delta ) );
	}

	private static SemanticCollectionChange semanticDelta(CollectionInterpretationContext context) {
		return new SemanticCollectionChange.Delta( produceDelta( context ) );
	}

	private static CollectionDelta produceDelta(CollectionInterpretationContext context) {
		final List<CollectionChange> changes;
		if ( context.baseline() == CollectionBaseline.EMPTY ) {
			changes = collectCreateChanges( context );
		}
		else {
			final var existing = context.collection().getChangeSet( context.persister() );
			changes = existing == null
					? collectLegacyComparisonChanges( context )
					: adaptChangeSet( existing );
		}
		return new CollectionDelta(
				context.baseline(),
				DeltaCoverage.COMPLETE,
				changes,
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		);
	}

	private static List<CollectionChange> collectCreateChanges(CollectionInterpretationContext context) {
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

	private static List<CollectionChange> collectLegacyComparisonChanges(CollectionInterpretationContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final var changes = new ArrayList<CollectionChange>();
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
				changes.add( new CollectionChange.Addition( collection.getElement( entry ), currentPosition ) );
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

	private static FrozenCollectionRows currentRows(
			CollectionInterpretationContext context) {
		return FrozenCollectionRows.from( context.collection(), context.persister() );
	}

	private static CollectionChangeSet toChangeSet(CollectionDelta delta) {
		final var removals = new ArrayList<CollectionChangeSet.Removal>();
		final var additions = new ArrayList<CollectionChangeSet.Addition>();
		final var shifts = new ArrayList<CollectionChangeSet.Shift>();
		final var valueChanges = new ArrayList<CollectionChangeSet.ValueChange>();
		for ( var change : delta.changes() ) {
			if ( change instanceof CollectionChange.Addition addition ) {
				additions.add( new CollectionChangeSet.Addition(
						addition.element(),
						addition.currentPosition()
				) );
			}
			else if ( change instanceof CollectionChange.Removal removal ) {
				removals.add( new CollectionChangeSet.Removal(
						removal.element(),
						removal.snapshotPosition()
				) );
			}
			else if ( change instanceof CollectionChange.Replacement replacement ) {
				if ( Objects.equals( replacement.snapshotPosition(), replacement.currentPosition() ) ) {
					valueChanges.add( new CollectionChangeSet.ValueChange(
							replacement.snapshotValue(),
							replacement.currentValue(),
							replacement.currentPosition()
					) );
				}
				else {
					removals.add( new CollectionChangeSet.Removal(
							replacement.snapshotValue(),
							replacement.snapshotPosition()
					) );
					additions.add( new CollectionChangeSet.Addition(
							replacement.currentValue(),
							replacement.currentPosition()
					) );
				}
			}
			else if ( change instanceof CollectionChange.PositionChange positionChange ) {
				shifts.add( new CollectionChangeSet.Shift(
						positionChange.element(),
						positionChange.snapshotPosition(),
						positionChange.currentPosition()
				) );
			}
			else if ( change instanceof CollectionChange.Clear ) {
				throw new IllegalStateException( "Bulk clear cannot be represented as row changes" );
			}
		}
		return new CollectionChangeSet( removals, additions, shifts, valueChanges );
	}

	private static boolean requiresCurrentState(CollectionTransition transition) {
		return transition == CollectionTransition.CREATE || transition == CollectionTransition.UPDATE;
	}

	private static PhysicalCollectionMutation.RemovalMode removalMode(
			CollectionInterpretationContext context) {
		return context.emptySnapshot() || context.removalSkipped()
				? PhysicalCollectionMutation.RemovalMode.SKIP
				: PhysicalCollectionMutation.RemovalMode.EXECUTE;
	}

	private LegacyCompatibleCollectionMutationInterpreter() {
	}
}
