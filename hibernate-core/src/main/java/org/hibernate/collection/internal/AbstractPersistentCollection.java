/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.collection.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.naming.NamingException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.EmptyIterator;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.jboss.logging.Logger;

/**
 * Base class implementing {@link org.hibernate.collection.spi.PersistentCollection}
 *
 * @author Gavin King
 */
public abstract class AbstractPersistentCollection implements Serializable, PersistentCollection {
	private static final Logger log = Logger.getLogger( AbstractPersistentCollection.class );

	private transient SessionImplementor session;
	private boolean initialized;
	private transient List<DelayedOperation> operationQueue;
	private transient boolean directlyAccessible;
	private transient boolean initializing;
	private Object owner;
	private int cachedSize = -1;

	private String role;
	private Serializable key;
	// collections detect changes made via their public interface and mark
	// themselves as dirty as a performance optimization
	private boolean dirty;
	private Serializable storedSnapshot;

	private String sessionFactoryUuid;
	private boolean specjLazyLoad = false;

	public final String getRole() {
		return role;
	}

	public final Serializable getKey() {
		return key;
	}

	public final boolean isUnreferenced() {
		return role == null;
	}

	public final boolean isDirty() {
		return dirty;
	}

	public final void clearDirty() {
		dirty = false;
	}

	public final void dirty() {
		dirty = true;
	}

	public final Serializable getStoredSnapshot() {
		return storedSnapshot;
	}

	//Careful: these methods do not initialize the collection.

	/**
	 * Is the initialized collection empty?
	 */
	public abstract boolean empty();

	/**
	 * Called by any read-only method of the collection interface
	 */
	protected final void read() {
		initialize( false );
	}

