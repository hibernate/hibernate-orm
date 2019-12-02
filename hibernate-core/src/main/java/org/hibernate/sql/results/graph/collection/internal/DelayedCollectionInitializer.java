/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer {

	// per-row state
	private PersistentCollection collectionInstance;

	public DelayedCollectionInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		super( navigablePath, attributeMapping, parentAccess, false, keyContainerAssembler, keyCollectionAssembler );
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
		if(collectionKey != null) {

			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();

			final PluralAttributeMapping attributeMapping = getCollectionAttributeMapping();
			final CollectionPersister collectionDescriptor = attributeMapping.getCollectionDescriptor();

			// todo (6.0) : look for LoadingCollectionEntry?

			final PersistentCollection existing = persistenceContext.getCollection( collectionKey );
			if ( existing != null ) {
				collectionInstance = existing;
			}
			else {
				final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();

				collectionInstance = collectionSemantics.instantiateWrapper(
						collectionKey.getKey(),
						collectionDescriptor,
						session
				);

				getParentAccess().registerResolutionListener(
						owner -> collectionInstance.setOwner( owner )
				);

				persistenceContext.addUninitializedCollection(
						collectionDescriptor,
						collectionInstance,
						collectionKey.getKey()
				);

				if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
					session.getPersistenceContext().addCollectionHolder( collectionInstance );
				}
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
