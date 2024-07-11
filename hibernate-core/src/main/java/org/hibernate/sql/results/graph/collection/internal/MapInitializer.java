/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	private static final String CONCRETE_NAME = MapInitializer.class.getSimpleName();

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
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
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
		if ( keyInitializer != null || valueInitializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentMap<?, ?> map = getCollectionInstance( data );
			assert map != null;
			for ( Map.Entry<?, ?> entry : map.entrySet() ) {
				if ( keyInitializer != null ) {
					keyInitializer.resolveInstance( entry.getKey(), rowProcessingState );
				}
				if ( valueInitializer != null ) {
					valueInitializer.resolveInstance( entry.getValue(), rowProcessingState );
				}
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
