/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentIdentifierBag;
import org.hibernate.metamodel.model.domain.internal.PersistentBagDescriptorImpl;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

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
			PersistentBagDescriptorImpl bagDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler elementAssembler,
			DomainResultAssembler collectionIdAssembler) {
		super( bagDescriptor, parentAccess, navigablePath, selected, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.elementAssembler = elementAssembler;
		this.collectionIdAssembler = collectionIdAssembler;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void readCollectionRow(RowProcessingState rowProcessingState) {
		if ( collectionIdAssembler != null ) {
			( ( PersistentIdentifierBag ) getCollectionInstance() ).load(
					(Integer) collectionIdAssembler.assemble( rowProcessingState ),
					elementAssembler.assemble( rowProcessingState )
			);
		}
		else {
			( (PersistentBag) getCollectionInstance() ).load( elementAssembler.assemble( rowProcessingState ) );
		}
	}

	@Override
	public String toString() {
		return "BagInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
