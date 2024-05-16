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
public class MapInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = MapInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> mapKeyAssembler;
	private final DomainResultAssembler<?> mapValueAssembler;

	/**
	 * @deprecated Use {@link #MapInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, LockMode, DomainResult, DomainResult, boolean, AssemblerCreationState, Fetch, Fetch)} instead.
	 */
	@Deprecated(forRemoval = true)
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
		this(
				navigablePath,
				attributeMapping,
				(InitializerParent) parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState,
				mapKeyFetch,
				mapValueFetch
		);
	}

	public MapInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			InitializerParent parent,
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
		this.mapKeyAssembler = mapKeyFetch.createAssembler( (InitializerParent) this, creationState );
		this.mapValueAssembler = mapValueFetch.createAssembler( (InitializerParent) this, creationState );
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		super.forEachSubInitializer( consumer, arg );
		final Initializer keyInitializer = mapKeyAssembler.getInitializer();
		if ( keyInitializer != null ) {
			consumer.accept( keyInitializer, arg );
		}
		final Initializer valueInitializer = mapValueAssembler.getInitializer();
		if ( valueInitializer != null ) {
			consumer.accept( valueInitializer, arg );
		}
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
					keyInitializer.initializeInstanceFromParent( entry.getKey() );
				}
				if ( valueInitializer != null ) {
					valueInitializer.initializeInstanceFromParent( entry.getValue() );
				}
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(RowProcessingState rowProcessingState) {
		final Initializer keyInitializer = mapKeyAssembler.getInitializer();
		final Initializer valueInitializer = mapValueAssembler.getInitializer();
		if ( keyInitializer != null || valueInitializer != null ) {
			final PersistentMap<?, ?> map = getCollectionInstance();
			assert map != null;
			for ( Map.Entry<?, ?> entry : map.entrySet() ) {
				if ( keyInitializer != null ) {
					keyInitializer.resolveInstance( entry.getKey() );
				}
				if ( valueInitializer != null ) {
					valueInitializer.resolveInstance( entry.getValue() );
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
