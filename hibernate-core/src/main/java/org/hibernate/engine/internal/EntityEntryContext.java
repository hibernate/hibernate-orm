/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.InstanceIdentityStore;
import org.hibernate.persister.entity.EntityPersister;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.hibernate.engine.internal.ManagedTypeHelper.asManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptableOrNull;
import static org.hibernate.engine.internal.ManagedTypeHelper.isManagedEntity;

/**
 * Defines a context for maintaining the relation between an entity associated with the
 * {@code Session} ultimately owning this {@code EntityEntryContext} instance and that
 * entity's corresponding {@link EntityEntry}. Two approaches are supported:<ul>
 *     <li>
 *         the entity to {@link EntityEntry} association is maintained in a {code @Map}
 *         within this class, or
 *     </li>
 *     <li>
 *         the {@link EntityEntry} is injected into the entity via it implementing the
 *         {@link ManagedEntity} contract, either directly or through bytecode enhancement.
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class EntityEntryContext {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( EntityEntryContext.class );

	private final transient PersistenceContext persistenceContext;

	private transient InstanceIdentityStore<ImmutableManagedEntityHolder> immutableManagedEntityXref;
	private transient int currentInstanceId = 1;

	private transient ManagedEntity head;
	private transient ManagedEntity tail;
	private transient int count;

	private transient IdentityHashMap<Object,ManagedEntity> nonEnhancedEntityXref;

	@SuppressWarnings("unchecked")
	private transient Map.Entry<Object,EntityEntry>[] reentrantSafeEntries = new Map.Entry[0];
	private transient boolean dirty;

	/**
	 * Constructs a EntityEntryContext
	 */
	public EntityEntryContext(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Adds the entity and {@link EntityEntry} to this context, associating them.
	 *
	 * @param entity The entity
	 * @param entityEntry The entry
	 */
	public void addEntityEntry(Object entity, EntityEntry entityEntry) {
		// IMPORTANT!!!!!
		//		add is called more than once of some entities.  In such cases the first
		//		call is simply setting up a "marker" to avoid infinite looping from reentrancy

		// any addition (even the double one described above) should invalidate the cross-ref array
		dirty = true;

		// We only need to check a mutable EntityEntry is associated with the same PersistenceContext.
		// Immutable EntityEntry can be associated with multiple PersistenceContexts, so no need to check.
		// ImmutableEntityEntry#getPersistenceContext() throws an exception (HHH-10251).
		assert !entityEntry.getPersister().isMutable()
			|| ( (EntityEntryImpl) entityEntry ).getPersistenceContext() == persistenceContext;

		// Determine the appropriate ManagedEntity instance to use based on whether the entity is enhanced or not.
		// Throw an exception if entity is a mutable ManagedEntity that is associated with a different
		// PersistenceContext.
		ManagedEntity managedEntity = getAssociatedManagedEntity( entity );

		int instanceId = nextManagedEntityInstanceId();
		final boolean alreadyAssociated = managedEntity != null;
		if ( !alreadyAssociated ) {
			if ( isManagedEntity( entity ) ) {
				final ManagedEntity managed = asManagedEntity( entity );
				assert managed.$$_hibernate_getInstanceId() == 0;
				if ( entityEntry.getPersister().isMutable() ) {
					managedEntity = managed;
					// We know that managedEntity is not associated with the same PersistenceContext.
					// Check if managedEntity is associated with a different PersistenceContext.
					checkNotAssociatedWithOtherPersistenceContextIfMutable( managedEntity );
				}
				else {
					// Create a holder for PersistenceContext-related data.
					managedEntity = new ImmutableManagedEntityHolder( managed );
					if ( !isReferenceCachingEnabled( entityEntry.getPersister() ) ) {
						putImmutableManagedEntity( managed, instanceId, (ImmutableManagedEntityHolder) managedEntity );
					}
					else {
						// When reference caching is enabled we cannot set the instance-id on the entity instance
						instanceId = 0;
						putManagedEntity( entity, managedEntity );
					}
				}
			}
			else {
				managedEntity = new ManagedEntityImpl( entity );
				putManagedEntity( entity, managedEntity );
			}
		}

		if ( alreadyAssociated ) {
			// if the entity was already associated with the context, skip the linking step.
			managedEntity.$$_hibernate_setEntityEntry( entityEntry );
			return;
		}

		// TODO: can dirty be set to true here?

		// finally, set up linking and count
		final ManagedEntity previous;
		if ( tail == null ) {
			assert head == null;
			// Protect against stale data in the ManagedEntity and nullify previous reference.
			previous = null;
			head = managedEntity;
			tail = head;
			count = 1;
		}
		else {
			tail.$$_hibernate_setNextManagedEntity( managedEntity );
			previous = tail;
			tail = managedEntity;
			count++;
		}

		// Protect against stale data left in the ManagedEntity nullify next reference.
		managedEntity.$$_hibernate_setPersistenceInfo( entityEntry, previous, null, instanceId );
	}

	private static boolean isReferenceCachingEnabled(EntityPersister persister) {
		// Immutable entities which can use reference caching are treated as non-enhanced entities, as setting
		// the instance-id on them would be problematic in different sessions
		return persister.canUseReferenceCacheEntries() && persister.canReadFromCache();
	}

	private ManagedEntity getAssociatedManagedEntity(Object entity) {
		if ( isManagedEntity( entity ) ) {
			final ManagedEntity managedEntity = asManagedEntity( entity );
			if ( managedEntity.$$_hibernate_getEntityEntry() == null ) {
				// it is not associated
				return null;
			}
			final EntityEntryImpl entityEntry =
					(EntityEntryImpl) managedEntity.$$_hibernate_getEntityEntry();

			if ( entityEntry.getPersister().isMutable() ) {
				return entityEntry.getPersistenceContext() == persistenceContext
						? managedEntity // it is associated
						: null;
			}
			else if ( !isReferenceCachingEnabled( entityEntry.getPersister() ) ) {
				// if managedEntity is associated with this EntityEntryContext, it may have
				// an entry in immutableManagedEntityXref and its holder will be returned.
				return immutableManagedEntityXref != null
						? immutableManagedEntityXref.get( managedEntity.$$_hibernate_getInstanceId(), managedEntity )
						: null;
			}
		}
		return nonEnhancedEntityXref != null
				? nonEnhancedEntityXref.get( entity )
				: null;
	}

	private void putManagedEntity(Object entity, ManagedEntity managedEntity) {
		if ( nonEnhancedEntityXref == null ) {
			nonEnhancedEntityXref = new IdentityHashMap<>();
		}
		nonEnhancedEntityXref.put( entity, managedEntity );
	}

	private int nextManagedEntityInstanceId() {
		return currentInstanceId++;
	}

	private void putImmutableManagedEntity(ManagedEntity managed, int instanceId, ImmutableManagedEntityHolder holder) {
		if ( immutableManagedEntityXref == null ) {
			immutableManagedEntityXref = new InstanceIdentityStore<>();
		}
		immutableManagedEntityXref.put( managed, instanceId, holder );
	}

	private void checkNotAssociatedWithOtherPersistenceContextIfMutable(ManagedEntity managedEntity) {
		// we only have to check mutable managedEntity
		final EntityEntryImpl entityEntry = (EntityEntryImpl) managedEntity.$$_hibernate_getEntityEntry();
		if ( entityEntry == null ||
				!entityEntry.getPersister().isMutable() ||
				entityEntry.getPersistenceContext() == null ||
				entityEntry.getPersistenceContext() == persistenceContext ) {
			return;
		}
		if ( entityEntry.getPersistenceContext().getSession().isOpen() ) {
			// NOTE: otherPersistenceContext may be operating on the entityEntry in a different thread.
			//       it is not safe to associate entityEntry with this EntityEntryContext.
			throw new HibernateException(
					"Illegal attempt to associate a ManagedEntity with two open persistence contexts: " + entityEntry
			);
		}
		else {
			// otherPersistenceContext is associated with a closed PersistenceContext
			log.stalePersistenceContextInEntityEntry( entityEntry.toString() );
		}
	}

	/**
	 * Does this entity exist in this context, associated with an {@link EntityEntry}?
	 *
	 * @param entity The entity to check
	 *
	 * @return {@code true} if it is associated with this context
	 */
	public boolean hasEntityEntry(Object entity) {
		return getEntityEntry( entity ) != null;
	}

	/**
	 * Retrieve the associated {@link EntityEntry} for the given entity.
	 *
	 * @param entity The entity
	 *
	 * @return The associated {@link EntityEntry}
	 */
	public EntityEntry getEntityEntry(Object entity) {
		// locate a ManagedEntity for the entity, but only if it is associated with the same PersistenceContext.
		final ManagedEntity managedEntity = getAssociatedManagedEntity( entity );
		// and get/return the EntityEntry from the ManagedEntry
		return managedEntity == null
				? null
				: managedEntity.$$_hibernate_getEntityEntry();
	}

	/**
	 * Remove an entity from the context, returning its {@link EntityEntry}.
	 *
	 * @param entity The entity to remove
	 *
	 * @return The removed {@link EntityEntry}
	 */
	public EntityEntry removeEntityEntry(Object entity) {
		// locate a ManagedEntity for the entity, but only if it is associated with the same PersistenceContext.
		// no need to check if the entity is a ManagedEntity that is associated with a different PersistenceContext
		final ManagedEntity managedEntity = getAssociatedManagedEntity( entity );
		if ( managedEntity == null ) {
			// not associated with this EntityEntryContext, so nothing to do.
			return null;
		}

		dirty = true;

		if ( managedEntity instanceof ImmutableManagedEntityHolder holder ) {
			assert entity == holder.managedEntity;
			if ( !isReferenceCachingEnabled( holder.$$_hibernate_getEntityEntry().getPersister() ) ) {
				immutableManagedEntityXref.remove( managedEntity.$$_hibernate_getInstanceId(), entity );
			}
			else {
				nonEnhancedEntityXref.remove( entity );
			}
		}
		else if ( !isManagedEntity( entity ) ) {
			nonEnhancedEntityXref.remove( entity );
		}

		// re-link
		count--;

		if ( count == 0 ) {
			// handle as a special case...
			head = null;
			tail = null;

			assert managedEntity.$$_hibernate_getPreviousManagedEntity() == null;
			assert managedEntity.$$_hibernate_getNextManagedEntity() == null;
		}
		else {
			// otherwise, previous or next (or both) should be non-null
			final ManagedEntity previous = managedEntity.$$_hibernate_getPreviousManagedEntity();
			final ManagedEntity next = managedEntity.$$_hibernate_getNextManagedEntity();
			if ( previous == null ) {
				// we are removing head
				assert managedEntity == head;
				head = next;
			}
			else {
				previous.$$_hibernate_setNextManagedEntity( next );
			}

			if ( next == null ) {
				// we are removing tail
				assert managedEntity == tail;
				tail = previous;
			}
			else {
				next.$$_hibernate_setPreviousManagedEntity( previous );
			}
		}

		// finally clean out the ManagedEntity and return the associated EntityEntry
		return clearManagedEntity( managedEntity );
	}

	/**
	 * The main bugaboo with {@code IdentityMap} that warranted this class in the
	 * first place.
	 * <p>
	 * Return an array of all the entity/{@link EntityEntry} pairs in this context.
	 * The array is to make sure that the iterators built off of it are safe from
	 * concurrency/reentrancy.
	 *
	 * @return The safe array
	 */
	public Map.Entry<Object, EntityEntry>[] reentrantSafeEntityEntries() {
		if ( dirty ) {
			reentrantSafeEntries = new EntityEntryCrossRefImpl[count];
			int i = 0;
			ManagedEntity managedEntity = head;
			while ( managedEntity != null ) {
				reentrantSafeEntries[i++] = new EntityEntryCrossRefImpl(
						managedEntity.$$_hibernate_getEntityInstance(),
						managedEntity.$$_hibernate_getEntityEntry()
				);
				managedEntity = managedEntity.$$_hibernate_getNextManagedEntity();
			}
			dirty = false;
		}
		return reentrantSafeEntries;
	}

	private void processEachManagedEntity(final Consumer<ManagedEntity> action) {
		ManagedEntity node = head;
		while ( node != null ) {
			final ManagedEntity next = node.$$_hibernate_getNextManagedEntity();
			action.accept( node );
			node = next;
		}
	}

	// Could have used #processEachManagedEntity but avoided because of measurable overhead.
	// Careful, this needs to be very efficient as we potentially iterate quite a bit!
	// Also: we perform two operations at once, so to not iterate on the list twice;
	// being a linked list, multiple iterations are not cache friendly at all.
	private void clearAllReferencesFromManagedEntities() {
		ManagedEntity nextManagedEntity = head;
		while ( nextManagedEntity != null ) {
			final ManagedEntity current = nextManagedEntity;
			nextManagedEntity = current.$$_hibernate_getNextManagedEntity();
			Object toProcess = current.$$_hibernate_getEntityInstance();
			unsetSession( asPersistentAttributeInterceptableOrNull( toProcess ) );
			clearManagedEntity( current ); //careful this also unlinks from the "next" entry in the list
		}
	}

	private static void unsetSession(PersistentAttributeInterceptable persistentAttributeInterceptable) {
		if ( persistentAttributeInterceptable != null ) {
			final PersistentAttributeInterceptor interceptor = persistentAttributeInterceptable.$$_hibernate_getInterceptor();
			if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
				( (LazyAttributeLoadingInterceptor) interceptor ).unsetSession();
			}
		}
	}

	/**
	 * Clear this context of all managed entities.
	 */
	public void clear() {
		dirty = true;

		clearAllReferencesFromManagedEntities();

		if ( immutableManagedEntityXref != null ) {
			immutableManagedEntityXref.clear();
		}

		if ( nonEnhancedEntityXref != null ) {
			nonEnhancedEntityXref.clear();
		}

		head = null;
		tail = null;
		count = 0;

		reentrantSafeEntries = null;
		currentInstanceId = 1;
	}

	/**
	 * Resets the persistence information in a managed entity, and returns its previous {@link EntityEntry}
	 *
	 * @param node the managed entity to clear
	 * @return the previous {@link EntityEntry} contained in the managed entity
	 */
	private static EntityEntry clearManagedEntity(final ManagedEntity node) {
		return node.$$_hibernate_setPersistenceInfo( null, null, null, 0 );
	}

	/**
	 * Down-grade locks to {@link LockMode#NONE} for all entities in this context
	 */
	public void downgradeLocks() {
		processEachManagedEntity( EntityEntryContext::downgradeLockOnManagedEntity );
	}

	private static void downgradeLockOnManagedEntity(final ManagedEntity node) {
		final EntityEntry entityEntry = node.$$_hibernate_getEntityEntry();
		if ( entityEntry != null ) {
			entityEntry.setLockMode( LockMode.NONE );
		}
	}

	/**
	 * JDK serialization hook for serializing
	 *
	 * @param oos The stream to write ourselves to
	 *
	 * @throws IOException Indicates an IO exception accessing the given stream
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		log.tracef( "Starting serialization of [%s] EntityEntry entries", count );
		oos.writeInt( count );
		if ( count == 0 ) {
			return;
		}

		ManagedEntity managedEntity = head;
		while ( managedEntity != null ) {
			// so we know whether or not to build a ManagedEntityImpl on deserialize
			oos.writeBoolean( managedEntity == managedEntity.$$_hibernate_getEntityInstance() );
			oos.writeObject( managedEntity.$$_hibernate_getEntityInstance() );
			// we need to know which implementation of EntityEntry is being serialized
			oos.writeInt( managedEntity.$$_hibernate_getEntityEntry().getClass().getName().length() );
			oos.writeChars( managedEntity.$$_hibernate_getEntityEntry().getClass().getName() );
			managedEntity.$$_hibernate_getEntityEntry().serialize( oos );

			managedEntity = managedEntity.$$_hibernate_getNextManagedEntity();
		}
	}

	/**
	 * JDK serialization hook for deserializing
	 *
	 * @param ois The stream to read ourselves from
	 * @param rtn The persistence context we belong to
	 *
	 * @return The deserialized EntityEntryContext
	 *
	 * @throws IOException Indicates an IO exception accessing the given stream
	 * @throws ClassNotFoundException Problem reading stream data
	 */
	public static EntityEntryContext deserialize(ObjectInputStream ois, StatefulPersistenceContext rtn)
			throws IOException, ClassNotFoundException {
		final int count = ois.readInt();
		log.tracef( "Starting deserialization of [%s] EntityEntry entries", count );

		final EntityEntryContext context = new EntityEntryContext( rtn );
		context.count = count;
		context.dirty = true;

		if ( count == 0 ) {
			return context;
		}

		ManagedEntity previous = null;

		for ( int i = 0; i < count; i++ ) {
			final boolean isEnhanced = ois.readBoolean();
			final Object entity = ois.readObject();

			//Call deserialize method dynamically via reflection
			final int numChars = ois.readInt();
			final char[] entityEntryClassNameArr = new char[numChars];
			for ( int j = 0; j < numChars; j++ ) {
				entityEntryClassNameArr[j] = ois.readChar();
			}

			final EntityEntry entry = deserializeEntityEntry( entityEntryClassNameArr, ois, rtn );

			final int instanceId = context.nextManagedEntityInstanceId();
			final ManagedEntity managedEntity;
			if ( isEnhanced ) {
				final ManagedEntity castedEntity = asManagedEntity( entity );
				if ( entry.getPersister().isMutable() ) {
					managedEntity = castedEntity;
				}
				else {
					managedEntity = new ImmutableManagedEntityHolder( castedEntity );
					if ( !isReferenceCachingEnabled( entry.getPersister() ) ) {
						context.putImmutableManagedEntity( castedEntity, instanceId, (ImmutableManagedEntityHolder) managedEntity );
					}
					else {
						context.putManagedEntity( entity, castedEntity );
					}
				}
			}
			else {
				managedEntity = new ManagedEntityImpl( entity );
				context.putManagedEntity( entity, managedEntity );
			}

			if ( previous == null ) {
				context.head = managedEntity;
			}
			else {
				previous.$$_hibernate_setNextManagedEntity( managedEntity );
			}

			managedEntity.$$_hibernate_setPersistenceInfo( entry, previous, null, instanceId );

			previous = managedEntity;
		}

		context.tail = previous;

		return context;
	}

	private static EntityEntry deserializeEntityEntry(
			char[] entityEntryClassNames, ObjectInputStream ois, StatefulPersistenceContext persistenceContext){
		EntityEntry entry = null;

		final String entityEntryClassName = new String( entityEntryClassNames );
		final Class<?> entityEntryClass =
				persistenceContext.getSession().getFactory().getClassLoaderService()
						.classForName( entityEntryClassName );

		try {
			final Method deserializeMethod =
					entityEntryClass.getDeclaredMethod( "deserialize", ObjectInputStream.class, PersistenceContext.class );
			entry = (EntityEntry) deserializeMethod.invoke( null, ois, persistenceContext );
		}
		catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			log.errorf( "Enable to deserialize [%s]", entityEntryClassName );
		}

		return entry;

	}

	public int getNumberOfManagedEntities() {
		return count;
	}

	/**
	 * The wrapper for entity classes which do not implement {@link ManagedEntity}.
	 */
	private static class ManagedEntityImpl implements ManagedEntity {
		private final Object entityInstance;
		private EntityEntry entityEntry;
		private ManagedEntity previous;
		private ManagedEntity next;
		private boolean useTracker;

		public ManagedEntityImpl(Object entityInstance) {
			this.entityInstance = entityInstance;
			useTracker = false;
		}

		@Override
		public Object $$_hibernate_getEntityInstance() {
			return entityInstance;
		}

		@Override
		public EntityEntry $$_hibernate_getEntityEntry() {
			return entityEntry;
		}

		@Override
		public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {
			this.entityEntry = entityEntry;
		}

		@Override
		public ManagedEntity $$_hibernate_getNextManagedEntity() {
			return next;
		}

		@Override
		public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {
			this.next = next;
		}

		@Override
		public void $$_hibernate_setUseTracker(boolean useTracker) {
			this.useTracker = useTracker;
		}

		@Override
		public boolean $$_hibernate_useTracker() {
			return useTracker;
		}

		@Override
		public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
			return previous;
		}

		@Override
		public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
			this.previous = previous;
		}

		@Override
		public int $$_hibernate_getInstanceId() {
			return 0;
		}

		@Override
		public void $$_hibernate_setInstanceId(int id) {
		}

		@Override
		public EntityEntry $$_hibernate_setPersistenceInfo(EntityEntry entityEntry, ManagedEntity previous, ManagedEntity next, int instanceId) {
			final EntityEntry oldEntry = this.entityEntry;
			this.entityEntry = entityEntry;
			this.previous = previous;
			this.next = next;
			return oldEntry;
		}
	}

	private static class ImmutableManagedEntityHolder implements ManagedEntity {
		private final ManagedEntity managedEntity;
		private ManagedEntity previous;
		private ManagedEntity next;

		public ImmutableManagedEntityHolder(ManagedEntity immutableManagedEntity) {
			this.managedEntity = immutableManagedEntity;
		}

		@Override
		public Object $$_hibernate_getEntityInstance() {
			return managedEntity.$$_hibernate_getEntityInstance();
		}

		@Override
		public EntityEntry $$_hibernate_getEntityEntry() {
			return managedEntity.$$_hibernate_getEntityEntry();
		}

		@Override
		public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {
			// need to think about implications for memory leaks here if we don't removed reference to EntityEntry
			if ( entityEntry == null ) {
				if ( canClearEntityEntryReference( managedEntity.$$_hibernate_getEntityEntry() ) ) {
					managedEntity.$$_hibernate_setEntityEntry( null );
				}
				// otherwise, do nothing.
			}
			else {
				// TODO: we may want to do something different here if
				// managedEntity is in the process of being deleted.
				// An immutable ManagedEntity can be associated with
				// multiple PersistenceContexts. Changing the status
				// to DELETED probably should not happen directly
				// in the ManagedEntity because that would affect all
				// PersistenceContexts to which the ManagedEntity is
				// associated.
				managedEntity.$$_hibernate_setEntityEntry( entityEntry );
			}
		}

		@Override
		public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
			// previous reference cannot be stored in an immutable ManagedEntity;
			// previous reference is maintained by this ManagedEntityHolder.
			return previous;
		}

		@Override
		public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
			// previous reference cannot be stored in an immutable ManagedEntity;
			// previous reference is maintained by this ManagedEntityHolder.
			this.previous = previous;
		}

		@Override
		public ManagedEntity $$_hibernate_getNextManagedEntity() {
			// next reference cannot be stored in an immutable ManagedEntity;
			// next reference is maintained by this ManagedEntityHolder.
			return next;
		}

		@Override
		public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {
			// next reference cannot be stored in an immutable ManagedEntity;
			// next reference is maintained by this ManagedEntityHolder.
			this.next = next;
		}

		@Override
		public void $$_hibernate_setUseTracker(boolean useTracker) {
			managedEntity.$$_hibernate_setUseTracker( useTracker );
		}

		@Override
		public boolean $$_hibernate_useTracker() {
			return managedEntity.$$_hibernate_useTracker();
		}

		// Check instance type of EntityEntry and if type is ImmutableEntityEntry,
		// check to see if entity is referenced cached in the second level cache
		private static boolean canClearEntityEntryReference(EntityEntry entityEntry) {
			final EntityPersister persister = entityEntry.getPersister();
			return persister.isMutable() || !isReferenceCachingEnabled( persister );
		}

		@Override
		public int $$_hibernate_getInstanceId() {
			return managedEntity.$$_hibernate_getInstanceId();
		}

		@Override
		public void $$_hibernate_setInstanceId(int id) {
			managedEntity.$$_hibernate_setInstanceId( id );
		}

		@Override
		public EntityEntry $$_hibernate_setPersistenceInfo(EntityEntry entityEntry, ManagedEntity previous, ManagedEntity next, int instanceId) {
			final EntityEntry oldEntry;
			if ( entityEntry == null ) {
				oldEntry = managedEntity.$$_hibernate_getEntityEntry();
				if ( canClearEntityEntryReference( oldEntry ) ) {
					managedEntity.$$_hibernate_setEntityEntry( null );
				}
			}
			else {
				managedEntity.$$_hibernate_setEntityEntry( entityEntry );
				oldEntry = null; // no need to retrieve the previous entity entry
			}
			this.previous = previous;
			this.next = next;
			managedEntity.$$_hibernate_setInstanceId( instanceId );
			return oldEntry;
		}
	}

	/**
	 * Used in building the {@link #reentrantSafeEntityEntries()} entries
	 */
	public interface EntityEntryCrossRef extends Map.Entry<Object,EntityEntry> {
		/**
		 * The entity
		 *
		 * @return The entity
		 */
		Object getEntity();

		/**
		 * The associated EntityEntry
		 *
		 * @return The EntityEntry associated with the entity in this context
		 */
		EntityEntry getEntityEntry();
	}

	/**
	 * Implementation of the EntityEntryCrossRef interface
	 */
	private static class EntityEntryCrossRefImpl implements EntityEntryCrossRef {
		private final Object entity;
		private EntityEntry entityEntry;

		private EntityEntryCrossRefImpl(Object entity, EntityEntry entityEntry) {
			this.entity = entity;
			this.entityEntry = entityEntry;
		}

		@Override
		public Object getEntity() {
			return entity;
		}

		@Override
		public EntityEntry getEntityEntry() {
			return entityEntry;
		}

		@Override
		public Object getKey() {
			return getEntity();
		}

		@Override
		public EntityEntry getValue() {
			return getEntityEntry();
		}

		@Override
		public EntityEntry setValue(EntityEntry entityEntry) {
			final EntityEntry old = this.entityEntry;
			this.entityEntry = entityEntry;
			return old;
		}
	}
}