	/**
	 * Called by the {@link Collection#size} method
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean readSize() {
		if ( !initialized ) {
			if ( cachedSize != -1 && !hasQueuedOperations() ) {
				return true;
			}
			else {
				boolean isExtraLazy = withTemporarySessionIfNeeded(
						new LazyInitializationWork<Boolean>() {
							@Override
							public Boolean doWork() {
								CollectionEntry entry = session.getPersistenceContext().getCollectionEntry( AbstractPersistentCollection.this );

								if ( entry != null ) {
									CollectionPersister persister = entry.getLoadedPersister();
									if ( persister.isExtraLazy() ) {
										if ( hasQueuedOperations() ) {
											session.flush();
										}
										cachedSize = persister.getSize( entry.getLoadedKey(), session );
										return true;
									}
									else {
										read();
									}
								}
								else{
									throwLazyInitializationExceptionIfNotConnected();
								}
								return false;
							}
						}
				);
				if ( isExtraLazy ) {
					return true;
				}
			}
		}
		return false;
	}

	public static interface LazyInitializationWork<T> {
		public T doWork();
	}

	private <T> T withTemporarySessionIfNeeded(LazyInitializationWork<T> lazyInitializationWork) {
		SessionImplementor originalSession = null;
		boolean isTempSession = false;
		boolean isJTA = false;

		if ( session == null ) {
			if ( specjLazyLoad ) {
				session = openTemporarySessionForLoading();
				isTempSession = true;
			}
			else {
				throw new LazyInitializationException( "could not initialize proxy - no Session" );
			}
		}
		else if ( !session.isOpen() ) {
			if ( specjLazyLoad ) {
				originalSession = session;
				session = openTemporarySessionForLoading();
				isTempSession = true;
			}
			else {
				throw new LazyInitializationException( "could not initialize proxy - the owning Session was closed" );
			}
		}
		else if ( !session.isConnected() ) {
			if ( specjLazyLoad ) {
				originalSession = session;
				session = openTemporarySessionForLoading();
				isTempSession = true;
			}
			else {
				throw new LazyInitializationException( "could not initialize proxy - the owning Session is disconnected" );
			}
		}

		if ( isTempSession ) {
			// TODO: On the next major release, add an
			// 'isJTA' or 'getTransactionFactory' method to Session.
			isJTA = session.getTransactionCoordinator()
					.getTransactionContext().getTransactionEnvironment()
					.getTransactionFactory()
					.compatibleWithJtaSynchronization();
			
			if ( !isJTA ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				( ( Session) session ).beginTransaction();
			}
			
			session.getPersistenceContext().addUninitializedDetachedCollection(
					session.getFactory().getCollectionPersister( getRole() ),
					this
			);
		}

		try {
			return lazyInitializationWork.doWork();
		}
		finally {
			if ( isTempSession ) {
				// make sure the just opened temp session gets closed!
				try {
					if ( !isJTA ) {
						( ( Session) session ).getTransaction().commit();
					}
					( (Session) session ).close();
				}
				catch (Exception e) {
					log.warn( "Unable to close temporary session used to load lazy collection associated to no session" );
				}
				session = originalSession;
			}
		}
	}

	private SessionImplementor openTemporarySessionForLoading() {
		if ( sessionFactoryUuid == null ) {
			throwLazyInitializationException( "SessionFactory UUID not known to create temporary Session for loading" );
		}

		SessionFactoryImplementor sf = (SessionFactoryImplementor)
				SessionFactoryRegistry.INSTANCE.getSessionFactory( sessionFactoryUuid );
		return (SessionImplementor) sf.openSession();
	}

	protected Boolean readIndexExistence(final Object index) {
		if ( !initialized ) {
			Boolean extraLazyExistenceCheck = withTemporarySessionIfNeeded(
					new LazyInitializationWork<Boolean>() {
						@Override
						public Boolean doWork() {
							CollectionEntry entry = session.getPersistenceContext().getCollectionEntry( AbstractPersistentCollection.this );
							CollectionPersister persister = entry.getLoadedPersister();
							if ( persister.isExtraLazy() ) {
								if ( hasQueuedOperations() ) {
									session.flush();
								}
								return persister.indexExists( entry.getLoadedKey(), index, session );
							}
							else {
								read();
							}
							return null;
						}
					}
			);
			if ( extraLazyExistenceCheck != null ) {
				return extraLazyExistenceCheck;
			}
		}
		return null;
	}

	protected Boolean readElementExistence(final Object element) {
		if ( !initialized ) {
			Boolean extraLazyExistenceCheck = withTemporarySessionIfNeeded(
					new LazyInitializationWork<Boolean>() {
						@Override
						public Boolean doWork() {
							CollectionEntry entry = session.getPersistenceContext().getCollectionEntry( AbstractPersistentCollection.this );
							CollectionPersister persister = entry.getLoadedPersister();
							if ( persister.isExtraLazy() ) {
								if ( hasQueuedOperations() ) {
									session.flush();
								}
								return persister.elementExists( entry.getLoadedKey(), element, session );
							}
							else {
								read();
							}
							return null;
						}
					}
			);
			if ( extraLazyExistenceCheck != null ) {
				return extraLazyExistenceCheck;
			}
		}
		return null;
	}

	protected static final Object UNKNOWN = new MarkerObject( "UNKNOWN" );

	protected Object readElementByIndex(final Object index) {
		if ( !initialized ) {
			class ExtraLazyElementByIndexReader implements LazyInitializationWork {
				private boolean isExtraLazy;
				private Object element;

				@Override
				public Object doWork() {
					CollectionEntry entry = session.getPersistenceContext().getCollectionEntry( AbstractPersistentCollection.this );
					CollectionPersister persister = entry.getLoadedPersister();
					isExtraLazy = persister.isExtraLazy();
					if ( isExtraLazy ) {
						if ( hasQueuedOperations() ) {
							session.flush();
						}
						element = persister.getElementByIndex( entry.getLoadedKey(), index, session, owner );
					}
					else {
						read();
					}
					return null;
				}
			}

			ExtraLazyElementByIndexReader reader = new ExtraLazyElementByIndexReader();
			//noinspection unchecked
			withTemporarySessionIfNeeded( reader );
			if ( reader.isExtraLazy ) {
				return reader.element;
			}
		}
		return UNKNOWN;

	}

	protected int getCachedSize() {
		return cachedSize;
	}

	private boolean isConnectedToSession() {
		return session != null &&
				session.isOpen() &&
				session.getPersistenceContext().containsCollection( this );
	}

	/**
	 * Called by any writer method of the collection interface
	 */
	protected final void write() {
		initialize( true );
		dirty();
	}

