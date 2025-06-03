/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.collection.spi.PersistentArrayHolder;
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
 * @author Chris Cranford
 */
public class ArrayInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {
	private final DomainResultAssembler<Integer> listIndexAssembler;
	private final DomainResultAssembler<?> elementAssembler;

	private final int indexBase;

	public ArrayInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping arrayDescriptor,
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
				arrayDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
		//noinspection unchecked
		this.listIndexAssembler = (DomainResultAssembler<Integer>) listIndexFetch.createAssembler( this, creationState );
		this.elementAssembler = elementFetch.createAssembler( this, creationState );
		this.indexBase = getCollectionAttributeMapping().getIndexMetadata().getListIndexBase();
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
	public @Nullable PersistentArrayHolder<?> getCollectionInstance(ImmediateCollectionInitializerData data) {
		return (PersistentArrayHolder<?>) super.getCollectionInstance( data );
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Integer indexValue = listIndexAssembler.assemble( rowProcessingState );
		if ( indexValue == null ) {
			throw new HibernateException( "Illegal null value for array index encountered while reading: "
					+ getCollectionAttributeMapping().getNavigableRole() );
		}
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		int index = indexValue;

		if ( indexBase != 0 ) {
			index -= indexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			loadingState.add( i, null );
		}

		loadingState.set( index, element );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, ImmediateCollectionInitializerData data) {
		final Object[] array = (Object[]) getInitializedPart().getValue( parentInstance );
		assert array != null;
		data.setCollectionInstance( data.getRowProcessingState().getSession()
				.getPersistenceContextInternal()
				.getCollectionHolder( array ) );
		data.setState( State.INITIALIZED );
		initializeSubInstancesFromParent( data );
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final Iterator iter = getCollectionInstance( data ).elements();
			while ( iter.hasNext() ) {
				initializer.initializeInstanceFromParent( iter.next(), rowProcessingState );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			Integer index = listIndexAssembler.assemble( rowProcessingState );
			if ( index != null ) {
				final PersistentArrayHolder<?> arrayHolder = getCollectionInstance( data );
				assert arrayHolder != null;
				if ( indexBase != 0 ) {
					index -= indexBase;
				}
				initializer.resolveInstance( Array.get( arrayHolder.getArray(), index ), rowProcessingState );
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
		return "ArrayInitializer{" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
