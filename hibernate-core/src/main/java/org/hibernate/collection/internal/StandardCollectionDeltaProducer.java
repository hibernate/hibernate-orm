/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.CollectionBaseline;
import org.hibernate.collection.spi.CollectionChange;
import org.hibernate.collection.spi.CollectionDelta;
import org.hibernate.collection.spi.CollectionDeltaProducer;
import org.hibernate.collection.spi.CollectionDeltaProduction;
import org.hibernate.collection.spi.CollectionDeltaProductionContext;
import org.hibernate.collection.spi.DeferredCollectionPosition;
import org.hibernate.collection.spi.DeferredCollectionIdentifier;
import org.hibernate.collection.spi.DeltaCoverage;
import org.hibernate.collection.spi.DeltaSource;
import org.hibernate.collection.spi.PersistedCollectionSize;
import org.hibernate.collection.spi.QueuedCollectionOperation;

/// Shared immutable producers for Hibernate's standard collection semantics.
///
/// @since 8.0
/// @author Steve Ebersole
enum StandardCollectionDeltaProducer implements CollectionDeltaProducer {
	ARRAY,
	BAG,
	IDENTIFIER_BAG,
	LIST,
	MAP,
	SET;

	@Override
	public CollectionDeltaProduction produceDelta(CollectionDeltaProductionContext context) {
		final var collection = context.collection();
		if ( collection.wasInitialized() ) {
			if ( this == IDENTIFIER_BAG ) {
				return CollectionDeltaProduction.produced( normalizeIdentifierBag( context ) );
			}
			return CollectionDeltaProducer.legacyCompatible().produceDelta( context );
		}
		if ( !collection.hasQueuedOperations() ) {
			return CollectionDeltaProduction.initializationRequired();
		}

		final var operations = collection.getQueuedOperations();
		final List<CollectionChange> changes = switch ( this ) {
			case ARRAY, IDENTIFIER_BAG -> throw new IllegalStateException(
					"Queued operations are not supported for " + this + " semantics"
			);
			case BAG, SET -> normalizeUnindexed( operations );
			case MAP -> normalizeMap( operations );
			case LIST -> normalizeList( operations );
		};
		return CollectionDeltaProduction.produced( new CollectionDelta(
				CollectionBaseline.UNINITIALIZED,
				DeltaCoverage.EXPLICIT_CHANGES_ONLY,
				changes,
				Set.of( DeltaSource.QUEUED_OPERATION_LOG )
		) );
	}

	private static CollectionDelta normalizeIdentifierBag(
			CollectionDeltaProductionContext context) {
		final var collection = context.collection();
		final var persister = context.persister();
		final var changes = new ArrayList<CollectionChange>();
		final Map<?, ?> snapshot = collection.getStoredSnapshot() instanceof Map<?, ?> map
				? map
				: Map.of();

		if ( context.baseline() != CollectionBaseline.EMPTY ) {
			final var deletions = collection.getDeletes( persister, true );
			while ( deletions.hasNext() ) {
				final Object identifier = deletions.next();
				changes.add( new CollectionChange.Removal( snapshot.get( identifier ), identifier ) );
			}
		}

		final var entries = collection.entries( persister );
		int position = 0;
		while ( entries.hasNext() ) {
			final Object entry = entries.next();
			final Object identifier = collection.getIdentifier( entry, position );
			final Object rowIdentity = identifier == null
					? new DeferredCollectionIdentifier( collection, position )
					: identifier;
			if ( context.baseline() == CollectionBaseline.EMPTY
						? collection.includeInRecreate(
								entry,
								position,
								collection,
								persister.getAttributeMapping()
						)
						: collection.includeInInsert(
								entry,
								position,
								collection,
								persister.getAttributeMapping()
						) ) {
				changes.add( new CollectionChange.Addition( collection.getElement( entry ), rowIdentity ) );
			}
			else if ( context.baseline() != CollectionBaseline.EMPTY
					&& collection.needsUpdating( entry, position, persister.getAttributeMapping() ) ) {
				changes.add( new CollectionChange.Replacement(
						identifier == null ? null : snapshot.get( identifier ),
						collection.getElement( entry ),
						rowIdentity,
						rowIdentity
				) );
			}
			position++;
		}

		return new CollectionDelta(
				context.baseline(),
				DeltaCoverage.COMPLETE,
				changes,
				Set.of( DeltaSource.SNAPSHOT_COMPARISON )
		);
	}

	private static List<CollectionChange> normalizeUnindexed(
			List<QueuedCollectionOperation> operations) {
		final var changes = new ArrayList<CollectionChange>( operations.size() );
		for ( var operation : operations ) {
			switch ( operation.kind() ) {
				case ADD -> changes.add( new CollectionChange.Addition( operation.addedValue(), null ) );
				case REMOVE -> changes.add( new CollectionChange.Removal( operation.orphan(), null ) );
				case CLEAR -> changes.add( new CollectionChange.Clear() );
				default -> throw unexpected( operation );
			}
		}
		return changes;
	}

	private static List<CollectionChange> normalizeMap(
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
				default -> throw unexpected( operation );
			}
		}
		return changes;
	}

	private static List<CollectionChange> normalizeList(
			List<QueuedCollectionOperation> operations) {
		final var changes = new ArrayList<CollectionChange>( operations.size() );
		final var persistedSize = new PersistedCollectionSize();
		boolean cleared = false;
		int relativeSize = 0;
		for ( var operation : operations ) {
			switch ( operation.kind() ) {
				case ADD -> {
					final Object position;
					if ( operation.position() != null ) {
						position = operation.position();
					}
					else if ( cleared ) {
						position = relativeSize;
					}
					else {
						position = new DeferredCollectionPosition( persistedSize, relativeSize );
					}
					changes.add( new CollectionChange.Addition( operation.addedValue(), position ) );
					relativeSize++;
				}
				case REMOVE -> {
					changes.add( new CollectionChange.Removal(
							operation.orphan(),
							operation.position()
					) );
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
				default -> throw unexpected( operation );
			}
		}
		return changes;
	}

	private static IllegalArgumentException unexpected(QueuedCollectionOperation operation) {
		return new IllegalArgumentException(
				"Unexpected queued " + operation.kind() + " operation at order " + operation.order()
		);
	}
}
