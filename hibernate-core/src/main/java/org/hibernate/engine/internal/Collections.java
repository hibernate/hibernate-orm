/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.pretty.MessageHelper;

import org.jboss.logging.Logger;

/**
 * Implements book-keeping for the collection persistence by reachability algorithm
 *
 * @author Gavin King
 */
public final class Collections {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Collections.class.getName()
	);

	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param coll The collection to be updated by un-reachability.
	 * @param session The session
	 */
	public static void processUnreachableCollection(PersistentCollection coll, SessionImplementor session) {
		if ( coll.getOwner() == null ) {
			processNeverReferencedCollection( coll, session );
		}
		else {
			processDereferencedCollection( coll, session );
		}
	}

	private static void processDereferencedCollection(PersistentCollection coll, SessionImplementor session) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final CollectionEntry entry = persistenceContext.getCollectionEntry( coll );
		final PersistentCollectionDescriptor loadedDescriptor = entry.getLoadedCollectionDescriptor();

		if ( loadedDescriptor != null && LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Collection dereferenced: %s",
					MessageHelper.collectionInfoString( loadedDescriptor,
							coll, entry.getLoadedKey(), session
					)
			);
		}

		// do a check
		final boolean hasOrphanDelete = loadedDescriptor != null && loadedDescriptor.hasOrphanDelete();
		if ( hasOrphanDelete ) {
			final EntityTypeDescriptor ownerEntityDescriptor = getOwnerEntityDescriptor(
					loadedDescriptor,
					session.getSessionFactory()
			);

			Object ownerId = ownerEntityDescriptor.getIdentifier( coll.getOwner(), session );
			if ( ownerId == null ) {
				// the owning entity may have been deleted and its identifier unset due to
				// identifier-rollback; in which case, try to look up its identifier from
				// the persistence context
				if ( session.getFactory().getSessionFactoryOptions().isIdentifierRollbackEnabled() ) {
					final EntityEntry ownerEntry = persistenceContext.getEntry( coll.getOwner() );
					if ( ownerEntry != null ) {
						ownerId = ownerEntry.getId();
					}
				}
				if ( ownerId == null ) {
					throw new AssertionFailure( "Unable to determine collection owner identifier for orphan-delete processing" );
				}
			}
			final EntityKey key = session.generateEntityKey( ownerId, ownerEntityDescriptor );
			final Object owner = persistenceContext.getEntity( key );
			if ( owner == null ) {
				throw new AssertionFailure(
						"collection owner not associated with session: " +
						loadedDescriptor.getNavigableRole()
				);
			}
			final EntityEntry e = persistenceContext.getEntry( owner );
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( e != null && e.getStatus() != Status.DELETED && e.getStatus() != Status.GONE ) {
				throw new HibernateException(
						"A collection with cascade=\"all-delete-orphan\" was no longer referenced by the owning entity instance: " +
						loadedDescriptor.getNavigableRole()
				);
			}
		}

		// do the work
		entry.setCurrentDescriptor( null );
		entry.setCurrentKey( null );
		prepareCollectionForUpdate( coll, entry );

	}

	private static void processNeverReferencedCollection(PersistentCollection coll, SessionImplementor session)
			throws HibernateException {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final CollectionEntry entry = persistenceContext.getCollectionEntry( coll );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Found collection with unloaded owner: %s",
					MessageHelper.collectionInfoString( 
							entry.getLoadedCollectionDescriptor(),
							coll,
							entry.getLoadedKey(),
							session
					)
			);
		}

		entry.setCurrentDescriptor( entry.getLoadedCollectionDescriptor() );
		entry.setCurrentKey( entry.getLoadedKey() );

		prepareCollectionForUpdate( coll, entry);

	}

    /**
     * Initialize the role of the collection.
     *
     * @param collection The collection to be updated by reachability.
     * @param collectionAttribute The PluralPersistentAttribute.
     * @param entity The owner of the collection.
	 * @param session The session from which this request originates
     */
	public static void processReachableCollection(
			PersistentCollection collection,
			PluralPersistentAttribute collectionAttribute,
			Object entity,
			SessionImplementor session) {
		collection.setOwner( entity );
		final CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( collection );

		if ( ce == null ) {
			// refer to comment in StatefulPersistenceContext.addCollection()
			throw new HibernateException(
					"Found two representations of same collection: " + collectionAttribute.getNavigableName() );
		}

		final SessionFactoryImplementor factory = session.getFactory();
		ce.setCurrentDescriptor( collectionAttribute.getCollectionDescriptor() );

		ce.setCurrentKey( collectionAttribute.getCollectionDescriptor().getKeyOfOwner( entity, session ) );

		final EntityTypeDescriptor ownerEntityDescriptor = getOwnerEntityDescriptor( collectionAttribute.getCollectionDescriptor(), factory );
		final boolean isBytecodeEnhanced = ownerEntityDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		if ( isBytecodeEnhanced && !collection.wasInitialized() ) {
			// skip it
			LOG.debugf(
					"Skipping uninitialized bytecode-lazy collection: %s",
					MessageHelper.collectionInfoString( collectionAttribute.getCollectionDescriptor(), collection, ce.getCurrentKey(), session )
			);
			ce.setReached( true );
			ce.setProcessed( true );
		}
		else {
			// The CollectionEntry.isReached() stuff is just to detect any silly users
			// who set up circular or shared references between/to collections.
			if ( ce.isReached() ) {
				// We've been here before
				throw new HibernateException(
						"Found shared references to a collection: " + collectionAttribute.getCollectionDescriptor().getNavigableRole().getFullPath()
				);
			}
			ce.setReached( true );

			if ( LOG.isDebugEnabled() ) {
				if ( collection.wasInitialized() ) {
					LOG.debugf(
							"Collection found: %s, was: %s (initialized)",
							MessageHelper.collectionInfoString(
									collectionAttribute.getCollectionDescriptor(),
									collection,
									ce.getCurrentKey(),
									session
							),
							MessageHelper.collectionInfoString(
									ce.getLoadedCollectionDescriptor(),
									collection,
									ce.getLoadedKey(),
									session
							)
					);
				}
				else {
					LOG.debugf(
							"Collection found: %s, was: %s (uninitialized)",
							MessageHelper.collectionInfoString(
									collectionAttribute.getCollectionDescriptor(),
									collection,
									ce.getCurrentKey(),
									session
							),
							MessageHelper.collectionInfoString(
									ce.getLoadedCollectionDescriptor(),
									collection,
									ce.getLoadedKey(),
									session
							)
					);
				}
			}

			prepareCollectionForUpdate( collection, ce );
		}
	}

	private static EntityTypeDescriptor getOwnerEntityDescriptor(
			PersistentCollectionDescriptor descriptor,
			SessionFactoryImplementor factory) {
		return factory.getMetamodel().findEntityDescriptor(  descriptor.getContainer().getNavigableName() );
	}

	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	@SuppressWarnings( {"JavaDoc"})
	private static void prepareCollectionForUpdate(
			PersistentCollection collection,
			CollectionEntry entry) {
		if ( entry.isProcessed() ) {
			throw new AssertionFailure( "collection was processed twice by flush()" );
		}
		entry.setProcessed( true );

		final PersistentCollectionDescriptor loadedDescriptor = entry.getLoadedCollectionDescriptor();
		final PersistentCollectionDescriptor currentDescriptor = entry.getCurrentDescriptor();
		if ( loadedDescriptor != null || currentDescriptor != null ) {
			// it is or was referenced _somewhere_

			// check if the key changed
			// excludes marking key changed when the loaded key is a DelayedPostInsertIdentifier.
			final boolean keyChanged = currentDescriptor != null
					&& entry != null
					&& !currentDescriptor.getKeyJavaTypeDescriptor().areEqual( entry.getLoadedKey(), entry.getCurrentKey() )
					&& !( entry.getLoadedKey() instanceof DelayedPostInsertIdentifier );

			// if either its role changed, or its key changed
			final boolean ownerChanged = loadedDescriptor != currentDescriptor || keyChanged;

			if ( ownerChanged ) {
				// do a check
				final boolean orphanDeleteAndRoleChanged =
						loadedDescriptor != null && currentDescriptor != null && loadedDescriptor.hasOrphanDelete();

				if (orphanDeleteAndRoleChanged) {
					throw new HibernateException(
							"Don't change the reference to a collection with delete-orphan enabled : "
									+ loadedDescriptor.getNavigableRole().getFullPath()
					);
				}

				// do the work
				if ( currentDescriptor != null ) {
					entry.setDorecreate( true );
				}

				if ( loadedDescriptor != null ) {
					// we will need to remove ye olde entries
					entry.setDoremove( true );
					if ( entry.isDorecreate() ) {
						LOG.trace( "Forcing collection initialization" );
						collection.forceInitialization();
					}
				}
			}
			else if ( collection.isDirty() ) {
				// the collection's elements have changed
				entry.setDoupdate( true );
			}
		}
	}

	/**
	 * Disallow instantiation
	 */
	private Collections() {
	}
}
