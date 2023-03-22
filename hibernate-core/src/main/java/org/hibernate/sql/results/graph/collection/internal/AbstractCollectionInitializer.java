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
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer implements CollectionInitializer {
	private final NavigablePath collectionPath;
	protected final PluralAttributeMapping collectionAttributeMapping;

	protected final FetchParentAccess parentAccess;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	protected final DomainResultAssembler<?> collectionKeyResultAssembler;

	protected PersistentCollection<?> collectionInstance;
	protected CollectionKey collectionKey;

	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			DomainResultAssembler<?> collectionKeyResultAssembler) {
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.parentAccess = parentAccess;
		this.collectionKeyResultAssembler = collectionKeyResultAssembler;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor( parentAccess, collectionAttributeMapping ) ) {
			return;
		}

		// A null collection key result assembler means that we can use the parent key
		final Object collectionKeyValue;
		if ( collectionKeyResultAssembler == null ) {
			collectionKeyValue = parentAccess.getParentKey();
		}
		else {
			collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
		}

		if ( collectionKeyValue != null ) {
			this.collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					collectionKeyValue
			);

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.COLL_LOAD_LOGGER.debugf(
						"(%s) Current row collection key : %s",
						this.getClass().getSimpleName(),
						LoggingHelper.toLoggableString( getNavigablePath(), this.collectionKey.getKey() )
				);
			}
		}
	}

	protected void resolveInstance(RowProcessingState rowProcessingState, boolean isEager) {
		if ( collectionKey != null ) {
			if ( parentAccess != null ) {
				final EntityInitializer parentEntityInitializer = parentAccess.findFirstEntityInitializer();
				if ( parentEntityInitializer != null && parentEntityInitializer.isEntityInitialized() ) {
					return;
				}
			}
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			final FetchParentAccess fetchParentAccess = parentAccess.findFirstEntityDescriptorAccess();

			final LoadingCollectionEntry loadingEntry = persistenceContext.getLoadContexts()
					.findLoadingCollectionEntry( collectionKey );

			if ( loadingEntry != null ) {
				collectionInstance = loadingEntry.getCollectionInstance();
				if ( collectionInstance.getOwner() == null ) {
					fetchParentAccess.registerResolutionListener(
							owner -> collectionInstance.setOwner( owner )
					);
				}
				return;
			}

			final PersistentCollection<?> existing = persistenceContext.getCollection( collectionKey );

			if ( existing != null ) {
				collectionInstance = existing;
				if ( collectionInstance.getOwner() == null ) {
					fetchParentAccess.registerResolutionListener(
							owner -> collectionInstance.setOwner( owner )
					);
				}
				return;
			}

			final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();
			final Object key = collectionKey.getKey();

			collectionInstance = collectionSemantics.instantiateWrapper(
					key,
					collectionDescriptor,
					session
			);

			fetchParentAccess.registerResolutionListener(
					owner -> collectionInstance.setOwner( owner )
			);

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					collectionInstance,
					key
			);

			if ( isEager ) {
				persistenceContext.addNonLazyCollection( collectionInstance );
			}

			if ( collectionSemantics.getCollectionClassification() == CollectionClassification.ARRAY ) {
				session.getPersistenceContext().addCollectionHolder( collectionInstance );
			}
		}
	}

	@Override
	public PersistentCollection<?> getCollectionInstance() {
		return collectionInstance;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return collectionPath;
	}

	public PluralAttributeMapping getCollectionAttributeMapping() {
		return collectionAttributeMapping;
	}

	@Override
	public PluralAttributeMapping getInitializedPart() {
		return getCollectionAttributeMapping();
	}

	protected FetchParentAccess getParentAccess() {
		return parentAccess;
	}

	@Override
	public CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
		resolveKey( rowProcessingState );
		return collectionKey;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		collectionKey = null;
	}
}
