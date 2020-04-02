/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.CompositeType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.type.UUIDBinaryType;
import org.hibernate.type.UUIDCharType;

/**
 * Base class implementing {@link org.hibernate.collection.spi.PersistentCollection}
 *
 * @author Gavin King
 */
public abstract class AbstractPersistentCollection implements Serializable, PersistentCollection {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPersistentCollection.class );

	private transient SharedSessionContractImplementor session;
	private boolean isTempSession = false;
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
	protected boolean elementRemoved;
	private Serializable storedSnapshot;

	private String sessionFactoryUuid;
	private boolean allowLoadOutsideTransaction;

	/**
	 * Not called by Hibernate, but used by non-JDK serialization,
	 * eg. SOAP libraries.
	 */
	public AbstractPersistentCollection() {
	}

	protected AbstractPersistentCollection(SharedSessionContractImplementor session) {
		this.session = session;
	}

	/**
	 * @deprecated {@link #AbstractPersistentCollection(SharedSessionContractImplementor)} should be used instead.
	 */
	@Deprecated
	protected AbstractPersistentCollection(SessionImplementor session) {
		this( (SharedSessionContractImplementor) session );
	}

	@Override
	public final String getRole() {
		return role;
	}

	@Override
	public final Serializable getKey() {
		return key;
	}

	@Override
	public final boolean isUnreferenced() {
		return role == null;
	}

	@Override
	public final boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isElementRemoved() {
		return elementRemoved;
	}

	@Override
	public final void clearDirty() {
		dirty = false;
		elementRemoved = false;
	}

	@Override
	public final void dirty() {
		dirty = true;
	}

	@Override
	public final Serializable getStoredSnapshot() {
		return storedSnapshot;
	}

	//Careful: these methods do not initialize the collection.

	@Override
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
				final boolean isExtraLazy = withTemporarySessionIfNeeded(
						new LazyInitializationWork<Boolean>() {
							@Override
							public Boolean doWork() {
								final CollectionEntry entry = session.getPersistenceContextInternal().getCollectionEntry( AbstractPersistentCollection.this );

								if ( entry != null ) {
									final CollectionPersister persister = entry.getLoadedPersister();
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

	/**
	 * TBH not sure why this is public
	 *
	 * @param <T> The java type of the return for this LazyInitializationWork
	 */
	public static interface LazyInitializationWork<T> {
		/**
		 * Do the represented work and return the result.
		 *
		 * @return The result
		 */
		public T doWork();
	}

	private <T> T withTemporarySessionIfNeeded(LazyInitializationWork<T> lazyInitializationWork) {
		SharedSessionContractImplementor tempSession = null;

		if ( session == null ) {
			if ( allowLoadOutsideTransaction ) {
				tempSession = openTemporarySessionForLoading();
			}
			else {
				throwLazyInitializationException( "could not initialize proxy - no Session" );
			}
		}
		else if ( !session.isOpenOrWaitingForAutoClose() ) {
			if ( allowLoadOutsideTransaction ) {
				tempSession = openTemporarySessionForLoading();
			}
			else {
				throwLazyInitializationException( "could not initialize proxy - the owning Session was closed" );
			}
		}
		else if ( !session.isConnected() ) {
			if ( allowLoadOutsideTransaction ) {
				tempSession = openTemporarySessionForLoading();
			}
			else {
				throwLazyInitializationException( "could not initialize proxy - the owning Session is disconnected" );
			}
		}

		SharedSessionContractImplementor originalSession = null;
		boolean isJTA = false;

		if ( tempSession != null ) {
			isTempSession = true;
			originalSession = session;
			session = tempSession;

			isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

			if ( !isJTA ) {
				// Explicitly handle the transactions only if we're not in
				// a JTA environment.  A lazy loading temporary session can
				// be created even if a current session and transaction are
				// open (ex: session.clear() was used).  We must prevent
				// multiple transactions.
				( (Session) session ).beginTransaction();
			}

			session.getPersistenceContextInternal().addUninitializedDetachedCollection(
					session.getFactory().getCollectionPersister( getRole() ),
					this
			);
		}

		try {
			return lazyInitializationWork.doWork();
		}
		finally {
			if ( tempSession != null ) {
				// make sure the just opened temp session gets closed!
				isTempSession = false;
				session = originalSession;

				try {
					if ( !isJTA ) {
						( (Session) tempSession ).getTransaction().commit();
					}
					( (Session) tempSession ).close();
				}
				catch (Exception e) {
					LOG.warn( "Unable to close temporary session used to load lazy collection associated to no session" );
				}
			}
		}
	}

	private SharedSessionContractImplementor openTemporarySessionForLoading() {
		if ( sessionFactoryUuid == null ) {
			throwLazyInitializationException( "SessionFactory UUID not known to create temporary Session for loading" );
		}

		final SessionFactoryImplementor sf = (SessionFactoryImplementor)
				SessionFactoryRegistry.INSTANCE.getSessionFactory( sessionFactoryUuid );
		final SharedSessionContractImplementor session = (SharedSessionContractImplementor) sf.openSession();
		session.getPersistenceContextInternal().setDefaultReadOnly( true );
		session.setFlushMode( FlushMode.MANUAL );
		return session;
	}

	protected Boolean readIndexExistence(final Object index) {
		if ( !initialized ) {
			final Boolean extraLazyExistenceCheck = withTemporarySessionIfNeeded(
					new LazyInitializationWork<Boolean>() {
						@Override
						public Boolean doWork() {
							final CollectionEntry entry = session.getPersistenceContextInternal().getCollectionEntry( AbstractPersistentCollection.this );
							final CollectionPersister persister = entry.getLoadedPersister();
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
			final Boolean extraLazyExistenceCheck = withTemporarySessionIfNeeded(
					new LazyInitializationWork<Boolean>() {
						@Override
						public Boolean doWork() {
							final CollectionEntry entry = session.getPersistenceContextInternal().getCollectionEntry( AbstractPersistentCollection.this );
							final CollectionPersister persister = entry.getLoadedPersister();
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
					final CollectionEntry entry = session.getPersistenceContextInternal().getCollectionEntry( AbstractPersistentCollection.this );
					final CollectionPersister persister = entry.getLoadedPersister();
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

			final ExtraLazyElementByIndexReader reader = new ExtraLazyElementByIndexReader();
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

	protected boolean isConnectedToSession() {
		return session != null
				&& session.isOpen()
				&& session.getPersistenceContextInternal().containsCollection( this );
	}

	protected boolean isInitialized() {
		return initialized;
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
		return !initialized
				&& isConnectedToSession()
				&& isInverseCollection();
	}

	/**
	 * Is this collection in a state that would allow us to
	 * "queue" puts? This is a special case, because of orphan
	 * delete.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isPutQueueEnabled() {
		return !initialized
				&& isConnectedToSession()
				&& isInverseOneToManyOrNoOrphanDelete();
	}

	/**
	 * Is this collection in a state that would allow us to
	 * "queue" clear? This is a special case, because of orphan
	 * delete.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isClearQueueEnabled() {
		return !initialized
				&& isConnectedToSession()
				&& isInverseCollectionNoOrphanDelete();
	}

	/**
	 * Is this the "inverse" end of a bidirectional association?
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isInverseCollection() {
		final CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( this );
		return ce != null && ce.getLoadedPersister().isInverse();
	}

	/**
	 * Is this the "inverse" end of a bidirectional association with
	 * no orphan delete enabled?
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isInverseCollectionNoOrphanDelete() {
		final CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( this );
		if ( ce == null ) {
			return false;
		}
		final CollectionPersister loadedPersister = ce.getLoadedPersister();
		return loadedPersister.isInverse() && !loadedPersister.hasOrphanDelete();
	}

	/**
	 * Is this the "inverse" end of a bidirectional one-to-many, or
	 * of a collection with no orphan delete?
	 */
	@SuppressWarnings({"JavaDoc"})
	protected boolean isInverseOneToManyOrNoOrphanDelete() {
		final CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( this );
		if ( ce == null ) {
			return false;
		}
		final CollectionPersister loadedPersister = ce.getLoadedPersister();
		return loadedPersister.isInverse() && ( loadedPersister.isOneToMany() || !loadedPersister.hasOrphanDelete() );
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
		//needed so that we remove this collection from the second-level cache
		dirty = true;
	}

	/**
	 * Replace entity instances with copy in {@code copyCache}/.
	 *
	 * @param copyCache - mapping from entity in the process of being
	 *                    merged to managed copy.
	 */
	public final void replaceQueuedOperationValues(CollectionPersister persister, Map copyCache) {
		for ( DelayedOperation operation : operationQueue ) {
			if ( ValueDelayedOperation.class.isInstance( operation ) ) {
				( (ValueDelayedOperation) operation ).replace( persister, copyCache );
			}
		}
	}

	/**
	 * After reading all existing elements from the database,
	 * add the queued elements to the underlying collection.
	 */
	protected final void performQueuedOperations() {
		for ( DelayedOperation operation : operationQueue ) {
			operation.operate();
		}
		clearOperationQueue();
	}

	@Override
	public void setSnapshot(Serializable key, String role, Serializable snapshot) {
		this.key = key;
		this.role = role;
		this.storedSnapshot = snapshot;
	}

	@Override
	public void postAction() {
		clearOperationQueue();
		cachedSize = -1;
		clearDirty();
	}

	public final void clearOperationQueue() {
		operationQueue = null;
	}

	@Override
	public Object getValue() {
		return this;
	}

	@Override
	public void beginRead() {
		// override on some subclasses
		initializing = true;
	}

	@Override
	public boolean endRead() {
		//override on some subclasses
		return afterInitialize();
	}

	@Override
	public boolean afterInitialize() {
		setInitialized();
		//do this bit after setting initialized to true or it will recurse
		if ( hasQueuedOperations() ) {
			performQueuedOperations();
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

	@Override
	public boolean isDirectlyAccessible() {
		return directlyAccessible;
	}

	@Override
	public final boolean unsetSession(SharedSessionContractImplementor currentSession) {
		prepareForPossibleLoadingOutsideTransaction();
		if ( currentSession == this.session ) {
			if ( !isTempSession ) {
				if ( hasQueuedOperations() ) {
					final String collectionInfoString = MessageHelper.collectionInfoString( getRole(), getKey() );
					try {
						final TransactionStatus transactionStatus =
								session.getTransactionCoordinator().getTransactionDriverControl().getStatus();
						if ( transactionStatus.isOneOf(
								TransactionStatus.ROLLED_BACK,
								TransactionStatus.MARKED_ROLLBACK,
								TransactionStatus.FAILED_COMMIT,
								TransactionStatus.FAILED_ROLLBACK,
								TransactionStatus.ROLLING_BACK
						) ) {
							// It was due to a rollback.
							LOG.queuedOperationWhenDetachFromSessionOnRollback( collectionInfoString );
						}
						else {
							// We don't know why the collection is being detached.
							// Just log the info.
							LOG.queuedOperationWhenDetachFromSession( collectionInfoString );
						}
					}
					catch (Exception e) {
						// We don't know why the collection is being detached.
						// Just log the info.
						LOG.queuedOperationWhenDetachFromSession( collectionInfoString );
					}
				}
				this.session = null;
			}
			return true;
		}
		else {
			if ( this.session != null ) {
				LOG.logCannotUnsetUnexpectedSessionInCollection( generateUnexpectedSessionStateMessage( currentSession ) );
			}
			return false;
		}
	}

	protected void prepareForPossibleLoadingOutsideTransaction() {
		if ( session != null ) {
			allowLoadOutsideTransaction = session.getFactory().getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();

			if ( allowLoadOutsideTransaction && sessionFactoryUuid == null ) {
				sessionFactoryUuid = session.getFactory().getUuid();
			}
		}
	}

	@Override
	public final boolean setCurrentSession(SharedSessionContractImplementor session) throws HibernateException {
		if ( session == this.session ) {
			return false;
		}
		else if ( this.session != null ) {
			final String msg = generateUnexpectedSessionStateMessage( session );
			if ( isConnectedToSession() ) {
				throw new HibernateException(
						"Illegal attempt to associate a collection with two open sessions. " + msg
				);
			}
			else {
				LOG.logUnexpectedSessionInCollectionNotConnected( msg );
			}
		}
		if ( hasQueuedOperations() ) {
			LOG.queuedOperationWhenAttachToSession( MessageHelper.collectionInfoString( getRole(), getKey() ) );
		}
		this.session = session;
		return true;
	}

	private String generateUnexpectedSessionStateMessage(SharedSessionContractImplementor session) {
		// NOTE: If this.session != null, this.session may be operating on this collection
		// (e.g., by changing this.role, this.key, or even this.session) in a different thread.

		// Grab the current role and key (it can still get changed by this.session...)
		// If this collection is connected to this.session, then this.role and this.key should
		// be consistent with the CollectionEntry in this.session (as long as this.session doesn't
		// change it). Don't access the CollectionEntry in this.session because that could result
		// in multi-threaded access to this.session.
		final String roleCurrent = role;
		final Serializable keyCurrent = key;

		final StringBuilder sb = new StringBuilder( "Collection : " );
		if ( roleCurrent != null ) {
			sb.append( MessageHelper.collectionInfoString( roleCurrent, keyCurrent ) );
		}
		else {
			final CollectionEntry ce = session.getPersistenceContextInternal().getCollectionEntry( this );
			if ( ce != null ) {
				sb.append(
						MessageHelper.collectionInfoString(
								ce.getLoadedPersister(),
								this,
								ce.getLoadedKey(),
								session
						)
				);
			}
			else {
				sb.append( "<unknown>" );
			}
		}
		// only include the collection contents if debug logging
		if ( LOG.isDebugEnabled() ) {
			final String collectionContents = wasInitialized() ? toString() : "<uninitialized>";
			sb.append( "\nCollection contents: [" ).append( collectionContents ).append( "]" );
		}
		return sb.toString();
	}

	@Override
	public boolean needsRecreate(CollectionPersister persister) {
		// Workaround for situations like HHH-7072.  If the collection element is a component that consists entirely
		// of nullable properties, we currently have to forcefully recreate the entire collection.  See the use
		// of hasNotNullableColumns in the AbstractCollectionPersister constructor for more info.  In order to delete
		// row-by-row, that would require SQL like "WHERE ( COL = ? OR ( COL is null AND ? is null ) )", rather than
		// the current "WHERE COL = ?" (fails for null for most DBs).  Note that
		// the param would have to be bound twice.  Until we eventually add "parameter bind points" concepts to the
		// AST in ORM 5+, handling this type of condition is either extremely difficult or impossible.  Forcing
		// recreation isn't ideal, but not really any other option in ORM 4.
		// Selecting a type used in where part of update statement
		// (must match condidion in org.hibernate.persister.collection.BasicCollectionPersister.doUpdateRows).
		// See HHH-9474
		Type whereType;
		if ( persister.hasIndex() ) {
			whereType = persister.getIndexType();
		}
		else {
			whereType = persister.getElementType();
		}
		if ( whereType instanceof CompositeType ) {
			CompositeType componentIndexType = (CompositeType) whereType;
			return !componentIndexType.hasNotNullProperty();
		}
		return false;
	}

	@Override
	public final void forceInitialization() throws HibernateException {
		if ( !initialized ) {
			if ( initializing ) {
				throw new AssertionFailure( "force initialize loading collection" );
			}
			initialize( false );
		}
	}


	/**
	 * Get the current snapshot from the session
	 */
	@SuppressWarnings({"JavaDoc"})
	protected final Serializable getSnapshot() {
		return session.getPersistenceContext().getSnapshot( this );
	}

	@Override
	public final boolean wasInitialized() {
		return initialized;
	}

	@Override
	public boolean isRowUpdatePossible() {
		return true;
	}

	@Override
	public final boolean hasQueuedOperations() {
		return operationQueue != null;
	}

	@Override
	public final Iterator queuedAdditionIterator() {
		if ( hasQueuedOperations() ) {
			return new Iterator() {
				private int index;

				@Override
				public Object next() {
					return operationQueue.get( index++ ).getAddedInstance();
				}

				@Override
				public boolean hasNext() {
					return index < operationQueue.size();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		else {
			return Collections.emptyIterator();
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public final Collection getQueuedOrphans(String entityName) {
		if ( hasQueuedOperations() ) {
			final Collection additions = new ArrayList( operationQueue.size() );
			final Collection removals = new ArrayList( operationQueue.size() );
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

	@Override
	public void preInsert(CollectionPersister persister) throws HibernateException {
	}

	@Override
	public void afterRowInsert(CollectionPersister persister, Object entry, int i) throws HibernateException {
	}

	@Override
	public abstract Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException;

	/**
	 * Get the session currently associated with this collection.
	 *
	 * @return The session
	 */
	public final SharedSessionContractImplementor getSession() {
		return session;
	}

	protected final class IteratorProxy implements Iterator {
		protected final Iterator itr;

		public IteratorProxy(Iterator itr) {
			this.itr = itr;
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public Object next() {
			return itr.next();
		}

		@Override
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

		@Override
		@SuppressWarnings({"unchecked"})
		public void add(Object o) {
			write();
			itr.add( o );
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public boolean hasPrevious() {
			return itr.hasPrevious();
		}

		@Override
		public Object next() {
			return itr.next();
		}

		@Override
		public int nextIndex() {
			return itr.nextIndex();
		}

		@Override
		public Object previous() {
			return itr.previous();
		}

		@Override
		public int previousIndex() {
			return itr.previousIndex();
		}

		@Override
		public void remove() {
			write();
			itr.remove();
		}

		@Override
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

		@Override
		@SuppressWarnings({"unchecked"})
		public boolean add(Object o) {
			write();
			return set.add( o );
		}

		@Override
		@SuppressWarnings({"unchecked"})
		public boolean addAll(Collection c) {
			write();
			return set.addAll( c );
		}

		@Override
		public void clear() {
			write();
			set.clear();
		}

		@Override
		public boolean contains(Object o) {
			return set.contains( o );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean containsAll(Collection c) {
			return set.containsAll( c );
		}

		@Override
		public boolean isEmpty() {
			return set.isEmpty();
		}

		@Override
		public Iterator iterator() {
			return new IteratorProxy( set.iterator() );
		}

		@Override
		public boolean remove(Object o) {
			write();
			return set.remove( o );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean removeAll(Collection c) {
			write();
			return set.removeAll( c );
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean retainAll(Collection c) {
			write();
			return set.retainAll( c );
		}

		@Override
		public int size() {
			return set.size();
		}

		@Override
		public Object[] toArray() {
			return set.toArray();
		}

		@Override
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
		@SuppressWarnings("unchecked")
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
		@SuppressWarnings("unchecked")
		public boolean removeAll(Collection c) {
			write();
			return list.removeAll( c );
		}

		@Override
		@SuppressWarnings("unchecked")
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

	protected interface ValueDelayedOperation extends DelayedOperation {
		void replace(CollectionPersister collectionPersister, Map copyCache);
	}

	protected abstract class AbstractValueDelayedOperation implements ValueDelayedOperation {
		private Object addedValue;
		private Object orphan;

		protected AbstractValueDelayedOperation(Object addedValue, Object orphan) {
			this.addedValue = addedValue;
			this.orphan = orphan;
		}

		public void replace(CollectionPersister persister, Map copyCache) {
			if ( addedValue != null ) {
				addedValue = getReplacement( persister.getElementType(), addedValue, copyCache );
			}
		}

		protected final Object getReplacement(Type type, Object current, Map copyCache) {
			return type.replace( current, null, session, owner, copyCache );
		}

		@Override
		public final Object getAddedInstance() {
			return addedValue;
		}

		@Override
		public final Object getOrphan() {
			return orphan;
		}
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
			SharedSessionContractImplementor session) throws HibernateException {

		// short-circuit(s)
		if ( currentElements.size() == 0 ) {
			// no new elements, the old list contains only Orphans
			return oldElements;
		}
		if ( oldElements.size() == 0 ) {
			// no old elements, so no Orphans neither
			return oldElements;
		}

		final EntityPersister entityPersister = session.getFactory().getEntityPersister( entityName );
		final Type idType = entityPersister.getIdentifierType();
		final boolean useIdDirect = mayUseIdDirect( idType );

		// create the collection holding the Orphans
		final Collection res = new ArrayList();

		// collect EntityIdentifier(s) of the *current* elements - add them into a HashSet for fast access
		final java.util.Set currentIds = new HashSet();
		final java.util.Set currentSaving = new IdentitySet();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		for ( Object current : currentElements ) {
			if ( current != null && ForeignKeys.isNotTransient( entityName, current, null, session ) ) {
				final EntityEntry ee = persistenceContext.getEntry( current );
				if ( ee != null && ee.getStatus() == Status.SAVING ) {
					currentSaving.add( current );
				}
				else {
					final Serializable currentId = ForeignKeys.getEntityIdentifierIfNotUnsaved(
							entityName,
							current,
							session
					);
					currentIds.add( useIdDirect ? currentId : new TypedValue( idType, currentId ) );
				}
			}
		}

		// iterate over the *old* list
		for ( Object old : oldElements ) {
			if ( !currentSaving.contains( old ) ) {
				final Serializable oldId = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, old, session );
				if ( !currentIds.contains( useIdDirect ? oldId : new TypedValue( idType, oldId ) ) ) {
					res.add( old );
				}
			}
		}

		return res;
	}

	private static boolean mayUseIdDirect(Type idType) {
		return idType == StringType.INSTANCE
			|| idType == IntegerType.INSTANCE
			|| idType == LongType.INSTANCE
			|| idType == UUIDBinaryType.INSTANCE
			|| idType == UUIDCharType.INSTANCE
			|| idType == PostgresUUIDType.INSTANCE;
	}

	/**
	 * Removes entity entries that have an equal identifier with the incoming entity instance
	 *
	 * @param list The list containing the entity instances
	 * @param entityInstance The entity instance to match elements.
	 * @param entityName The entity name
	 * @param session The session
	 */
	public static void identityRemove(
			Collection list,
			Object entityInstance,
			String entityName,
			SharedSessionContractImplementor session) {

		if ( entityInstance != null && ForeignKeys.isNotTransient( entityName, entityInstance, null, session ) ) {
			final EntityPersister entityPersister = session.getFactory().getEntityPersister( entityName );
			final Type idType = entityPersister.getIdentifierType();

			final Serializable idOfCurrent = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, entityInstance, session );
			final Iterator itr = list.iterator();
			while ( itr.hasNext() ) {
				final Serializable idOfOld = ForeignKeys.getEntityIdentifierIfNotUnsaved( entityName, itr.next(), session );
				if ( idType.isEqual( idOfCurrent, idOfOld, session.getFactory() ) ) {
					itr.remove();
					break;
				}
			}

		}
	}

	/**
	 * Removes entity entries that have an equal identifier with the incoming entity instance
	 *
	 * @param list The list containing the entity instances
	 * @param entityInstance The entity instance to match elements.
	 * @param entityName The entity name
	 * @param session The session
	 *
	 * @deprecated {@link #identityRemove(Collection, Object, String, SharedSessionContractImplementor)}
	 *             should be used instead.
	 */
	@Deprecated
	public static void identityRemove(
			Collection list,
			Object entityInstance,
			String entityName,
			SessionImplementor session) {
		identityRemove( list, entityInstance, entityName, (SharedSessionContractImplementor) session );
	}

	@Override
	public Object getIdentifier(Object entry, int i) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getOwner() {
		return owner;
	}

	@Override
	public void setOwner(Object owner) {
		this.owner = owner;
	}

}
