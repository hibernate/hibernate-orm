/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Andrea Boriero
 */
public class SelectEagerCollectionInitializer extends AbstractNonJoinCollectionInitializer<AbstractCollectionInitializer.CollectionInitializerData> {

	public SelectEagerCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			InitializerParent<?> parent,
			@Nullable DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parent, collectionKeyResult, false, creationState );
	}

	@Override
	public void resolveInstance(CollectionInitializerData data) {
		resolveInstance( data, true );
	}

	@Override
	public void resolveInstance(@Nullable Object instance, CollectionInitializerData data) {
		resolveInstance( instance, data, true );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, CollectionInitializerData data) {
		final Object instance = getInitializedPart().getValue( parentInstance );
		if ( instance == null ) {
			setMissing( data );
		}
		else {
			final PersistentCollection<?> collection;
			if ( collectionAttributeMapping.getCollectionDescriptor()
					.getCollectionSemantics()
					.getCollectionClassification() == CollectionClassification.ARRAY ) {
				collection = data.getRowProcessingState().getSession().getPersistenceContextInternal()
						.getCollectionHolder( instance );
			}
			else {
				collection = (PersistentCollection<?>) instance;
			}
			data.setState( State.INITIALIZED );
			data.setCollectionInstance( collection );
			collection.forceInitialization();
		}
	}

	@Override
	public String toString() {
		return "SelectEagerCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
