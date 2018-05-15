/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class SetInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler elementAssembler;

	public SetInitializer(
			PersistentCollectionDescriptor setDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler elementAssembler) {
		super( setDescriptor, parentAccess, navigablePath, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.elementAssembler = elementAssembler;
	}

	@Override
	public PersistentSet getCollectionInstance() {
		return (PersistentSet) super.getCollectionInstance();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void readCollectionRow(RowProcessingState rowProcessingState) {
		getCollectionInstance().load( elementAssembler.assemble( rowProcessingState ) );
	}

	@Override
	public String toString() {
		return "SetInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
