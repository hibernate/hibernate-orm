/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.LoadingCollectionEntryImpl;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Base support for CollectionInitializer implementations that represent
 * an immediate initialization of some sort (join, select, batch, sub-select)
 * for a persistent collection.
 *
 * @implNote Mainly an intention contract wrt the immediacy of the fetch.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractImmediateCollectionInitializer extends AbstractCollectionInitializer {
	private final LockMode lockMode;

	// per-row state
	private PersistentCollection collectionInstance;
	private boolean responsible;
	private boolean collectionEmpty = true;

	public AbstractImmediateCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		super( collectionPath, collectionAttributeMapping, parentAccess, selected, keyContainerAssembler, keyCollectionAssembler );
		this.lockMode = lockMode;
	}

	@Override
	public PersistentCollection getCollectionInstance() {
		return collectionInstance;
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( collectionInstance != null ) {
			return;
		}

		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		final CollectionKey collectionKey = resolveCollectionKey( rowProcessingState );

		if ( CollectionLoadingLogger.TRACE_ENABLED ) {
			CollectionLoadingLogger.INSTANCE.tracef(
					"(%s) Beginning Initializer#resolveInstance for collection : %s",
					StringHelper.collapse( this.getClass().getName() ),
					LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
			);
		}

		// determine the PersistentCollection instance to use and whether
		// we (this initializer) is responsible for loading its state

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// First, look for a LoadingCollectionEntry

		final LoadingCollectionEntry existingLoadingEntry = persistenceContext
				.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );
//		final LoadingCollectionEntry existingLoadingEntry = rowProcessingState.getJdbcValuesSourceProcessingState()
//				.findLoadingCollectionLocally( getCollectionDescriptor(), collectionKey.getKey() );

		final PluralAttributeMapping collectionAttributeMapping = getCollectionAttributeMapping();
		final CollectionPersister collectionDescriptor = collectionAttributeMapping.getCollectionDescriptor();
		final CollectionSemantics collectionSemantics = collectionDescriptor.getCollectionSemantics();

		if ( existingLoadingEntry != null ) {
			collectionInstance = existingLoadingEntry.getCollectionInstance();

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Found existing loading collection entry [%s]; using loading collection instance - %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						LoggingHelper.toLoggableString( collectionInstance )
				);
			}

			if ( existingLoadingEntry.getInitializer() == this ) {
				// we are responsible for loading the collection values
				responsible = true;
			}
			else {
				// the entity is already being loaded elsewhere
				if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
					CollectionLoadingLogger.INSTANCE.debugf(
							"(%s) Collection [%s] being loaded by another initializer [%s] - skipping processing",
							StringHelper.collapse( this.getClass().getName() ),
							LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
							existingLoadingEntry.getInitializer()
					);
				}

				// EARLY EXIT!!!
				return;
			}
		}
		else {
			final PersistentCollection existing = persistenceContext.getCollection( collectionKey );
			if ( existing != null ) {
				collectionInstance = existing;

				// we found the corresponding collection instance on the Session.  If
				// it is already initialized we have nothing to do

				if ( collectionInstance.wasInitialized() ) {
					if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
						CollectionLoadingLogger.INSTANCE.debugf(
								"(%s) Found existing collection instance [%s] in Session; skipping processing - [%s]",
								StringHelper.collapse( this.getClass().getName() ),
								LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
								LoggingHelper.toLoggableString( collectionInstance )
						);
					}

					// EARLY EXIT!!!
					return;
				}
				else {
					assert isSelected();
					takeResponsibility( rowProcessingState, collectionKey );
				}
			}
			else {
				final PersistentCollection existingUnowned = persistenceContext.useUnownedCollection( collectionKey );
				if ( existingUnowned != null ) {
					collectionInstance = existingUnowned;

					// we found the corresponding collection instance as unowned on the Session.  If
					// it is already initialized we have nothing to do

					if ( collectionInstance.wasInitialized() ) {
						if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
							CollectionLoadingLogger.INSTANCE.debugf(
									"(%s) Found existing unowned collection instance [%s] in Session; skipping processing - [%s]",
									StringHelper.collapse( this.getClass().getName() ),
									LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
									LoggingHelper.toLoggableString( collectionInstance )
							);
						}

						// EARLY EXIT!!!
						return;
					}
					else {
						assert isSelected();
						takeResponsibility( rowProcessingState, collectionKey );
					}
				}
			}

			if ( ! isSelected() ) {
				collectionInstance = collectionSemantics.instantiateWrapper(
						collectionKey.getKey(),
						collectionDescriptor,
						session
				);
				persistenceContext.addNonLazyCollection( collectionInstance );

				// EARLY EXIT!!!
				return;
			}
		}

		if ( collectionInstance == null && collectionKey != null ) {
			collectionInstance = collectionSemantics.instantiateWrapper(
					collectionKey.getKey(),
					getInitializingCollectionDescriptor(),
					session
			);

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Created new collection wrapper [%s] : %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						LoggingHelper.toLoggableString( collectionInstance )
				);
			}

			persistenceContext.addUninitializedCollection( collectionDescriptor, collectionInstance, collectionKey.getKey() );

			takeResponsibility( rowProcessingState, collectionKey );
		}

		if ( responsible ) {
			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Responsible for loading collection [%s] : %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						LoggingHelper.toLoggableString( collectionInstance )
				);
			}

			if ( getParentAccess() != null ) {
				getParentAccess().registerResolutionListener(
						owner -> collectionInstance.setOwner( owner )
				);
			}

