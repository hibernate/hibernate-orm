/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.internal.log.LoggingHelper;
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
		this.mapKeyAssembler = mapKeyFetch.createAssembler( this, creationState );
		this.mapValueAssembler = mapValueFetch.createAssembler( this, creationState );
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		super.forEachSubInitializer( consumer, data );
		final Initializer<?> keyInitializer = mapKeyAssembler.getInitializer();
		if ( keyInitializer != null ) {
			consumer.accept( keyInitializer, data.getRowProcessingState() );
		}
		final Initializer<?> valueInitializer = mapValueAssembler.getInitializer();
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
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object key = mapKeyAssembler.assemble( rowProcessingState );
		if ( key == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		final Object value = mapValueAssembler.assemble( rowProcessingState );
		if ( value == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		loadingState.add( new Object[] { key, value } );
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final Initializer<?> keyInitializer = mapKeyAssembler.getInitializer();
		final Initializer<?> valueInitializer = mapValueAssembler.getInitializer();
		if ( keyInitializer != null || valueInitializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentMap<?, ?> map = getCollectionInstance( data );
			assert map != null;
			for ( Map.Entry<?, ?> entry : map.entrySet() ) {
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
		final Initializer<?> keyInitializer = mapKeyAssembler.getInitializer();
		final Initializer<?> valueInitializer = mapValueAssembler.getInitializer();
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
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
		return "MapInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
