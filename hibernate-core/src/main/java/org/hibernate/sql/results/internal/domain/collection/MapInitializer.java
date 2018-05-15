/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

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
			PersistentCollectionDescriptor collectionDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler mapKeyAssembler,
			DomainResultAssembler mapValueAssembler) {
		super( collectionDescriptor, parentAccess, navigablePath, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.mapKeyAssembler = mapKeyAssembler;
		this.mapValueAssembler = mapValueAssembler;
	}

	@Override
	public PersistentMap getCollectionInstance() {
		return (PersistentMap) super.getCollectionInstance();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void readCollectionRow(RowProcessingState rowProcessingState) {
		getCollectionInstance().load(
				mapKeyAssembler.assemble( rowProcessingState ),
				mapValueAssembler.assemble( rowProcessingState )
		);
	}

	@Override
	public String toString() {
		return "MapInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
