/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;

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
	private static final Logger log = Logger.getLogger( EntityEntryContext.class );

	private transient ManagedEntity head;
	private transient ManagedEntity tail;
	private transient int count = 0;

	private transient IdentityHashMap<Object,ManagedEntity> nonEnhancedEntityXref;

	@SuppressWarnings( {"unchecked"})
	private transient Map.Entry<Object,EntityEntry>[] reentrantSafeEntries = new Map.Entry[0];
	private transient boolean dirty = false;

	public EntityEntryContext() {
	}

	public void addEntityEntry(Object entity, EntityEntry entityEntry) {
		// IMPORTANT!!!!!
		//		add is called more than once of some entities.  In such cases the first
		//		call is simply setting up a "marker" to avoid infinite looping from reentrancy
		//
		// any addition (even the double one described above) should invalidate the cross-ref array
		dirty = true;

		// determine the appropriate ManagedEntity instance to use based on whether the entity is enhanced or not.
		// also track whether the entity was already associated with the context
		final ManagedEntity managedEntity;
		final boolean alreadyAssociated;
		if ( ManagedEntity.class.isInstance( entity ) ) {
			managedEntity = (ManagedEntity) entity;
			alreadyAssociated = managedEntity.$$_hibernate_getEntityEntry() != null;
		}
		else {
			ManagedEntity wrapper = null;
			if ( nonEnhancedEntityXref == null ) {
				nonEnhancedEntityXref = new IdentityHashMap<Object, ManagedEntity>();
			}
			else {
				wrapper = nonEnhancedEntityXref.get( entity );
			}

			if ( wrapper == null ) {
				wrapper = new ManagedEntityImpl( entity );
				nonEnhancedEntityXref.put( entity, wrapper );
				alreadyAssociated = false;
			}
			else {
				alreadyAssociated = true;
			}

			managedEntity = wrapper;
		}

		// associate the EntityEntry with the entity
		managedEntity.$$_hibernate_setEntityEntry( entityEntry );

		if ( alreadyAssociated ) {
			// if the entity was already associated with the context, skip the linking step.
			return;
		}

		// finally, set up linking and count
		if ( tail == null ) {
			assert head == null;
			head = managedEntity;
			tail = head;
			count = 1;
		}
		else {
			tail.$$_hibernate_setNextManagedEntity( managedEntity );
			managedEntity.$$_hibernate_setPreviousManagedEntity( tail );
			tail = managedEntity;
			count++;
		}
	}

	public boolean hasEntityEntry(Object entity) {
		return getEntityEntry( entity ) != null;
	}

	public EntityEntry getEntityEntry(Object entity) {
		final ManagedEntity managedEntity;
		if ( ManagedEntity.class.isInstance( entity ) ) {
			managedEntity = (ManagedEntity) entity;
		}
		else if ( nonEnhancedEntityXref == null ) {
			managedEntity = null;
		}
		else {
			managedEntity = nonEnhancedEntityXref.get( entity );
		}

		return managedEntity == null
				? null
				: managedEntity.$$_hibernate_getEntityEntry();
	}

	public EntityEntry removeEntityEntry(Object entity) {
		dirty = true;

		final ManagedEntity managedEntity;
		if ( ManagedEntity.class.isInstance( entity ) ) {
			managedEntity = (ManagedEntity) entity;
		}
		else if ( nonEnhancedEntityXref == null ) {
			managedEntity = null;
		}
		else {
			managedEntity = nonEnhancedEntityXref.remove( entity );
		}

		if ( managedEntity == null ) {
			return null;
		}

		// prepare for re-linking...
		ManagedEntity previous = managedEntity.$$_hibernate_getPreviousManagedEntity();
		ManagedEntity next = managedEntity.$$_hibernate_getNextManagedEntity();
		managedEntity.$$_hibernate_setPreviousManagedEntity( null );
		managedEntity.$$_hibernate_setNextManagedEntity( null );

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

		EntityEntry theEntityEntry = managedEntity.$$_hibernate_getEntityEntry();
		managedEntity.$$_hibernate_setEntityEntry( null );
		return theEntityEntry;
	}

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

		if ( nonEnhancedEntityXref != null ) {
			nonEnhancedEntityXref.clear();
		}

		head = null;
		tail = null;
		count = 0;

		reentrantSafeEntries = null;
	}

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
			managedEntity.$$_hibernate_getEntityEntry().serialize( oos );

			managedEntity = managedEntity.$$_hibernate_getNextManagedEntity();
		}
	}

	public static EntityEntryContext deserialize(ObjectInputStream ois, StatefulPersistenceContext rtn) throws IOException, ClassNotFoundException {
		final int count = ois.readInt();
		log.tracef( "Starting deserialization of [%s] EntityEntry entries", count );

		final EntityEntryContext context = new EntityEntryContext();
		context.count = count;
		context.dirty = true;

		if ( count == 0 ) {
			return context;
		}

		ManagedEntity previous = null;

		for ( int i = 0; i < count; i++ ) {
			final boolean isEnhanced = ois.readBoolean();
			final Object entity = ois.readObject();
			final EntityEntry entry = EntityEntry.deserialize( ois, rtn );
			final ManagedEntity managedEntity;
			if ( isEnhanced ) {
				managedEntity = (ManagedEntity) entity;
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

	public int getNumberOfManagedEntities() {
		return count;
	}

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

	public static interface EntityEntryCrossRef extends Map.Entry<Object,EntityEntry> {
		public Object getEntity();
		public EntityEntry getEntityEntry();
	}
}
