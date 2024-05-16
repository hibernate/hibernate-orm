/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentList;
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
 * CollectionInitializer for PersistentList loading
 * 
 * @author Steve Ebersole
 */
public class ListInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = ListInitializer.class.getSimpleName();

	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int listIndexBase;

	/**
	 * @deprecated Use {@link #ListInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, LockMode, DomainResult, DomainResult, boolean, AssemblerCreationState, Fetch, Fetch)} instead.
	 */
	@Deprecated(forRemoval = true)
	public ListInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			Fetch listIndexFetch,
			Fetch elementFetch,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this(
				navigablePath,
				attributeMapping,
				(InitializerParent) parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer, creationState, listIndexFetch,
				elementFetch
		);
	}

	public ListInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			InitializerParent parent,
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
		this.listIndexAssembler = (DomainResultAssembler<Integer>) listIndexFetch.createAssembler( (InitializerParent) this, creationState );
		this.elementAssembler = elementFetch.createAssembler( (InitializerParent) this, creationState );
		this.listIndexBase = attributeMapping.getIndexMetadata().getListIndexBase();
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		super.forEachSubInitializer( consumer, arg );
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, arg );
		}
	}

	@Override
	public @Nullable PersistentList<?> getCollectionInstance() {
		return (PersistentList<?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for list index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		int index = indexValue;

		if ( listIndexBase != 0 ) {
			index -= listIndexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			loadingState.add( i, null );
		}

		loadingState.set( index, element );
	}

	@Override
	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final PersistentList<?> list = getCollectionInstance();
			assert list != null;
			for ( Object element : list ) {
				initializer.initializeInstanceFromParent( element );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(RowProcessingState rowProcessingState) {
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final PersistentList<?> list = getCollectionInstance();
			assert list != null;
			for ( Object element : list ) {
				initializer.resolveInstance( element );
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
		return "ListInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