//			if ( getCollectionDescriptor().getSemantics().getCollectionClassification() == CollectionClassification.ARRAY ) {
//				persistenceContext.addCollectionHolder( collectionInstance );
//			}
		}
	}

	protected void takeResponsibility(RowProcessingState rowProcessingState, CollectionKey collectionKey) {
		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
				collectionKey,
				new LoadingCollectionEntryImpl(
						getCollectionAttributeMapping().getCollectionDescriptor(),
						this,
						collectionKey.getKey(),
						collectionInstance
				)
		);
		responsible = true;
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( !responsible ) {
			return;
		}

		final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContext();

		// the LHS key value of the association
		final CollectionKey collectionKey = resolveCollectionKey( rowProcessingState );
		// the RHS key value of the association
		final Object keyCollectionValue = getKeyCollectionValue();

		if ( keyCollectionValue != null ) {
			// the row contains an element in the collection...
			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Reading element from row for collection [%s] -> %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						LoggingHelper.toLoggableString( collectionInstance )
				);
			}

			readCollectionRow( rowProcessingState );
			collectionEmpty = false;
		}
	}

	protected abstract void readCollectionRow(RowProcessingState rowProcessingState);

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );

		collectionInstance = null;
		responsible = false;
		collectionEmpty = true;
	}

	@Override
	public void endLoading(ExecutionContext context) {
		if ( getParentAccess() == null && collectionEmpty ) {
			// collection is empty; handle special logic here.
			final CollectionKey collectionKey = context.getCollectionKey();
			if ( collectionKey != null ) {
				// We expected to load a collection with this collection key but we found the collection
				// contained no results, therefore we need to do the collection init phase here because
				// the LoadingCollectionEntry won't finalize this for us without at least one row.
				final PersistenceContext persistenceContext = context.getSession().getPersistenceContext();
				final PersistentCollection collection = persistenceContext.getCollection( collectionKey );
				collection.beforeInitialize( getCollectionAttributeMapping().getCollectionDescriptor(), 0 );
				collection.beginRead();
				collection.endRead();

				final CollectionEntry entry = persistenceContext.getCollectionEntry( collection );
				if ( entry != null ) {
					entry.postInitialize( collection );
				}
			}
		}
	}
}
