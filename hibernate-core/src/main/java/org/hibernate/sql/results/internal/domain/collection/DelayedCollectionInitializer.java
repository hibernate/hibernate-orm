/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer {

	// per-row state
	private PersistentCollection collectionInstance;

	public DelayedCollectionInitializer(
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			PersistentCollectionDescriptor fetchCollectionDescriptor,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		super( fetchCollectionDescriptor, parentAccess, navigablePath, false, keyContainerAssembler, keyCollectionAssembler );
	}

	@Override
	public PersistentCollection getCollectionInstance() {
		return collectionInstance;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		super.resolveKey( rowProcessingState );

		getParentAccess().registerResolutionListener(
				owner -> collectionInstance.setOwner( owner )
		);
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		final CollectionKey collectionKey = resolveCollectionKey( rowProcessingState );

		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		final PersistentCollectionDescriptor collectionDescriptor = getFetchedAttribute().getPersistentCollectionDescriptor();

		// todo (6.0) : look for LoadingCollectionEntry?

		final PersistentCollection existing = persistenceContext.getCollection( collectionKey );
		if ( existing != null ) {
			collectionInstance = existing;
		}
		else {
			collectionInstance = collectionDescriptor.instantiateWrapper(
					session,
					collectionKey.getKey()
			);

			getParentAccess().registerResolutionListener(
					owner -> collectionInstance.setOwner( owner )
			);

			persistenceContext.addUninitializedCollection( collectionDescriptor, collectionInstance, collectionKey.getKey() );
			final CollectionEntry collectionEntry = persistenceContext.getCollectionEntry( collectionInstance );
			collectionEntry.setCurrentKey( collectionKey.getKey() );

			if ( getCollectionDescriptor().getSemantics().getCollectionClassification() == CollectionClassification.ARRAY ) {
				session.getPersistenceContext().addCollectionHolder( collectionInstance );
			}
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		/// nothing to do
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		collectionInstance = null;

		super.finishUpRow( rowProcessingState );
	}

	@Override
	public String toString() {
		return "DelayedCollectionInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
