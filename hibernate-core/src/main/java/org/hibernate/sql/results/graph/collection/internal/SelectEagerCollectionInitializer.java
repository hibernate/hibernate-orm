/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.InitializerParent;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class SelectEagerCollectionInitializer extends AbstractCollectionInitializer {

	/**
	 * @deprecated Use {@link #SelectEagerCollectionInitializer(NavigablePath, PluralAttributeMapping, InitializerParent, DomainResult, AssemblerCreationState)}  instead.
	 */
	@Deprecated(forRemoval = true)
	public SelectEagerCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess,
			@Nullable DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parentAccess, collectionKeyResult, false, creationState );
	}

	public SelectEagerCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			InitializerParent parent,
			@Nullable DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parent, collectionKeyResult, false, creationState );
	}

	@Override
	public void resolveInstance() {
		resolveInstance( rowProcessingState, true );
	}

	@Override
	public void resolveInstance(@Nullable Object instance) {
		resolveInstance( instance, rowProcessingState, true );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final Object instance = getInitializedPart().getValue( parentInstance );
		if ( collectionAttributeMapping.getCollectionDescriptor()
				.getCollectionSemantics()
				.getCollectionClassification() == CollectionClassification.ARRAY ) {
			collectionInstance = rowProcessingState.getSession().getPersistenceContextInternal()
					.getCollectionHolder( instance );
		}
		else {
			collectionInstance = (PersistentCollection<?>) instance;
		}
		state = State.INITIALIZED;
		collectionInstance.forceInitialization();
	}

	@Override
	public String toString() {
		return "SelectEagerCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
