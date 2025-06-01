/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.emptyIterator;
import static org.hibernate.engine.internal.ForeignKeys.isTransient;
import static org.hibernate.engine.internal.ManagedTypeHelper.isHibernateProxy;
import static org.hibernate.internal.util.StringHelper.join;

/**
 * @author Steve Ebersole
 */
public class CascadingActions {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			CascadingAction.class.getName()
	);

	/**
	 * Disallow instantiation
	 */
	private CascadingActions() {
	}

	/**
	 * @see org.hibernate.Session#remove(Object)
	 */
	public static final CascadingAction<DeleteContext> REMOVE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				DeleteContext context,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to delete: {0}", childEntityName );
			session.delete( childEntityName, child, isCascadeDeleteEnabled, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// delete does cascade to uninitialized collections
			return getAllElementsIterator( collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			// orphans should be deleted during delete
			return true;
		}

		@Override
		public boolean anythingToCascade(EntityPersister persister) {
			return persister.hasCascadeDelete();
		}

		@Override
		public ForeignKeyDirection directionAffectedByCascadeDelete() {
			return ForeignKeyDirection.FROM_PARENT;
		}

		@Override
		public String toString() {
			return "ACTION_DELETE";
		}
	};

	/**
	 * Used in legacy {@code Session#delete} method, which has been removed
	 *
	 * @deprecated Use {@link #REMOVE}
	 */
	@Deprecated(since = "6.6", forRemoval = true)
	public static final CascadingAction<DeleteContext> DELETE = REMOVE;

	/**
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 *
	 * @deprecated because {@link org.hibernate.annotations.CascadeType#LOCK}
	 *             is deprecated
	 */
	@Deprecated(since="7", forRemoval = true)
	public static final CascadingAction<LockOptions> LOCK = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				LockOptions lockOptions,
				boolean isCascadeDeleteEnabled) {
			LOG.tracev( "Cascading to lock: {0}", childEntityName );
			session.lock( childEntityName, child, lockOptions );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// lock doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				RefreshContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to refresh: {0}", childEntityName );
			session.refresh( childEntityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// refresh doesn't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				Void nothing,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to evict: {0}", childEntityName );
			session.evict( child );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// evicts don't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
	 * @see org.hibernate.Session#merge(Object)
	 */
	public static final CascadingAction<MergeContext> MERGE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				MergeContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to merge: {0}", childEntityName );
			session.merge( childEntityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// merges don't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				PersistContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist: {0}", childEntityName );
			session.persist( childEntityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
		public boolean anythingToCascade(EntityPersister persister) {
			return persister.hasCascadePersist();
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
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				PersistContext context,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to persist on flush: {0}", childEntityName );
			session.persistOnFlush( childEntityName, child, context );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// persists don't cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
		}

		@Override
		public boolean deleteOrphans() {
			return true;
		}

		@Override
		public boolean performOnLazyProperty() {
			return false;
		}

		@Override
		public boolean anythingToCascade(EntityPersister persister) {
			return persister.hasCascadePersist();
		}

		@Override
		public String toString() {
			return "ACTION_PERSIST_ON_FLUSH";
		}
	};

	@Internal
	// this is not a real type of cascade, but it's a check that
	// is at least a bit sensitive to the cascade style, and it's
	// convenient to be able to use the graph-walking logic that
	// is already implemented in the Cascade class (though perhaps
	// we could reuse the logic in Nullability instead)
	public static final CascadingAction<Void> CHECK_ON_FLUSH = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				Void nothing,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			if ( child != null && isChildTransient( session, child, childEntityName, isCascadeDeleteEnabled ) ) {
				throw new TransientPropertyValueException(
						"Persistent instance of '" + parentEntityName
							+ "' references an unsaved transient instance of '" + childEntityName
							+ "' (persist the transient instance before flushing)",
						childEntityName,
						parentEntityName,
						attributePath == null
								? propertyName
								: join( ".", attributePath ) + '.' + propertyName
				);
			}
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			if ( collectionType.isInverse( session.getSessionFactory() ) ) {
				// For now, don't throw when an unowned collection
				// contains references to transient/deleted objects.
				// Strictly speaking, we should throw: but it just
				// feels a bit too heavy-handed, especially in the
				// case where the entity isn't transient but removed.
				return emptyIterator();
			}
			else {
				return getLoadedElementsIterator( collectionType, collection );
			}
		}

		@Override
		public boolean anythingToCascade(EntityPersister persister) {
			// Must override the implementation from the superclass
			// because transient checking happens even for entities
			// with cascade NONE on all associations
			// if the entity has no associations, we can just ignore it
			return persister.hasToOnes()
				|| persister.hasOwnedCollections()
				// when hibernate.unowned_association_transient_check
				// is enabled, we have to check unowned associations
				|| persister.hasCollections()
					&& persister.getFactory().getSessionFactoryOptions()
							.isUnownedAssociationTransientCheck();
		}

		@Override
		public boolean appliesTo(Type type, CascadeStyle style) {
			// Very important to override the implementation from
			// the superclass, because CHECK_ON_FLUSH is the only
			// style that executes for fields with cascade NONE
			return super.appliesTo( type, style )
				// we only care about associations here,
				// but they can hide inside embeddables
				&& ( type.isComponentType() || type.isAssociationType() );
		}

		@Override
		public boolean cascadeNow(
				CascadePoint cascadePoint,
				AssociationType associationType,
				SessionFactoryImplementor factory) {
			return super.cascadeNow( cascadePoint, associationType, factory )
				&& ( factory.getSessionFactoryOptions().isUnownedAssociationTransientCheck()
						|| !isUnownedAssociation( associationType, factory ) );
		}

		private static boolean isUnownedAssociation(AssociationType associationType, SessionFactoryImplementor factory) {
			if ( associationType instanceof ManyToOneType manyToOne ) {
				// logical one-to-one + non-null unique key property name indicates unowned
				return manyToOne.isLogicalOneToOne() && manyToOne.getRHSUniqueKeyPropertyName() != null;
			}
			else if ( associationType instanceof OneToOneType oneToOne ) {
				// constrained false + non-null unique key property name indicates unowned
				return oneToOne.isNullable() && oneToOne.getRHSUniqueKeyPropertyName() != null;
			}
			else if ( associationType instanceof CollectionType collectionType ) {
				// for collections, we can ask the persister if we're on the inverse side
				return collectionType.isInverse( factory );
			}
			return false;
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
		public ForeignKeyDirection directionAffectedByCascadeDelete() {
			return ForeignKeyDirection.TO_PARENT;
		}

		@Override
		public String toString() {
			return "ACTION_CHECK_ON_FLUSH";
		}
	};

	private static boolean isChildTransient(EventSource session, Object child, String entityName, boolean isCascadeDeleteEnabled) {
		if ( isHibernateProxy( child ) ) {
			// a proxy is always non-transient
			// and ForeignKeys.isTransient()
			// is not written to expect a proxy
			// TODO: but the proxied entity might have been deleted!
			return false;
		}
		else {
			final EntityEntry entry = session.getPersistenceContextInternal().getEntry( child );
			if ( entry != null ) {
				// if it's associated with the session
				// we are good, even if it's not yet
				// inserted, since ordering problems
				// are detected and handled elsewhere
				return entry.getStatus().isDeletedOrGone()
					// if the foreign key is 'on delete cascade'
					// we don't have to throw because the database
					// will delete the parent for us
					&& !isCascadeDeleteEnabled;
			}
			else {
				// TODO: check if it is a merged entity which has not yet been flushed
				// Currently this throws if you directly reference a new transient
				// instance after a call to merge() that results in its managed copy
				// being scheduled for insertion, if the insert has not yet occurred.
				// This is not terrible: it's more correct to "swap" the reference to
				// point to the managed instance, but it's probably too heavy-handed.
				return isTransient( entityName, child, null, session );
			}
		}
	}

	/**
	 * @see org.hibernate.Session#replicate
	 */
	public static final CascadingAction<ReplicationMode> REPLICATE = new BaseCascadingAction<>() {
		@Override
		public void cascade(
				EventSource session,
				Object child,
				String childEntityName,
				String parentEntityName,
				String propertyName,
				List<String> attributePath,
				ReplicationMode mode,
				boolean isCascadeDeleteEnabled)
				throws HibernateException {
			LOG.tracev( "Cascading to replicate: {0}", childEntityName );
			session.replicate( childEntityName, child, mode );
		}

		@Override
		public Iterator<?> getCascadableChildrenIterator(
				EventSource session,
				CollectionType collectionType,
				Object collection) {
			// replicate does cascade to uninitialized collections
			return getLoadedElementsIterator( collectionType, collection );
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
		public boolean performOnLazyProperty() {
			return true;
		}

		@Override
		public boolean anythingToCascade(EntityPersister persister) {
			// if the entity has cascade NONE everywhere, we can ignore it
			// TODO: the persister could track which kinds of cascade it has
			return persister.hasCascades();
		}

		@Override
		public boolean appliesTo(Type type, CascadeStyle style) {
			return style.doCascade( this );
		}

		@Override
		public boolean cascadeNow(
				CascadePoint cascadePoint,
				AssociationType associationType,
				SessionFactoryImplementor factory) {
			return associationType.getForeignKeyDirection().cascadeNow( cascadePoint );
		}

		@Override @Nullable
		public ForeignKeyDirection directionAffectedByCascadeDelete() {
			return null;
		}
	}

	/**
	 * Given a collection, get an iterator of all its children, loading them
	 * from the database if necessary.
	 *
	 * @param collectionType The mapping type of the collection.
	 * @param collection The collection instance.
	 * @return The children iterator.
	 */
	public static Iterator<?> getAllElementsIterator(
			CollectionType collectionType,
			Object collection) {
		return collectionType.getElementsIterator( collection );
	}

	/**
	 * Iterate just the elements of the collection that are already there. Don't load
	 * any new elements from the database.
	 */
	public static Iterator<?> getLoadedElementsIterator(
			CollectionType collectionType,
			Object collection) {
		if ( collection instanceof PersistentCollection<?> persistentCollection
				&& !persistentCollection.wasInitialized() ) {
			// does not handle arrays (that's ok, cos they can't be lazy)
			// or newly instantiated collections, so we can do the cast
			return persistentCollection.queuedAdditionIterator();
		}
		else {
			// handles arrays and newly instantiated collections
			return collectionType.getElementsIterator( collection );
		}
	}

	@Deprecated(forRemoval = true, since = "7.0")
	public static Iterator<?> getAllElementsIterator(
			EventSource session,
			CollectionType collectionType,
			Object collection) {
		return getAllElementsIterator( collectionType, collection );
	}

	@Deprecated(forRemoval = true, since = "7.0")
	public static Iterator<?> getLoadedElementsIterator(
			SharedSessionContractImplementor session,
			CollectionType collectionType,
			Object collection) {
		return getLoadedElementsIterator( collectionType, collection );
	}

}
