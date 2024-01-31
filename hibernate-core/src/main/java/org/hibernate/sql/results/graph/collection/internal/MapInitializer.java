/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
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
public class MapInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = MapInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> mapKeyAssembler;
	private final DomainResultAssembler<?> mapValueAssembler;

	public MapInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			Fetch mapKeyFetch,
			Fetch mapValueFetch,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		super(
				navigablePath,
				attributeMapping,
				parentAccess,
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
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected void forEachAssembler(Consumer<DomainResultAssembler<?>> consumer) {
		consumer.accept( mapKeyAssembler );
		consumer.accept( mapValueAssembler );
	}

	@Override
	public @Nullable PersistentMap<?, ?> getCollectionInstance() {
		return (PersistentMap<?, ?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
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
	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		final Initializer keyInitializer = mapKeyAssembler.getInitializer();
		final Initializer valueInitializer = mapValueAssembler.getInitializer();
		if ( keyInitializer != null || valueInitializer != null ) {
			final PersistentMap<?, ?> map = getCollectionInstance();
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
	public String toString() {
		return "MapInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
