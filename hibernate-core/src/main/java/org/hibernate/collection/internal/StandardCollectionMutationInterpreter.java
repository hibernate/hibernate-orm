/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.action.queue.spi.CollectionTransition;
import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionChangeSet;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionInterpretationContext;
import org.hibernate.collection.spi.CollectionInterpretationProduction;
import org.hibernate.collection.spi.CollectionMutationInterpretation;
import org.hibernate.collection.spi.CollectionMutationInterpreter;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.DeferredCollectionIdentifier;
import org.hibernate.collection.spi.DeferredCollectionPosition;
import org.hibernate.collection.spi.PersistedCollectionSize;
import org.hibernate.collection.spi.QueuedCollectionOperation;
import org.hibernate.collection.spi.PhysicalCollectionMutation;
import org.hibernate.collection.spi.SemanticCollectionChange;
import org.hibernate.persister.collection.AbstractCollectionPersister;

/// Shared immutable interpreters for Hibernate's standard collection semantics.
///
/// @since 8.0
/// @author Steve Ebersole
enum StandardCollectionMutationInterpreter implements CollectionMutationInterpreter {
	ARRAY,
	BAG,
	IDENTIFIER_BAG,
	LIST,
	MAP,
	SET;

	@Override
	public CollectionInterpretationProduction interpret(CollectionInterpretationContext context) {
		final var collection = context.collection();
		if ( context.transition() == CollectionTransition.REMOVE ) {
			return CollectionInterpretationProduction.produced( new CollectionMutationInterpretation(
					CollectionTransition.REMOVE,
					SemanticCollectionChange.bulkRemoval(),
					new PhysicalCollectionMutation.RemoveAll(
							context.emptySnapshot() || context.removalSkipped()
									? PhysicalCollectionMutation.RemovalMode.SKIP
									: PhysicalCollectionMutation.RemovalMode.EXECUTE
					),
					collection.getMutationGeneration()
			) );
		}
		if ( !collection.wasInitialized() ) {
			if ( !collection.hasQueuedOperations() ) {
				return CollectionInterpretationProduction.initializationRequired();
			}
			final var delta = queuedOperationDelta( collection.getQueuedOperations() );
			return CollectionInterpretationProduction.produced( new CollectionMutationInterpretation(
					context.transition(),
					new SemanticCollectionChange.Delta( delta ),
					PhysicalCollectionMutation.noWork(),
					collection.getMutationGeneration()
			) );
		}

		if ( context.transition() != CollectionTransition.UPDATE
				|| collection.empty()
				|| collection.needsRecreate( context.persister() ) ) {
			return CollectionMutationInterpreter.legacyCompatible().interpret( context );
		}

		final CollectionChangeSet changes;
		if ( this == IDENTIFIER_BAG ) {
			changes = collectIdentifierBagRowChanges( context );
		}
		else if ( this == LIST || this == MAP ) {
			changes = collection.getChangeSet( context.persister() );
		}
		else {
			changes = collectRowChanges( context );
		}
		if ( changes == null ) {
			return CollectionMutationInterpreter.legacyCompatible().interpret( context );
		}
		final SemanticCollectionChange semanticChange = context.semanticDeltaRequired()
				? new SemanticCollectionChange.Delta( toDelta( context.baseline(), changes ) )
				: SemanticCollectionChange.none();
		return CollectionInterpretationProduction.produced( new CollectionMutationInterpretation(
				context.transition(),
				semanticChange,
				new PhysicalCollectionMutation.RowChanges( changes ),
				collection.getMutationGeneration()
		) );
	}

	private CollectionDelta queuedOperationDelta(List<QueuedCollectionOperation> operations) {
		final List<CollectionChange> changes = switch ( this ) {
			case ARRAY, IDENTIFIER_BAG -> throw new IllegalStateException(
					"Queued operations are not supported for " + this + " semantics"
			);
			case BAG, SET -> normalizeUnindexedQueuedOperations( operations );
			case MAP -> normalizeMapQueuedOperations( operations );
			case LIST -> normalizeListQueuedOperations( operations );
		};
		return new CollectionDelta(
				CollectionBaseline.UNINITIALIZED,
				DeltaCoverage.EXPLICIT_CHANGES_ONLY,
				changes,
				Set.of( DeltaSource.QUEUED_OPERATION_LOG )
		);
	}

	private static List<CollectionChange> normalizeUnindexedQueuedOperations(
			List<QueuedCollectionOperation> operations) {
		final var changes = new ArrayList<CollectionChange>( operations.size() );
		for ( var operation : operations ) {
			switch ( operation.kind() ) {
				case ADD -> changes.add( new CollectionChange.Addition( operation.addedValue(), null ) );
				case REMOVE -> changes.add( new CollectionChange.Removal( operation.orphan(), null ) );
				case CLEAR -> changes.add( new CollectionChange.Clear() );
				default -> throw unexpectedQueuedOperation( operation );
			}
		}
		return changes;
	}

	private static List<CollectionChange> normalizeMapQueuedOperations(
			List<QueuedCollectionOperation> operations) {
		final var changes = new ArrayList<CollectionChange>( operations.size() );
		for ( var operation : operations ) {
			switch ( operation.kind() ) {
				case PUT -> changes.add( new CollectionChange.Replacement(
						operation.orphan(),
						operation.addedValue(),
						operation.position(),
						operation.position()
				) );
				case REMOVE -> changes.add( new CollectionChange.Removal(
						operation.orphan(),
						operation.position()
				) );
				case CLEAR -> changes.add( new CollectionChange.Clear() );
				default -> throw unexpectedQueuedOperation( operation );
			}
		}
		return changes;
	}

