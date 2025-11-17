/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentMap;
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
 * Represents an immediate initialization of some sort (join, select, batch, sub-select)
 * of a persistent Map valued attribute.
 *
 * @see DelayedCollectionInitializer
 *
 * @author Steve Ebersole
 */
public class MapInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {

	private final DomainResultAssembler<?> mapKeyAssembler;
	private final DomainResultAssembler<?> mapValueAssembler;

	public MapInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState,
			Fetch mapKeyFetch,
			Fetch mapValueFetch) {
		super(
				navigablePath,
				attributeMapping,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
		mapKeyAssembler = mapKeyFetch.createAssembler( this, creationState );
		mapValueAssembler = mapValueFetch.createAssembler( this, creationState );
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		super.forEachSubInitializer( consumer, data );
		final var keyInitializer = mapKeyAssembler.getInitializer();
		if ( keyInitializer != null ) {
			consumer.accept( keyInitializer, data.getRowProcessingState() );
		}
		final var valueInitializer = mapValueAssembler.getInitializer();
		if ( valueInitializer != null ) {
			consumer.accept( valueInitializer, data.getRowProcessingState() );
		}
	}

	@Override
	public @Nullable PersistentMap<?, ?> getCollectionInstance(ImmediateCollectionInitializerData data) {
		return (PersistentMap<?, ?>) super.getCollectionInstance( data );
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final var rowProcessingState = data.getRowProcessingState();
		final Object key = mapKeyAssembler.assemble( rowProcessingState );
		if ( key != null ) {
			final Object value = mapValueAssembler.assemble( rowProcessingState );
			if ( value != null ) {
				loadingState.add( new Object[] {key, value} );
			}
			// else if the element is null, then NotFoundAction must be IGNORE
		}
		// else if the key is null, then NotFoundAction must be IGNORE
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final var keyInitializer = mapKeyAssembler.getInitializer();
		final var valueInitializer = mapValueAssembler.getInitializer();
		if ( keyInitializer != null || valueInitializer != null ) {
			final var rowProcessingState = data.getRowProcessingState();
			final var map = getCollectionInstance( data );
			assert map != null;
			for ( var entry : map.entrySet() ) {
				if ( keyInitializer != null ) {
					keyInitializer.initializeInstanceFromParent( entry.getKey(), rowProcessingState );
				}
				if ( valueInitializer != null ) {
					valueInitializer.initializeInstanceFromParent( entry.getValue(), rowProcessingState );
				}
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final var keyInitializer = mapKeyAssembler.getInitializer();
		final var valueInitializer = mapValueAssembler.getInitializer();
		final var rowProcessingState = data.getRowProcessingState();
		if ( keyInitializer == null && valueInitializer != null ) {
			// For now, we only support resolving the value initializer instance when keys have no initializer,
			// though we could also support map keys with an initializer given that the initialized java type:
			// * is an entity that uses only the primary key in equals/hashCode.
			//   If the primary key type is an embeddable, the next condition must hold for that
			// * or is an embeddable that has no initializers for fields being used in the equals/hashCode
			//   which violate this same requirement (recursion)
			final Object key = mapKeyAssembler.assemble( rowProcessingState );
			if ( key != null ) {
				final PersistentMap<?, ?> map = getCollectionInstance( data );
				assert map != null;
				valueInitializer.resolveInstance( map.get( key ), rowProcessingState );
			}
		}
		else {
			if ( keyInitializer != null ) {
				keyInitializer.resolveKey( rowProcessingState );
			}
			if ( valueInitializer != null ) {
				valueInitializer.resolveKey( rowProcessingState );
			}
		}
	}

	@Override
	public DomainResultAssembler<?> getIndexAssembler() {
		return mapKeyAssembler;
	}

	@Override
	public DomainResultAssembler<?> getElementAssembler() {
		return mapValueAssembler;
	}

	@Override
	public String toString() {
		return "MapInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}
}
