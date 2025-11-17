/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentList;
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
 * CollectionInitializer for PersistentList loading
 *
 * @author Steve Ebersole
 */
public class ListInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {

	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int listIndexBase;

	public ListInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState,
			Fetch listIndexFetch,
			Fetch elementFetch) {
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
		//noinspection unchecked
		listIndexAssembler = (DomainResultAssembler<Integer>) listIndexFetch.createAssembler( this, creationState );
		elementAssembler = elementFetch.createAssembler( this, creationState );
		listIndexBase = attributeMapping.getIndexMetadata().getListIndexBase();
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
	public @Nullable PersistentList<?> getCollectionInstance(ImmediateCollectionInitializerData data) {
		return (PersistentList<?>) super.getCollectionInstance( data );
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final var rowProcessingState = data.getRowProcessingState();
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for list index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element != null ) {
			int index = indexValue;
			if ( listIndexBase != 0 ) {
				index -= listIndexBase;
			}
			for ( int i = loadingState.size(); i <= index; ++i ) {
				loadingState.add( i, null );
			}
			loadingState.set( index, element );
		}
		// else if the element is null, then NotFoundAction must be IGNORE
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final var initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final var rowProcessingState = data.getRowProcessingState();
			final var list = getCollectionInstance( data );
			assert list != null;
			for ( Object element : list ) {
				initializer.initializeInstanceFromParent( element, rowProcessingState );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final var initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final var rowProcessingState = data.getRowProcessingState();
			Integer index = listIndexAssembler.assemble( rowProcessingState );
			if ( index != null ) {
				final var list = getCollectionInstance( data );
				assert list != null;
				if ( listIndexBase != 0 ) {
					index -= listIndexBase;
				}
				initializer.resolveInstance( list.get( index ), rowProcessingState );
			}
		}
	}

	@Override
	public DomainResultAssembler<?> getIndexAssembler() {
		return listIndexAssembler;
	}

	@Override
	public DomainResultAssembler<?> getElementAssembler() {
		return elementAssembler;
	}

	@Override
	public String toString() {
		return "ListInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}
}
