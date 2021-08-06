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
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionInitializer extends AbstractCollectionInitializer {

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	private final DomainResultAssembler keyContainerAssembler;

	public DelayedCollectionInitializer(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedMapping,
			FetchParentAccess parentAccess,
			DomainResultAssembler keyContainerAssembler) {
		super( fetchedPath, fetchedMapping, parentAccess );
		this.keyContainerAssembler = keyContainerAssembler;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			return;
		}

		final Object parentKey = parentAccess.getParentKey();

		// We can only use the parent key if the key descriptor uses the primary key of the owner i.e. refersToPrimaryKey
		if ( parentKey != null && collectionAttributeMapping.getKeyDescriptor().getKeyPart().getNavigableRole().equals(
				collectionAttributeMapping.findContainingEntityMapping().getIdentifierMapping().getNavigableRole() ) ) {
			collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					parentKey
			);
			parentAccess.registerResolutionListener( owner -> {
				if ( collectionInstance != null ) {
					collectionInstance.setOwner( owner );
				}
			} );
			return;
		}

		final CollectionKey loadingKey = rowProcessingState.getCollectionKey();
		if ( loadingKey != null && loadingKey.getRole()
				.equals( getCollectionAttributeMapping().getNavigableRole().getNavigableName() ) ) {
			collectionKey = loadingKey;
			return;
		}

		final JdbcValuesSourceProcessingOptions processingOptions = rowProcessingState.getJdbcValuesSourceProcessingState()
				.getProcessingOptions();

		final Object keyContainerValue = keyContainerAssembler.assemble(
				rowProcessingState,
				processingOptions
		);
		if ( keyContainerValue != null ) {
			this.collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					keyContainerValue
			);

			// TODO: This fails e.g. EagerCollectionLazyKeyManyToOneTest because Order$Id#customer is null
			//   which is required for the hash code. Is this being null at this point a bug?
//			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
//				CollectionLoadingLogger.INSTANCE.debugf(
//						"(%s) Current row collection key : %s",
//						DelayedCollectionInitializer.class.getSimpleName(),
//						LoggingHelper.toLoggableString( getNavigablePath(), this.collectionKey.getKey() )
//				);
//			}
			parentAccess.registerResolutionListener( owner -> {
				if ( collectionInstance != null ) {
					collectionInstance.setOwner( owner );
				}
			} );
		}
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );

			if ( loadingEntry != null ) {
				collectionInstance = loadingEntry.getCollectionInstance();
				return;
			}

			final PersistentCollection existing = persistenceContext.getCollection( collectionKey );

			if ( existing != null ) {
				collectionInstance = existing;
				return;
			}

			final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();

			final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();

			final Object key = collectionKey.getKey();

			collectionInstance = collectionSemantics.instantiateWrapper(
					key,
					collectionDescriptor,
					session
			);

			parentAccess.registerResolutionListener(
					owner -> collectionInstance.setOwner( owner )
			);

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);

			if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
				session.getPersistenceContext().addCollectionHolder( collectionInstance );
			}
		}
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
