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
 * @author Steve Ebersole
 */
public class SetInitializer extends AbstractImmediateCollectionInitializer {
	private static final String CONCRETE_NAME = SetInitializer.class.getSimpleName();

	private final DomainResultAssembler<?> elementAssembler;

	/**
	 * @deprecated Use {@link #SetInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, LockMode, DomainResult, DomainResult, boolean, AssemblerCreationState, Fetch)} instead.
	 */
	@Deprecated(forRemoval = true)
	public SetInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping setDescriptor,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResult<?> collectionKeyResult,
			DomainResult<?> collectionValueKeyResult,
			Fetch elementFetch,
			boolean isResultInitializer,
			AssemblerCreationState creationState) {
		this(
				navigablePath,
				setDescriptor,
				(InitializerParent) parentAccess,
				lockMode,
				collectionKeyResult,
				collectionValueKeyResult,
				isResultInitializer,
				creationState,
				elementFetch
		);
	}

	public SetInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping setDescriptor,
			InitializerParent parent,
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
		this.elementAssembler = elementFetch.createAssembler( (InitializerParent) this, creationState );
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
	public @Nullable PersistentSet<?> getCollectionInstance() {
		return (PersistentSet<?>) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState) {
		final Object element = elementAssembler.assemble( rowProcessingState );
		if ( element == null ) {
			// If element is null, then NotFoundAction must be IGNORE
			return;
		}
		loadingState.add( element );
	}

	@Override
	protected void initializeSubInstancesFromParent(RowProcessingState rowProcessingState) {
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final PersistentSet<?> set = getCollectionInstance();
			assert set != null;
			for ( Object element : set ) {
				initializer.initializeInstanceFromParent( element );
			}
		}
	}

	@Override
	protected void resolveInstanceSubInitializers(RowProcessingState rowProcessingState) {
		final Initializer initializer = elementAssembler.getInitializer();
		if ( initializer != null ) {
			final PersistentSet<?> set = getCollectionInstance();
			assert set != null;
			for ( Object element : set ) {
				initializer.resolveInstance( element );
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