	/**
	 * Is this collection in a state that would allow us to
	 * "queue" operations?
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isOperationQueueEnabled() {
		return !initialized &&
				isConnectedToSession() &&
				isInverseCollection();
	}

	/**
	 * Is this collection in a state that would allow us to
	 * "queue" puts? This is a special case, because of orphan
	 * delete.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isPutQueueEnabled() {
		return !initialized &&
				isConnectedToSession() &&
				isInverseOneToManyOrNoOrphanDelete();
	}

	/**
	 * Is this collection in a state that would allow us to
	 * "queue" clear? This is a special case, because of orphan
	 * delete.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isClearQueueEnabled() {
		return !initialized &&
				isConnectedToSession() &&
				isInverseCollectionNoOrphanDelete();
	}

	/**
	 * Is this the "inverse" end of a bidirectional association?
	 */
	@SuppressWarnings({"JavaDoc"})
	private boolean isInverseCollection() {
		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( this );
		return ce != null && ce.getLoadedPersister().isInverse();
	}

	/**
	 * Is this the "inverse" end of a bidirectional association with
	 * no orphan delete enabled?
	 */
	@SuppressWarnings({"JavaDoc"})
	private boolean isInverseCollectionNoOrphanDelete() {
		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( this );
		return ce != null &&
				ce.getLoadedPersister().isInverse() &&
				!ce.getLoadedPersister().hasOrphanDelete();
	}

	/**
	 * Is this the "inverse" end of a bidirectional one-to-many, or
	 * of a collection with no orphan delete?
	 */
	@SuppressWarnings({"JavaDoc"})
	private boolean isInverseOneToManyOrNoOrphanDelete() {
		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( this );
		return ce != null && ce.getLoadedPersister().isInverse() && (
				ce.getLoadedPersister().isOneToMany() ||
						!ce.getLoadedPersister().hasOrphanDelete()
		);
	}

	/**
	 * Queue an addition
	 */
	@SuppressWarnings({"JavaDoc"})
	protected final void queueOperation(DelayedOperation operation) {
		if ( operationQueue == null ) {
			operationQueue = new ArrayList<DelayedOperation>( 10 );
		}
		operationQueue.add( operation );
		dirty = true; //needed so that we remove this collection from the second-level cache
	}

	/**
	 * After reading all existing elements from the database,
	 * add the queued elements to the underlying collection.
	 */
	protected final void performQueuedOperations() {
		for ( DelayedOperation operation : operationQueue ) {
			operation.operate();
		}
	}

	/**
	 * After flushing, re-init snapshot state.
	 */
	public void setSnapshot(Serializable key, String role, Serializable snapshot) {
		this.key = key;
		this.role = role;
		this.storedSnapshot = snapshot;
	}

	/**
	 * After flushing, clear any "queued" additions, since the
	 * database state is now synchronized with the memory state.
	 */
	public void postAction() {
		operationQueue = null;
		cachedSize = -1;
		clearDirty();
	}

	/**
	 * Not called by Hibernate, but used by non-JDK serialization,
	 * eg. SOAP libraries.
	 */
	public AbstractPersistentCollection() {
	}

	protected AbstractPersistentCollection(SessionImplementor session) {
		this.session = session;
	}

	/**
	 * return the user-visible collection (or array) instance
	 */
	public Object getValue() {
		return this;
	}

	/**
	 * Called just before reading any rows from the JDBC result set
	 */
	public void beginRead() {
		// override on some subclasses
		initializing = true;
	}

	/**
	 * Called after reading all rows from the JDBC result set
	 */
	public boolean endRead() {
		//override on some subclasses
		return afterInitialize();
	}