	private static List<CollectionChange> normalizeListQueuedOperations(
			List<QueuedCollectionOperation> operations) {
		final var changes = new ArrayList<CollectionChange>( operations.size() );
		final var persistedSize = new PersistedCollectionSize();
		boolean cleared = false;
		int relativeSize = 0;
		for ( var operation : operations ) {
			switch ( operation.kind() ) {
				case ADD -> {
					final Object position = operation.position() != null
							? operation.position()
							: cleared
									? relativeSize
									: new DeferredCollectionPosition( persistedSize, relativeSize );
					changes.add( new CollectionChange.Addition( operation.addedValue(), position ) );
					relativeSize++;
				}
				case REMOVE -> {
					changes.add( new CollectionChange.Removal( operation.orphan(), operation.position() ) );
					relativeSize--;
				}
				case SET -> changes.add( new CollectionChange.Replacement(
						operation.orphan(),
						operation.addedValue(),
						operation.position(),
						operation.position()
				) );
				case CLEAR -> {
					changes.add( new CollectionChange.Clear() );
					cleared = true;
					relativeSize = 0;
				}
				default -> throw unexpectedQueuedOperation( operation );
			}
		}
		return changes;
	}

	private static IllegalArgumentException unexpectedQueuedOperation(QueuedCollectionOperation operation) {
		return new IllegalArgumentException(
				"Unexpected queued " + operation.kind() + " operation at order " + operation.order()
		);
	}

	private static CollectionChangeSet collectIdentifierBagRowChanges(CollectionInterpretationContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final Map<?, ?> snapshot = collection.getStoredSnapshot() instanceof Map<?, ?> map
				? map
				: Map.of();
		final var removals = new ArrayList<CollectionChangeSet.Removal>();
		final var additions = new ArrayList<CollectionChangeSet.Addition>();
		final var valueChanges = new ArrayList<CollectionChangeSet.ValueChange>();

		final var deletions = collection.getDeletes( persister, true );
		while ( deletions.hasNext() ) {
			final Object identifier = deletions.next();
			removals.add( new CollectionChangeSet.Removal( snapshot.get( identifier ), identifier ) );
		}

		final var entries = collection.entries( persister );
		int position = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			final Object identifier = collection.getIdentifier( entry, position );
			final Object rowIdentity = identifier == null
					? new DeferredCollectionIdentifier( collection, position )
					: identifier;
			if ( collection.includeInInsert(
					entry,
					position,
					collection,
					persister.getAttributeMapping() ) ) {
				additions.add( new CollectionChangeSet.Addition(
						collection.getElement( entry ),
						rowIdentity
				) );
			}
			else if ( collection.needsUpdating( entry, position, persister.getAttributeMapping() ) ) {
				valueChanges.add( new CollectionChangeSet.ValueChange(
						identifier == null ? null : snapshot.get( identifier ),
						collection.getElement( entry ),
						rowIdentity
				) );
			}
			position++;
		}
		return new CollectionChangeSet( removals, additions, List.of(), valueChanges );
	}

	private CollectionChangeSet collectRowChanges(CollectionInterpretationContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final var removals = new ArrayList<CollectionChangeSet.Removal>();
		final var additions = new ArrayList<CollectionChangeSet.Addition>();
		final var valueChanges = new ArrayList<CollectionChangeSet.ValueChange>();

		final boolean indexIsFormula = !(persister instanceof AbstractCollectionPersister abstractPersister)
				|| !abstractPersister.hasPhysicalIndexColumn();
		final var deletions = collection.getDeletes( persister, indexIsFormula );
		while ( deletions.hasNext() ) {
			removals.add( new CollectionChangeSet.Removal( deletions.next(), null ) );
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
				additions.add( new CollectionChangeSet.Addition(
						collection.getElement( entry ),
						currentPosition
				) );
			}
			else if ( collection.needsUpdating( entry, position, persister.getAttributeMapping() ) ) {
				valueChanges.add( new CollectionChangeSet.ValueChange(
						collection.getSnapshotElement( entry, position ),
						collection.getElement( entry ),
						currentPosition
				) );
			}
			position++;
		}
		return new CollectionChangeSet( removals, additions, List.of(), valueChanges );
	}

	private static CollectionDelta toDelta(CollectionBaseline baseline, CollectionChangeSet changes) {
		final var semanticChanges = new ArrayList<CollectionChange>(
				changes.removals().size()
						+ changes.additions().size()
						+ changes.shifts().size()
						+ changes.valueChanges().size()
		);
		for ( var removal : changes.removals() ) {
			semanticChanges.add( new CollectionChange.Removal(
					removal.element(),
					removal.snapshotIndex()
			) );
		}
		for ( var addition : changes.additions() ) {
			semanticChanges.add( new CollectionChange.Addition( addition.element(), addition.index() ) );
		}
		for ( var shift : changes.shifts() ) {
			semanticChanges.add( new CollectionChange.PositionChange(
					shift.element(),
					shift.snapshotIndex(),
					shift.currentIndex()
			) );
		}
		for ( var valueChange : changes.valueChanges() ) {
			semanticChanges.add( new CollectionChange.Replacement(
					valueChange.oldValue(),
					valueChange.newValue(),
					valueChange.index(),
					valueChange.index()
			) );
		}
		return new CollectionDelta(
				baseline,
				DeltaCoverage.COMPLETE,
				semanticChanges,
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		);
	}

}
