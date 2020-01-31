/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Represents an immediate initialization of some sort (join, select, batch, sub-select)
 * of a persistent Map valued attribute.
 *
 * @see DelayedCollectionInitializer
 *
 * @author Steve Ebersole
 */
public class MapInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler mapKeyAssembler;
	private final DomainResultAssembler mapValueAssembler;

	public MapInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler mapKeyAssembler,
			DomainResultAssembler mapValueAssembler) {
		super( navigablePath, attributeMapping, parentAccess, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.mapKeyAssembler = mapKeyAssembler;
		this.mapValueAssembler = mapValueAssembler;
	}

	@Override
	public PersistentMap getCollectionInstance() {
		return (PersistentMap) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List loadingState,
			RowProcessingState rowProcessingState) {
		//noinspection unchecked
		loadingState.add(
				new Object[] {
						mapKeyAssembler.assemble( rowProcessingState ),
						mapValueAssembler.assemble( rowProcessingState )
				}
		);
	}

	@Override
	public String toString() {
		return "MapInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
