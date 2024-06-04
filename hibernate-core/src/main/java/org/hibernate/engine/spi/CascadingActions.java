/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;

/**
 * @author Steve Ebersole
 */
public class CascadingActions {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			CascadingAction.class.getName()
	);

	/**
	 * Disallow instantiation
	 */
	private CascadingActions() {
	}

	/**
	 * @see org.hibernate.Session#delete(Object)
	 */
	public static final CascadingAction<DeleteContext> DELETE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				DeleteContext context,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to delete: {0}", entityName );
			session.delete( entityName, child, isCascadeDeleteEnabled, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// delete does cascade to uninitialized collections
			return getAllElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should be deleted during delete
			return true;
		}

		@Override
		public String toString() {
			return "ACTION_DELETE";
		}
	};

	/**
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 */
	public static final CascadingAction<LockOptions> LOCK = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				LockOptions lockOptions,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to lock: {0}", entityName );
			session.lock( entityName, child, lockOptions );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// lock doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			//TODO: should orphans really be deleted during lock???
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_LOCK";
		}
	};

	/**
	 * @see org.hibernate.Session#refresh(Object)
	 */
	public static final CascadingAction<RefreshContext> REFRESH = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				RefreshContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to refresh: {0}", entityName );
			session.refresh( entityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// refresh doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_REFRESH";
		}
	};

	/**
	 * @see org.hibernate.Session#evict(Object)
	 */
	public static final CascadingAction<Void> EVICT = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				Void nothing,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to evict: {0}", entityName );
			session.evict( child );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// evicts don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_EVICT";
		}
	};

	/**
	 * @see org.hibernate.Session#saveOrUpdate(Object)
	 */
	public static final CascadingAction<PersistContext> SAVE_UPDATE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				PersistContext nothing,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to save or update: {0}", entityName );
			session.saveOrUpdate( entityName, child );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should be deleted during save/update
			return true;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_SAVE_UPDATE";
		}
	};

	/**
	 * @see org.hibernate.Session#merge(Object)
	 */
	public static final CascadingAction<MergeContext> MERGE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				MergeContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to merge: {0}", entityName );
			session.merge( entityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// merges don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should not be deleted during merge??
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_MERGE";
		}
	};

	/**
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction<PersistContext> PERSIST = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				PersistContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist: {0}", entityName );
			session.persist( entityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public String toString() {
			return "ACTION_PERSIST";
		}
	};

	/**
	 * Execute persist during flush time
	 *
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction<PersistContext> PERSIST_ON_FLUSH = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				PersistContext anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist on flush: {0}", entityName );
			session.persistOnFlush( entityName, child, anything );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return true;
		}

		@Override
		public boolean requiresNoCascadeChecking() {
			return true;
		}

		@Override
		public void noCascade(
				EventSource session,
				Object parent,
				EntityPersister persister,
				Type propertyType,
				int propertyIndex) {
			if ( propertyType.isEntityType() ) {
				final Object child = persister.getValue( parent, propertyIndex );
				if ( child != null
						&& !isInManagedState( child, session )
						&& !isHibernateProxy( child ) ) { //a proxy cannot be transient and it breaks ForeignKeys.isTransient
					final String childEntityName =
							((EntityType) propertyType).getAssociatedEntityName( session.getFactory() );
					if ( ForeignKeys.isTransient( childEntityName, child, null, session ) ) {
						String parentEntityName = persister.getEntityName();
						String propertyName = persister.getPropertyNames()[propertyIndex];
						throw new TransientPropertyValueException(
							"object references an unsaved transient instance - save the transient instance before flushing",
							childEntityName,
							parentEntityName,
							propertyName
						);
					}
				}
			}
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		private boolean isInManagedState(Object child, EventSource session) {
			EntityEntry entry = session.getPersistenceContextInternal().getEntry( child );
			if ( entry == null ) {
				return false;
			}
			else {
				switch ( entry.getStatus() ) {
					case MANAGED:
					case READ_ONLY:
					case SAVING:
						return true;
					default:
						return false;
				}
			}
		}

		@Override
		public String toString() {
			return "ACTION_PERSIST_ON_FLUSH";
		}
	};

	/**
	 * @see org.hibernate.Session#replicate
	 */
	public static final CascadingAction<ReplicationMode> REPLICATE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String entityName,
				ReplicationMode anything,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to replicate: {0}", entityName );
			session.replicate( entityName, child, anything );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// replicate does cascade to uninitialized collections
			return getLoadedElementsIterator( session, collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return false; //I suppose?
		}

		@Override
		public String toString() {
			return "ACTION_REPLICATE";
		}
	};

	public abstract static class BaseCascadingAction<T> implements CascadingAction<T> {
		@Override
		public boolean requiresNoCascadeChecking() {
			return false;
		}

		@Override
		public void noCascade(EventSource session, Object parent, EntityPersister persister, Type propertyType, int propertyIndex) {
		}

		@Override
		public boolean performOnLazyProperty() {
			return true;
		}
	}

	/**
	 * Given a collection, get an iterator of all its children, loading them
	 * from the database if necessary.
	 *
	 * @param session The session within which the cascade is occurring.
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 *
	 * @return The children iterator.
	 */
	public static Iterator<?> getAllElementsIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection) {
		return collectionType.getElementsIterator( collection );
	}

	/**
	 * Iterate just the elements of the collection that are already there. Don't load
	 * any new elements from the database.
	 */
	public static Iterator<?> getLoadedElementsIterator(
			SharedSessionContractImplementor session,
			CollectionType collectionType,
			Object collection) {
		if ( collectionIsInitialized( collection ) ) {
			// handles arrays and newly instantiated collections
			return collectionType.getElementsIterator( collection );
		}
		else {
			// does not handle arrays (thats ok, cos they can't be lazy)
			// or newly instantiated collections, so we can do the cast
			return ((PersistentCollection<?>) collection).queuedAdditionIterator();
		}
	}

	private static boolean collectionIsInitialized(Object collection) {
		return !(collection instanceof PersistentCollection)
			|| ((PersistentCollection<?>) collection).wasInitialized();
	}
}
