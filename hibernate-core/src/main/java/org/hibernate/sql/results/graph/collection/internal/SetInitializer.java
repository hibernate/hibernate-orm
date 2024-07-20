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
import org.hibernate.collection.spi.PersistentSet;
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
 * @author Steve Ebersole
 */
public class SetInitializer extends AbstractImmediateCollectionInitializer<AbstractImmediateCollectionInitializer.ImmediateCollectionInitializerData> {
	private static final String CONCRETE_NAME = SetInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> elementAssembler;

	public SetInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping setDescriptor,
			InitializerParent<?> parent,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			boolean isResultInitializer,
			AssemblerCreationState creationState,
			Fetch elementFetch) {
		super(
				navigablePath,
				setDescriptor,
				parent,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState
		);
		this.elementAssembler = elementFetch.createAssembler( this, creationState );
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
	public @Nullable PersistentSet<?> getCollectionInstance(ImmediateCollectionInitializerData data) {
		return (PersistentSet<?>) super.getCollectionInstance( data );
	}

	@Override
	protected void readCollectionRow(ImmediateCollectionInitializerData data, List<Object> loadingState) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		loadingState.add( element );
	}

	@Override
	protected void initializeSubInstancesFromParent(ImmediateCollectionInitializerData data) {
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentSet<?> set = getCollectionInstance( data );
			assert set != null;
			for ( Object element : set ) {
				initializer.initializeInstanceFromParent( element, rowProcessingState );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(ImmediateCollectionInitializerData data) {
		final Initializer<?> initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final RowProcessingState rowProcessingState = data.getRowProcessingState();
			final PersistentSet<?> set = getCollectionInstance( data );
			assert set != null;
			for ( Object element : set ) {
				initializer.resolveInstance( element, rowProcessingState );
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
		return "SetInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
