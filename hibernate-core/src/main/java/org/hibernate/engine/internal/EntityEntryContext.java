/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Defines a context for maintaining the relation between an entity associated with the Session ultimately owning this
 * EntityEntryContext instance and that entity's corresponding EntityEntry.  2 approaches are supported:<ul>
 *     <li>
 *         the entity->EntityEntry association is maintained in a Map within this class
 *     </li>
 *     <li>
 *         the EntityEntry is injected into the entity via it implementing the {@link org.hibernate.engine.spi.ManagedEntity} contract,
 *         either directly or through bytecode enhancement.
 *     </li>
 * </ul>
 * <p/>
 *
 * @author Steve Ebersole
 */
public class EntityEntryContext {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( EntityEntryContext.class );

	private transient PersistenceContext persistenceContext;

	private transient IdentityHashMap<ManagedEntity,ImmutableManagedEntityHolder> immutableManagedEntityXref;

	private transient ManagedEntity head;
	private transient ManagedEntity tail;
	private transient int count;

	private transient IdentityHashMap<Object,ManagedEntity> nonEnhancedEntityXref;

	@SuppressWarnings( {"unchecked"})
	private transient Map.Entry<Object,EntityEntry>[] reentrantSafeEntries = new Map.Entry[0];
	private transient boolean dirty;

	/**
	 * Constructs a EntityEntryContext
	 */
	public EntityEntryContext(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Adds the entity and entry to this context, associating them together
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

		assert AbstractEntityEntry.class.isInstance( entityEntry );

		// We only need to check a mutable EntityEntry is associated with the same PersistenceContext.
		// Immutable EntityEntry can be associated with multiple PersistenceContexts, so no need to check.
		// ImmutableEntityEntry#getPersistenceContext() throws an exception (HHH-10251).
		if ( entityEntry.getPersister().isMutable() ) {
			assert AbstractEntityEntry.class.cast( entityEntry ).getPersistenceContext() == persistenceContext;
		}

		// Determine the appropriate ManagedEntity instance to use based on whether the entity is enhanced or not.
		// Throw an exception if entity is a mutable ManagedEntity that is associated with a different
		// PersistenceContext.
		ManagedEntity managedEntity = getAssociatedManagedEntity( entity );
		final boolean alreadyAssociated = managedEntity != null;
		if ( !alreadyAssociated ) {
			if ( ManagedEntity.class.isInstance( entity ) ) {
				if ( entityEntry.getPersister().isMutable() ) {
					managedEntity = (ManagedEntity) entity;
					// We know that managedEntity is not associated with the same PersistenceContext.
					// Check if managedEntity is associated with a different PersistenceContext.
					checkNotAssociatedWithOtherPersistenceContextIfMutable( managedEntity );
				}
				else {
					// Create a holder for PersistenceContext-related data.
					managedEntity = new ImmutableManagedEntityHolder( (ManagedEntity) entity );
					if ( immutableManagedEntityXref == null ) {
						immutableManagedEntityXref = new IdentityHashMap<ManagedEntity, ImmutableManagedEntityHolder>();
					}
					immutableManagedEntityXref.put(
							(ManagedEntity) entity,
							(ImmutableManagedEntityHolder) managedEntity
					);
				}
			}
			else {
				if ( nonEnhancedEntityXref == null ) {
					nonEnhancedEntityXref = new IdentityHashMap<Object, ManagedEntity>();
				}
				managedEntity = new ManagedEntityImpl( entity );
				nonEnhancedEntityXref.put( entity, managedEntity );
			}
		}

		// associate the EntityEntry with the entity
		managedEntity.$$_hibernate_setEntityEntry( entityEntry );

		if ( alreadyAssociated ) {
			// if the entity was already associated with the context, skip the linking step.
			return;
		}

		// TODO: can dirty be set to true here?

		// finally, set up linking and count
		if ( tail == null ) {
			assert head == null;
			// Protect against stale data in the ManagedEntity and nullify previous/next references.
			managedEntity.$$_hibernate_setPreviousManagedEntity( null );
			managedEntity.$$_hibernate_setNextManagedEntity( null );
			head = managedEntity;
			tail = head;
			count = 1;
		}
		else {
			tail.$$_hibernate_setNextManagedEntity( managedEntity );
			managedEntity.$$_hibernate_setPreviousManagedEntity( tail );
			// Protect against stale data left in the ManagedEntity nullify next reference.
			managedEntity.$$_hibernate_setNextManagedEntity( null );
			tail = managedEntity;
			count++;
		}
	}

