/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
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

	public AbstractImmediateCollectionInitializer(
			PersistentCollectionDescriptor collectionDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			LockMode lockMode,
			DomainResultAssembler keyTargetAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		super( collectionDescriptor, parentAccess, navigablePath, selected, keyTargetAssembler, keyCollectionAssembler );
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
				persistenceContext.addUninitializedCollection( getCollectionDescriptor(), collectionInstance, collectionKey.getKey() );

				// note : this call adds the collection to the PC, so we will find it
				// next time (`existing`) and not attempt to load values
				//
				// todo (6.0) : we could possibly leverage subselect /  batch loading if we delay this until after all JdbcValues have been processed
				collectionInstance = getCollectionDescriptor().getLoader().load(
						collectionKey.getKey(),
						new LockOptions( lockMode ),
						session
				);

				// EARLY EXIT!!!
				return;
			}
		}

		if ( collectionInstance == null ) {
			collectionInstance = getCollectionDescriptor().instantiateWrapper(
					session,
					collectionKey.getKey()
			);

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Created new collection wrapper [%s] : %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() ),
						LoggingHelper.toLoggableString( collectionInstance )
				);
			}

			persistenceContext.addUninitializedCollection( getCollectionDescriptor(), collectionInstance, collectionKey.getKey() );

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

			if ( getCollectionDescriptor().getSemantics().getCollectionClassification() == CollectionClassification.ARRAY ) {
				persistenceContext.addCollectionHolder( collectionInstance );
			}
		}
	}

	protected void takeResponsibility(RowProcessingState rowProcessingState, CollectionKey collectionKey) {
		rowProcessingState.getJdbcValuesSourceProcessingState().registerLoadingCollection(
				collectionKey,
				new LoadingCollectionEntry(
						getCollectionDescriptor(),
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
		}
	}

	protected abstract void readCollectionRow(RowProcessingState rowProcessingState);

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		super.finishUpRow( rowProcessingState );

		collectionInstance = null;
		responsible = false;
	}
}
