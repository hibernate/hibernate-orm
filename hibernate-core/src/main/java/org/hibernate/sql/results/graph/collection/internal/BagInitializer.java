/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentIdentifierBag;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Initializer for both {@link PersistentBag} and {@link PersistentIdentifierBag}
 * collections
 *
 * @author Steve Ebersole
 */
public class BagInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler elementAssembler;
	private final DomainResultAssembler collectionIdAssembler;

	public BagInitializer(
			PluralAttributeMapping bagDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler elementAssembler,
			DomainResultAssembler collectionIdAssembler) {
		super( navigablePath, bagDescriptor, parentAccess, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.elementAssembler = elementAssembler;
		this.collectionIdAssembler = collectionIdAssembler;
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List loadingState,
			RowProcessingState rowProcessingState) {
		if ( collectionIdAssembler != null ) {
			final Object[] row = new Object[2];
			row[0] = collectionIdAssembler.assemble( rowProcessingState );
			row[1] = elementAssembler.assemble( rowProcessingState );

			//noinspection unchecked
			loadingState.add( row );
		}
		else {
			//noinspection unchecked
			loadingState.add( elementAssembler.assemble( rowProcessingState ) );
		}
	}

	@Override
	public String toString() {
		return "BagInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