	private ManagedEntity getAssociatedManagedEntity(Object entity) {
		if ( ManagedEntity.class.isInstance( entity ) ) {
			final ManagedEntity managedEntity = (ManagedEntity) entity;
			if ( managedEntity.$$_hibernate_getEntityEntry() == null ) {
				// it is not associated
				return null;
			}
			final AbstractEntityEntry entityEntry = (AbstractEntityEntry) managedEntity.$$_hibernate_getEntityEntry();

			if ( entityEntry.getPersister().isMutable() ) {
				return entityEntry.getPersistenceContext() == persistenceContext
						? managedEntity // it is associated
						: null;
			}
			else {
				// if managedEntity is associated with this EntityEntryContext, then
				// it will have an entry in immutableManagedEntityXref and its
				// holder will be returned.
				return immutableManagedEntityXref != null
						? immutableManagedEntityXref.get( managedEntity )
						: null;
			}
		}
		else {
			return nonEnhancedEntityXref != null
					? nonEnhancedEntityXref.get( entity )
					: null;
		}
	}

	private void checkNotAssociatedWithOtherPersistenceContextIfMutable(ManagedEntity managedEntity) {
		// we only have to check mutable managedEntity
		final AbstractEntityEntry entityEntry = (AbstractEntityEntry) managedEntity.$$_hibernate_getEntityEntry();
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
					"Illegal attempt to associate a ManagedEntity with two open persistence contexts. " + entityEntry
			);
		}
		else {
			// otherPersistenceContext is associated with a closed PersistenceContext
			log.stalePersistenceContextInEntityEntry( entityEntry.toString() );
		}
	}

	/**
	 * Does this entity exist in this context, associated with an EntityEntry?
	 *
	 * @param entity The entity to check
	 *
	 * @return {@code true} if it is associated with this context
	 */
	public boolean hasEntityEntry(Object entity) {
		return getEntityEntry( entity ) != null;
	}

	/**
	 * Retrieve the associated EntityEntry for the entity
	 *
	 * @param entity The entity to retrieve the EntityEntry for
	 *
	 * @return The associated EntityEntry
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
	 * Remove an entity from the context, returning the EntityEntry which was associated with it
	 *
	 * @param entity The entity to remove
	 *
	 * @return Tjee EntityEntry
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

		if ( ImmutableManagedEntityHolder.class.isInstance( managedEntity ) ) {
			assert entity == ( (ImmutableManagedEntityHolder) managedEntity ).managedEntity;
			immutableManagedEntityXref.remove( (ManagedEntity) entity );

		}
		else if ( !ManagedEntity.class.isInstance( entity ) ) {
			nonEnhancedEntityXref.remove( entity );
		}

		// prepare for re-linking...
		final ManagedEntity previous = managedEntity.$$_hibernate_getPreviousManagedEntity();
		final ManagedEntity next = managedEntity.$$_hibernate_getNextManagedEntity();
		managedEntity.$$_hibernate_setPreviousManagedEntity( null );
		managedEntity.$$_hibernate_setNextManagedEntity( null );

		// re-link
		count--;

		if ( count == 0 ) {
			// handle as a special case...
			head = null;
			tail = null;

			assert previous == null;
			assert next == null;
		}
		else {
			// otherwise, previous or next (or both) should be non-null
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
		final EntityEntry theEntityEntry = managedEntity.$$_hibernate_getEntityEntry();
		managedEntity.$$_hibernate_setEntityEntry( null );
		return theEntityEntry;
	}

	/**
	 * The main bugaboo with IdentityMap that warranted this class in the first place.
	 *
	 * Return an array of all the entity/EntityEntry pairs in this context.  The array is to make sure
	 * that the iterators built off of it are safe from concurrency/reentrancy
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

	/**
	 * Clear this context of all managed entities
	 */
	public void clear() {
		dirty = true;

		ManagedEntity node = head;
		while ( node != null ) {
			final ManagedEntity nextNode = node.$$_hibernate_getNextManagedEntity();

			node.$$_hibernate_setEntityEntry( null );

			node.$$_hibernate_setPreviousManagedEntity( null );
			node.$$_hibernate_setNextManagedEntity( null );

			node = nextNode;
		}

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
	}

	/**
	 * Down-grade locks to NONE for all entities in this context
	 */
	public void downgradeLocks() {
		if ( head == null ) {
			return;
		}

		ManagedEntity node = head;
		while ( node != null ) {
			node.$$_hibernate_getEntityEntry().setLockMode( LockMode.NONE );

			node = node.$$_hibernate_getNextManagedEntity();
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

			final ManagedEntity managedEntity;
			if ( isEnhanced ) {
				if ( entry.getPersister().isMutable() ) {
					managedEntity = (ManagedEntity) entity;
				}
				else {
					managedEntity = new ImmutableManagedEntityHolder( (ManagedEntity) entity );
					if ( context.immutableManagedEntityXref == null ) {
						context.immutableManagedEntityXref =
								new IdentityHashMap<ManagedEntity, ImmutableManagedEntityHolder>();
					}
					context.immutableManagedEntityXref.put(
							(ManagedEntity) entity,
							(ImmutableManagedEntityHolder) managedEntity

					);
				}
			}
			else {
				managedEntity = new ManagedEntityImpl( entity );
				if ( context.nonEnhancedEntityXref == null ) {
					context.nonEnhancedEntityXref = new IdentityHashMap<Object, ManagedEntity>();
				}
				context.nonEnhancedEntityXref.put( entity, managedEntity );
			}
			managedEntity.$$_hibernate_setEntityEntry( entry );

			if ( previous == null ) {
				context.head = managedEntity;
			}
			else {
				previous.$$_hibernate_setNextManagedEntity( managedEntity );
				managedEntity.$$_hibernate_setPreviousManagedEntity( previous );
			}

			previous = managedEntity;
		}

		context.tail = previous;

		return context;
	}

	private static EntityEntry deserializeEntityEntry(char[] entityEntryClassNameArr, ObjectInputStream ois, StatefulPersistenceContext rtn){
		EntityEntry entry = null;

		final String entityEntryClassName = new String( entityEntryClassNameArr );
		final Class entityEntryClass =   rtn.getSession().getFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityEntryClassName );

		try {
			final Method deserializeMethod = entityEntryClass.getDeclaredMethod( "deserialize", ObjectInputStream.class,	PersistenceContext.class );
			entry = (EntityEntry) deserializeMethod.invoke( null, ois, rtn );
		}
		catch (NoSuchMethodException e) {
			log.errorf( "Enable to deserialize [%s]", entityEntryClassName );
		}
		catch (InvocationTargetException e) {
			log.errorf( "Enable to deserialize [%s]", entityEntryClassName );
		}
		catch (IllegalAccessException e) {
			log.errorf( "Enable to deserialize [%s]", entityEntryClassName );
		}

		return entry;

	}

	public int getNumberOfManagedEntities() {
		return count;
	}

	/**
	 * The wrapper for entity classes which do not implement ManagedEntity
	 */
	private static class ManagedEntityImpl implements ManagedEntity {
		private final Object entityInstance;
		private EntityEntry entityEntry;
		private ManagedEntity previous;
		private ManagedEntity next;

		public ManagedEntityImpl(Object entityInstance) {
			this.entityInstance = entityInstance;
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
		public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
			return previous;
		}

		@Override
		public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
			this.previous = previous;
		}
	}

	private static class ImmutableManagedEntityHolder implements ManagedEntity {
		private ManagedEntity managedEntity;
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
				if ( canClearEntityEntryReference() ) {
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

		/*
		Check instance type of EntityEntry and if type is ImmutableEntityEntry, check to see if entity is referenced cached in the second level cache
		 */
		private boolean canClearEntityEntryReference(){

			if( managedEntity.$$_hibernate_getEntityEntry() == null ) {
				return true;
			}

			if( !(managedEntity.$$_hibernate_getEntityEntry() instanceof ImmutableEntityEntry) ) {
				return true;
			}
			else if( managedEntity.$$_hibernate_getEntityEntry().getPersister().canUseReferenceCacheEntries() ) {
				return false;
			}

			return true;

		}
	}

	/**
	 * Used in building the {@link #reentrantSafeEntityEntries()} entries
	 */
	public static interface EntityEntryCrossRef extends Map.Entry<Object,EntityEntry> {
		/**
		 * The entity
		 *
		 * @return The entity
		 */
		public Object getEntity();

		/**
		 * The associated EntityEntry
		 *
		 * @return The EntityEntry associated with the entity in this context
		 */
		public EntityEntry getEntityEntry();
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