	public boolean afterInitialize() {
		setInitialized();
		//do this bit after setting initialized to true or it will recurse
		if ( operationQueue != null ) {
			performQueuedOperations();
			operationQueue = null;
			cachedSize = -1;
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Initialize the collection, if possible, wrapping any exceptions
	 * in a runtime exception
	 *
	 * @param writing currently obsolete
	 *
	 * @throws LazyInitializationException if we cannot initialize
	 */
	protected final void initialize(final boolean writing) {
		if ( initialized ) {
			return;
		}

		withTemporarySessionIfNeeded(
				new LazyInitializationWork<Object>() {
					@Override
					public Object doWork() {
						session.initializeCollection( AbstractPersistentCollection.this, writing );
						return null;
					}
				}
		);
	}

	private void throwLazyInitializationExceptionIfNotConnected() {
		if ( !isConnectedToSession() ) {
			throwLazyInitializationException( "no session or session was closed" );
		}
		if ( !session.isConnected() ) {
			throwLazyInitializationException( "session is disconnected" );
		}
	}

	private void throwLazyInitializationException(String message) {
		throw new LazyInitializationException(
				"failed to lazily initialize a collection" +
						(role == null ? "" : " of role: " + role) +
						", " + message
		);
	}

	protected final void setInitialized() {
		this.initializing = false;
		this.initialized = true;
	}

	protected final void setDirectlyAccessible(boolean directlyAccessible) {
		this.directlyAccessible = directlyAccessible;
	}

	/**
	 * Could the application possibly have a direct reference to
	 * the underlying collection implementation?
	 */
	public boolean isDirectlyAccessible() {
		return directlyAccessible;
	}

	/**
	 * Disassociate this collection from the given session.
	 *
	 * @return true if this was currently associated with the given session
	 */
	public final boolean unsetSession(SessionImplementor currentSession) {
		prepareForPossibleSpecialSpecjInitialization();
		if ( currentSession == this.session ) {
			this.session = null;
			return true;
		}
		else {
			return false;
		}
	}

	protected void prepareForPossibleSpecialSpecjInitialization() {
		if ( session != null ) {
			specjLazyLoad = session.getFactory().getSettings().isInitializeLazyStateOutsideTransactionsEnabled();

			if ( specjLazyLoad && sessionFactoryUuid == null ) {
				try {
					sessionFactoryUuid = (String) session.getFactory().getReference().get( "uuid" ).getContent();
				}
				catch (NamingException e) {
					//not much we can do if this fails...
				}
			}
		}
	}


	/**
	 * Associate the collection with the given session.
	 *
	 * @return false if the collection was already associated with the session
	 *
	 * @throws HibernateException if the collection was already associated
	 * with another open session
	 */
	public final boolean setCurrentSession(SessionImplementor session) throws HibernateException {
		if ( session == this.session ) {
			return false;
		}
		else {
			if ( isConnectedToSession() ) {
				CollectionEntry ce = session.getPersistenceContext().getCollectionEntry( this );
				if ( ce == null ) {
					throw new HibernateException(
							"Illegal attempt to associate a collection with two open sessions"
					);
				}
				else {
					throw new HibernateException(
							"Illegal attempt to associate a collection with two open sessions: " +
									MessageHelper.collectionInfoString(
											ce.getLoadedPersister(), this,
											ce.getLoadedKey(), session
									)
					);
				}
			}
			else {
				this.session = session;
				return true;
			}
		}
	}

	/**
	 * Do we need to completely recreate this collection when it changes?
	 */
	public boolean needsRecreate(CollectionPersister persister) {
		return false;
	}

	/**
	 * To be called internally by the session, forcing
	 * immediate initialization.
	 */
	public final void forceInitialization() throws HibernateException {
		if ( !initialized ) {
			if ( initializing ) {
				throw new AssertionFailure( "force initialize loading collection" );
			}
			if ( session == null ) {
				throw new HibernateException( "collection is not associated with any session" );
			}
			if ( !session.isConnected() ) {
				throw new HibernateException( "disconnected session" );
			}
			session.initializeCollection( this, false );
		}
	}


	/**
	 * Get the current snapshot from the session
	 */
	@SuppressWarnings({"JavaDoc"})
	protected final Serializable getSnapshot() {
		return session.getPersistenceContext().getSnapshot( this );
	}

	/**
	 * Is this instance initialized?
	 */
	public final boolean wasInitialized() {
		return initialized;
	}

	public boolean isRowUpdatePossible() {
		return true;
	}

	/**
	 * Does this instance have any "queued" additions?
	 */
	public final boolean hasQueuedOperations() {
		return operationQueue != null;
	}

	/**
	 * Iterate the "queued" additions
	 */
	public final Iterator queuedAdditionIterator() {
		if ( hasQueuedOperations() ) {
			return new Iterator() {
				int i = 0;

				public Object next() {
					return operationQueue.get( i++ ).getAddedInstance();
				}

				public boolean hasNext() {
					return i < operationQueue.size();
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		else {
			return EmptyIterator.INSTANCE;
		}
	}

	/**
	 * Iterate the "queued" additions
	 */
	@SuppressWarnings({"unchecked"})
	public final Collection getQueuedOrphans(String entityName) {
		if ( hasQueuedOperations() ) {
			Collection additions = new ArrayList( operationQueue.size() );
			Collection removals = new ArrayList( operationQueue.size() );
			for ( DelayedOperation operation : operationQueue ) {
				additions.add( operation.getAddedInstance() );
				removals.add( operation.getOrphan() );
			}
			return getOrphans( removals, additions, entityName, session );
		}
		else {
			return Collections.EMPTY_LIST;
		}
	}

	/**
	 * Called before inserting rows, to ensure that any surrogate keys
	 * are fully generated
	 */
	public void preInsert(CollectionPersister persister) throws HibernateException {
	}

	/**
	 * Called after inserting a row, to fetch the natively generated id
	 */
	public void afterRowInsert(CollectionPersister persister, Object entry, int i) throws HibernateException {
	}

	/**
	 * get all "orphaned" elements
	 */
	public abstract Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException;

	/**
	 * Get the current session
	 */
	@SuppressWarnings({"JavaDoc"})
	public final SessionImplementor getSession() {
		return session;
	}

	protected final class IteratorProxy implements Iterator {
		protected final Iterator itr;

		public IteratorProxy(Iterator itr) {
			this.itr = itr;
		}

		public boolean hasNext() {
			return itr.hasNext();
		}

		public Object next() {
			return itr.next();
		}

		public void remove() {
			write();
			itr.remove();
		}

	}

	protected final class ListIteratorProxy implements ListIterator {
		protected final ListIterator itr;

		public ListIteratorProxy(ListIterator itr) {
			this.itr = itr;
		}

		@SuppressWarnings({"unchecked"})
		public void add(Object o) {
			write();
			itr.add( o );
		}

		public boolean hasNext() {
			return itr.hasNext();
		}

		public boolean hasPrevious() {
			return itr.hasPrevious();
		}

		public Object next() {
			return itr.next();
		}

		public int nextIndex() {
			return itr.nextIndex();
		}

		public Object previous() {
			return itr.previous();
		}

		public int previousIndex() {
			return itr.previousIndex();
		}

		public void remove() {
			write();
			itr.remove();
		}

		@SuppressWarnings({"unchecked"})
		public void set(Object o) {
			write();
			itr.set( o );
		}

	}

	protected class SetProxy implements java.util.Set {
		protected final Collection set;

		public SetProxy(Collection set) {
			this.set = set;
		}

		@SuppressWarnings({"unchecked"})
		public boolean add(Object o) {
			write();
			return set.add( o );
		}

		@SuppressWarnings({"unchecked"})
		public boolean addAll(Collection c) {
			write();
			return set.addAll( c );
		}

		public void clear() {
			write();
			set.clear();
		}

		public boolean contains(Object o) {
			return set.contains( o );
		}

		public boolean containsAll(Collection c) {
			return set.containsAll( c );
		}

		public boolean isEmpty() {
			return set.isEmpty();
		}

		public Iterator iterator() {
			return new IteratorProxy( set.iterator() );
		}

		public boolean remove(Object o) {
			write();
			return set.remove( o );
		}

		public boolean removeAll(Collection c) {
			write();
			return set.removeAll( c );
		}

		public boolean retainAll(Collection c) {
			write();
			return set.retainAll( c );
		}

		public int size() {
			return set.size();
		}

		public Object[] toArray() {
			return set.toArray();
		}

		@SuppressWarnings({"unchecked"})
		public Object[] toArray(Object[] array) {
			return set.toArray( array );
		}

	}

	protected final class ListProxy implements java.util.List {
		protected final List list;

		public ListProxy(List list) {
			this.list = list;
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public void add(int index, Object value) {
			write();
			list.add( index, value );
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public boolean add(Object o) {
			write();
			return list.add( o );
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public boolean addAll(Collection c) {
			write();
			return list.addAll( c );
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public boolean addAll(int i, Collection c) {
			write();
			return list.addAll( i, c );
		}

		@Override
		public void clear() {
			write();
			list.clear();
		}

		@Override
		public boolean contains(Object o) {
			return list.contains( o );
		}

		@Override
		public boolean containsAll(Collection c) {
			return list.containsAll( c );
		}

		@Override
		public Object get(int i) {
			return list.get( i );
		}

		@Override
		public int indexOf(Object o) {
			return list.indexOf( o );
		}

		@Override
		public boolean isEmpty() {
			return list.isEmpty();
		}

		@Override
		public Iterator iterator() {
			return new IteratorProxy( list.iterator() );
		}

		@Override
		public int lastIndexOf(Object o) {
			return list.lastIndexOf( o );
		}

		@Override
		public ListIterator listIterator() {
			return new ListIteratorProxy( list.listIterator() );
		}

		@Override
		public ListIterator listIterator(int i) {
			return new ListIteratorProxy( list.listIterator( i ) );
		}

		@Override
		public Object remove(int i) {
			write();
			return list.remove( i );
		}

		@Override
		public boolean remove(Object o) {
			write();
			return list.remove( o );
		}

		@Override
		public boolean removeAll(Collection c) {
			write();
			return list.removeAll( c );
		}

		@Override
		public boolean retainAll(Collection c) {
			write();
			return list.retainAll( c );
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public Object set(int i, Object o) {
			write();
			return list.set( i, o );
		}

		@Override
		public int size() {
			return list.size();
		}

		@Override
		public List subList(int i, int j) {
			return list.subList( i, j );
		}

		@Override
		public Object[] toArray() {
			return list.toArray();
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public Object[] toArray(Object[] array) {
			return list.toArray( array );
		}

	}

	/**
	 * Contract for operations which are part of a collection's operation queue.
	 */
	protected interface DelayedOperation {
		public void operate();

		public Object getAddedInstance();

		public Object getOrphan();
	}

	/**
	 * Given a collection of entity instances that used to
	 * belong to the collection, and a collection of instances
	 * that currently belong, return a collection of orphans
	 */
	@SuppressWarnings({"JavaDoc", "unchecked"})
	protected static Collection getOrphans(
			Collection oldElements,
			Collection currentElements,
			String entityName,
			SessionImplementor session) throws HibernateException {

		// short-circuit(s)
		if ( currentElements.size() == 0 ) {
			return oldElements; // no new elements, the old list contains only Orphans
		}
		if ( oldElements.size() == 0 ) {
			return oldElements; // no old elements, so no Orphans neither
		}

		final EntityPersister entityPersister = session.getFactory().getEntityPersister( entityName );
		final Type idType = entityPersister.getIdentifierType();

		// create the collection holding the Orphans
		Collection res = new ArrayList();

		// collect EntityIdentifier(s) of the *current* elements - add them into a HashSet for fast access
		java.util.Set currentIds = new HashSet();
		java.util.Set currentSaving = new IdentitySet();
		for ( Object current : currentElements ) {
			if ( current != null && ForeignKeys.isNotTransient( entityName, current, null, session ) ) {
				EntityEntry ee = session.getPersistenceContext().getEntry( current );
				if ( ee != null && ee.getStatus() == Status.SAVING ) {
					currentSaving.add( current );
				}
				else {
					Serializable currentId = ForeignKeys.getEntityIdentifierIfNotUnsaved(
							entityName,
							current,
							session
					);
					currentIds.add( new TypedValue( idType, currentId, entityPersister.getEntityMode() ) );
				}
			}
		}

		// iterate over the *old* list
		for ( Object old : oldElements ) {
			if ( !currentSaving.contains( old ) ) {
				Serializable oldId = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, old, session );
				if ( !currentIds.contains( new TypedValue( idType, oldId, entityPersister.getEntityMode() ) ) ) {
					res.add( old );
				}
			}
		}

		return res;
	}

	public static void identityRemove(
			Collection list,
			Object object,
			String entityName,
			SessionImplementor session) throws HibernateException {

		if ( object != null && ForeignKeys.isNotTransient( entityName, object, null, session ) ) {
			final EntityPersister entityPersister = session.getFactory().getEntityPersister( entityName );
			Type idType = entityPersister.getIdentifierType();

			Serializable idOfCurrent = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, object, session );
			Iterator itr = list.iterator();
			while ( itr.hasNext() ) {
				Serializable idOfOld = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, itr.next(), session );
				if ( idType.isEqual( idOfCurrent, idOfOld, session.getFactory() ) ) {
					itr.remove();
					break;
				}
			}

		}
	}

	public Object getIdentifier(Object entry, int i) {
		throw new UnsupportedOperationException();
	}

	public Object getOwner() {
		return owner;
	}

	public void setOwner(Object owner) {
		this.owner = owner;
	}

}

