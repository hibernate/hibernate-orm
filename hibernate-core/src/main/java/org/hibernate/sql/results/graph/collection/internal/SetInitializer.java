/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class SetInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler elementAssembler;

	public SetInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping setDescriptor,
			FetchParentAccess parentAccess,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler elementAssembler) {
		super( navigablePath, setDescriptor, parentAccess, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.elementAssembler = elementAssembler;
	}

	@Override
	public PersistentSet getCollectionInstance() {
		return (PersistentSet) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List loadingState,
			RowProcessingState rowProcessingState) {
		//noinspection unchecked
		loadingState.add( elementAssembler.assemble( rowProcessingState ) );
	}

	@Override
	public String toString() {
		return "SetInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
