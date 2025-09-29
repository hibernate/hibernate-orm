/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractNonJoinCollectionInitializer<AbstractCollectionInitializer.CollectionInitializerData> {

	public DelayedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			InitializerParent<?> parent,
			DomainResult<?> collectionKeyResult,
			AssemblerCreationState creationState) {
		super( fetchedPath, fetchedMapping, parent, collectionKeyResult, false, creationState );
	}

	@Override
	public void resolveInstance(CollectionInitializerData data) {
		resolveInstance( data, false );
	}

	@Override
	public void resolveInstance(Object instance, CollectionInitializerData data) {
		resolveInstance( instance, data, false );
	}

	@Override
	public boolean isEager() {
		// No need to call resolve on this initializer if parent is initialized
		return false;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return false;
	}

	@Override
	public String toString() {
		return "DelayedCollectionInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}

}
