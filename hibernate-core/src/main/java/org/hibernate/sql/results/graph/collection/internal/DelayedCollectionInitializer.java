/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer<AbstractCollectionInitializer.CollectionInitializerData> {

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
	public String toString() {
		return "DelayedCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
