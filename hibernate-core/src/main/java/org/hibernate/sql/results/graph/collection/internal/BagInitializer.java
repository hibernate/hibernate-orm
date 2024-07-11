/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentIdentifierBag;
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
 * Initializer for both {@link PersistentBag} and {@link PersistentIdentifierBag}
 * collections
 *
 * @author Steve Ebersole
 */
public class BagInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {
	private static final String CONCRETE_NAME = BagInitializer.class.getSimpleName();

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
		this.elementAssembler = elementFetch.createAssembler( this, creationState );
		this.collectionIdAssembler = collectionIdFetch == null
				? null
				: collectionIdFetch.createAssembler( this, creationState );
	}

	@Override
	protected String getSimpleConcreteImplName() {
		return CONCRETE_NAME;
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		super.forEachSubInitializer( consumer, data );
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		if ( collectionIdAssembler != null ) {
			final Object collectionId = collectionIdAssembler.assemble( rowProcessingState );
			if ( collectionId == null ) {
				return;
			}
			final Object element = elementAssembler.assemble( rowProcessingState );
			if ( element == null ) {
				// If element is null, then NotFoundAction must be IGNORE
				return;
			}

			loadingState.add( new Object[]{ collectionId, element } );
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
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentCollection<?> persistentCollection = getCollectionInstance( data );
			assert persistentCollection != null;
			if ( persistentCollection instanceof PersistentBag<?> ) {
				for ( Object element : ( (PersistentBag<?>) persistentCollection ) ) {
					initializer.initializeInstanceFromParent( element, rowProcessingState );
				}
			}
			else {
				for ( Object element : ( (PersistentIdentifierBag<?>) persistentCollection ) ) {
					initializer.initializeInstanceFromParent( element, rowProcessingState );
				}
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentCollection<?> persistentCollection = getCollectionInstance( data );
			assert persistentCollection != null;
			if ( persistentCollection instanceof PersistentBag<?> ) {
				for ( Object element : ( (PersistentBag<?>) persistentCollection ) ) {
					initializer.resolveInstance( element, rowProcessingState );
				}
			}
			else {
				for ( Object element : ( (PersistentIdentifierBag<?>) persistentCollection ) ) {
					initializer.resolveInstance( element, rowProcessingState );
				}
			}
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
		return "BagInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
