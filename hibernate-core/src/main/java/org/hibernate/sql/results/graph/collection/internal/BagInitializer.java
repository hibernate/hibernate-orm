/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.collection.spi.PersistentIdentifierBag;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * Initializer for both {@link PersistentBag} and {@link PersistentIdentifierBag}
 * collections
 *
 * @author Steve Ebersole
 */
public class BagInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {

	private final DomainResultAssembler<?> elementAssembler;
	private final DomainResultAssembler<?> collectionIdAssembler;

	public BagInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping bagDescriptor,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState,
			Fetch elementFetch,
			@Nullable Fetch collectionIdFetch) {
		super(
				navigablePath,
				bagDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
		elementAssembler = elementFetch.createAssembler( this, creationState );
		collectionIdAssembler =
				collectionIdFetch == null
						? null
						: collectionIdFetch.createAssembler( this, creationState );
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		super.forEachSubInitializer( consumer, data );
		final var initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final var rowProcessingState = data.getRowProcessingState();
		if ( collectionIdAssembler != null ) {
			final Object collectionId = collectionIdAssembler.assemble( rowProcessingState );
			if ( collectionId != null ) {
				final Object element = elementAssembler.assemble( rowProcessingState );
				if ( element != null ) {
					loadingState.add( new Object[] {collectionId, element} );
				}
				// otherwise, if element is null, then NotFoundAction must be IGNORE
			}
		}
		else {
			final Object element = elementAssembler.assemble( rowProcessingState );
			if ( element != null ) {
				// If element is null, then NotFoundAction must be IGNORE
				loadingState.add( element );
			}
		}
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final var initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final var rowProcessingState = data.getRowProcessingState();
			final var persistentCollection = getCollectionInstance( data );
			assert persistentCollection != null;
			if ( persistentCollection instanceof PersistentBag<?> bag ) {
				for ( Object element : bag ) {
					initializer.initializeInstanceFromParent( element, rowProcessingState );
				}
			}
			else if ( persistentCollection instanceof PersistentIdentifierBag<?> idbag ) {
				for ( Object element : idbag ) {
					initializer.initializeInstanceFromParent( element, rowProcessingState );
				}
			}
			else {
				throw new AssertionFailure( "Unexpected collection type" );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final var initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			initializer.resolveKey( data.getRowProcessingState() );
		}
	}

	@Override
	public DomainResultAssembler<?> getIndexAssembler() {
		return null;
	}

	@Override
	public DomainResultAssembler<?> getElementAssembler() {
		return elementAssembler;
	}

	@Override
	public String toString() {
		return "BagInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}
}
