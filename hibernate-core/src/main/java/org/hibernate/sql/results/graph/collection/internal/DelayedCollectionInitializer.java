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
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer {

	public DelayedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess,
			DomainResultAssembler<?> collectionKeyResultAssembler) {
		super( fetchedPath, fetchedMapping, parentAccess, collectionKeyResultAssembler );
		assert collectionKeyResultAssembler != null;
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		resolveInstance( rowProcessingState, false );
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
	}

	@Override
	public String toString() {
		return "DelayedCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );
		collectionInstance = null;
	}

}
