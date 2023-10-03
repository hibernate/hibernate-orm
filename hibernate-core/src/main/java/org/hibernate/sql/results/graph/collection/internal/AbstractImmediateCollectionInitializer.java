/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.sql.results.graph.collection.CollectionLoadingLogger.COLL_LOAD_LOGGER;

/**
 * Base support for CollectionInitializer implementations that represent
 * an immediate initialization of some sort (join, select, batch, sub-select)
 * for a persistent collection.
 *
 * @author Steve Ebersole
 * @implNote Mainly an intention contract wrt the immediacy of the fetch.
 */
public abstract class AbstractImmediateCollectionInitializer extends AbstractCollectionInitializer {

	// per-row state
	private LoadingCollectionEntryImpl responsibility;

	/**
	 * refers to the rows entry in the collection.  null indicates that the collection is empty
	 */
	private final DomainResultAssembler<?> collectionValueKeyResultAssembler;


	// per-row state
	private Object collectionValueKey;

	private boolean isInitialized;

	public AbstractImmediateCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyResultAssembler,
			DomainResultAssembler<?> collectionValueKeyResultAssembler) {
		super( collectionPath, collectionAttributeMapping, parentAccess, collectionKeyResultAssembler );
		this.collectionValueKeyResultAssembler = collectionValueKeyResultAssembler;
	}

	protected abstract String getSimpleConcreteImplName();

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( collectionInstance != null || collectionKey == null) {
			return;
		}

		if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isTraceEnabled() ) {
			COLL_LOAD_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance for collection : %s",
					getSimpleConcreteImplName(),
					LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
			);
		}

		// determine the PersistentCollection instance to use and whether
		// we (this initializer) is responsible for loading its state

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// First, look for a LoadingCollectionEntry
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		final LoadingCollectionEntry existingLoadingEntry = persistenceContext
				.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );

		if ( existingLoadingEntry != null ) {
			collectionInstance = existingLoadingEntry.getCollectionInstance();

			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Found existing loading collection entry [%s]; using loading collection instance - %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			if ( existingLoadingEntry.getInitializer() == this ) {
				// we are responsible for loading the collection values
				responsibility = (LoadingCollectionEntryImpl) existingLoadingEntry;
			}
			else {
				// the entity is already being loaded elsewhere
				if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
					COLL_LOAD_LOGGER.debugf(
							"(%s) Collection [%s] being loaded by another initializer [%s] - skipping processing",
							getSimpleConcreteImplName(),
							LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
							existingLoadingEntry.getInitializer()
					);
				}

				// EARLY EXIT!!!
				return;
			}
		}
		else {
			final PersistentCollection<?> existing = persistenceContext.getCollection( collectionKey );
			if ( existing != null ) {
				collectionInstance = existing;

				// we found the corresponding collection instance on the Session.  If
				// it is already initialized we have nothing to do

				if ( collectionInstance.wasInitialized() ) {
					if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
						COLL_LOAD_LOGGER.debugf(
								"(%s) Found existing collection instance [%s] in Session; skipping processing - [%s]",
								getSimpleConcreteImplName(),
								LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
								toLoggableString( collectionInstance )
						);
					}

					// EARLY EXIT!!!
					return;
				}
				else {
					takeResponsibility( rowProcessingState, collectionKey );
				}
			}
			else {
				final PersistentCollection<?> existingUnowned = persistenceContext.useUnownedCollection( collectionKey );
				if ( existingUnowned != null ) {
					collectionInstance = existingUnowned;

					// we found the corresponding collection instance as unowned on the Session.  If
					// it is already initialized we have nothing to do

					if ( collectionInstance.wasInitialized() ) {
						if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
							COLL_LOAD_LOGGER.debugf(
									"(%s) Found existing unowned collection instance [%s] in Session; skipping processing - [%s]",
									getSimpleConcreteImplName(),
									LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
									toLoggableString( collectionInstance )
							);
						}

						// EARLY EXIT!!!
						return;
					}
					else {
						takeResponsibility( rowProcessingState, collectionKey );
					}
				}
			}
		}

		if ( collectionInstance == null && collectionKey != null ) {
			final CollectionPersister collectionDescriptor = getCollectionAttributeMapping().getCollectionDescriptor();
			final CollectionSemantics<?, ?> collectionSemantics = collectionDescriptor.getCollectionSemantics();

			collectionInstance = collectionSemantics.instantiateWrapper(
					collectionKey.getKey(),
					getInitializingCollectionDescriptor(),
					session
			);

			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Created new collection wrapper [%s] : %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			persistenceContext.addUninitializedCollection(
					collectionDescriptor,
					collectionInstance,
					collectionKey.getKey()
			);

			takeResponsibility( rowProcessingState, collectionKey );
		}

		if ( responsibility != null ) {
			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Responsible for loading collection [%s] : %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			final FetchParentAccess entityParentAccess = getParentAccess() != null ?
					getParentAccess().findFirstEntityDescriptorAccess() :
					null;
			if ( entityParentAccess != null ) {
				entityParentAccess.registerResolutionListener(
						owner -> collectionInstance.setOwner( owner )
				);
			}
		}
	}

	/**
	 * Specialized toString handling for PersistentCollection.  All `PersistentCollection#toString`
	 * implementations are crazy expensive as they trigger a load
	 */
	private String toLoggableString(PersistentCollection<?> collectionInstance) {
		return collectionInstance == null
				? LoggingHelper.NULL
				: collectionInstance.getClass().getName() + "@" + System.identityHashCode( collectionInstance );
	}

	protected void takeResponsibility(RowProcessingState rowProcessingState, CollectionKey collectionKey) {
		responsibility = new LoadingCollectionEntryImpl(
				getCollectionAttributeMapping().getCollectionDescriptor(),
				this,
				collectionKey.getKey(),
				collectionInstance
		);
		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
				collectionKey,
				responsibility
		);
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}
		super.resolveKey( rowProcessingState );
		if ( collectionValueKeyResultAssembler == null ) {
			collectionValueKey = collectionKey.getKey();
		}
		else {
			collectionValueKey = collectionValueKeyResultAssembler.assemble( rowProcessingState );
		}
	}


	/**
	 * The value of the collection side of the collection key (FK).  Identifies
	 * inclusion in the collection.  Can be null to indicate that the current row
	 * does not contain any collection values
	 */
	protected Object getCollectionValueKey() {
		return collectionValueKey;
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( responsibility == null || isInitialized ) {
			return;
		}

		// the LHS key value of the association
		final CollectionKey collectionKey = resolveCollectionKey( rowProcessingState );

		// the RHS key value of the association - determines if the row contains an element of the initializing collection
		final Object collectionValueKey = getCollectionValueKey();

		if ( collectionValueKey != null ) {
			// the row contains an element in the collection...
			if ( CollectionLoadingLogger.COLL_LOAD_LOGGER.isDebugEnabled() ) {
				COLL_LOAD_LOGGER.debugf(
						"(%s) Reading element from row for collection [%s] -> %s",
						getSimpleConcreteImplName(),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						toLoggableString( collectionInstance )
				);
			}

			responsibility.load(
					loadingState -> readCollectionRow( collectionKey, loadingState, rowProcessingState )
			);
		}
		isInitialized = true;
	}

	protected abstract void readCollectionRow(
			CollectionKey collectionKey,
			List<Object> loadingState,
			RowProcessingState rowProcessingState);

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );

		collectionValueKey = null;
		collectionInstance = null;
		responsibility = null;
		isInitialized = false;
	}

}
