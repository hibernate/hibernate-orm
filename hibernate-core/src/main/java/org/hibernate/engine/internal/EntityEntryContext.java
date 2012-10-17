/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.LockMode;
import org.hibernate.engine.ManagedEntity;
import org.hibernate.engine.spi.EntityEntry;

/**
 * Defines a context for maintaining the relation between an entity associated with the Session ultimately owning this
 * EntityEntryContext instance and that entity's corresponding EntityEntry.  2 approaches are supported:<ul>
 *     <li>
 *         the entity->EntityEntry association is maintained in a Map within this class
 *     </li>
 *     <li>
 *         the EntityEntry is injected into the entity via it implementing the {@link org.hibernate.engine.ManagedEntity} contract,
 *         either directly or through bytecode enhancement.
 *     </li>
 * </ul>
 * <p/>
 * IMPL NOTE : This current implementation is not ideal in the {@link org.hibernate.engine.ManagedEntity} case.  The problem is that
 * the 'backingMap' is still the means to maintain ordering of the entries; but in the {@link org.hibernate.engine.ManagedEntity} case
 * the use of a Map is overkill, and double here so because of the need for wrapping the map keys.  But this is just
 * a quick prototype.
 *
 * @author Steve Ebersole
 */
public class EntityEntryContext {
	private static final Logger log = Logger.getLogger( EntityEntryContext.class );

	private LinkedHashMap<KeyWrapper,EntityEntry> backingMap = new LinkedHashMap<KeyWrapper, EntityEntry>();

	@SuppressWarnings( {"unchecked"})
	private transient Map.Entry<Object,EntityEntry>[] reentrantSafeEntries = new Map.Entry[0];
	private transient boolean dirty = false;

	public EntityEntryContext() {
	}

	/**
	 * Private constructor used during deserialization
	 *
	 * @param backingMap The backing map re-built from the serial stream
	 */
	private EntityEntryContext(LinkedHashMap<KeyWrapper, EntityEntry> backingMap) {
		this.backingMap = backingMap;
		// mark dirty so we can rebuild the 'reentrantSafeEntries'
		dirty = true;
	}

	public void clear() {
		dirty = true;
		for ( Map.Entry<KeyWrapper,EntityEntry> mapEntry : backingMap.entrySet() ) {
			final Object realKey = mapEntry.getKey().getRealKey();
			if ( ManagedEntity.class.isInstance( realKey ) ) {
				( (ManagedEntity) realKey ).hibernate_setEntityEntry( null );
			}
		}
		backingMap.clear();
		reentrantSafeEntries = null;
	}

	public void downgradeLocks() {
		// Downgrade locks
		for ( Map.Entry<KeyWrapper,EntityEntry> mapEntry : backingMap.entrySet() ) {
			final Object realKey = mapEntry.getKey().getRealKey();
			final EntityEntry entityEntry = ManagedEntity.class.isInstance( realKey )
					? ( (ManagedEntity) realKey ).hibernate_getEntityEntry()
					: mapEntry.getValue();
			entityEntry.setLockMode( LockMode.NONE );
		}
	}

	public boolean hasEntityEntry(Object entity) {
		return ManagedEntity.class.isInstance( entity )
				? ( (ManagedEntity) entity ).hibernate_getEntityEntry() == null
				: backingMap.containsKey( new KeyWrapper<Object>( entity ) );
	}

	public EntityEntry getEntityEntry(Object entity) {
		return ManagedEntity.class.isInstance( entity )
				? ( (ManagedEntity) entity ).hibernate_getEntityEntry()
				: backingMap.get( new KeyWrapper<Object>( entity ) );
	}

	public EntityEntry removeEntityEntry(Object entity) {
		dirty = true;
		if ( ManagedEntity.class.isInstance( entity ) ) {
			backingMap.remove( new KeyWrapper<Object>( entity ) );
			final EntityEntry entityEntry = ( (ManagedEntity) entity ).hibernate_getEntityEntry();
			( (ManagedEntity) entity ).hibernate_setEntityEntry( null );
			return entityEntry;
		}
		else {
			return backingMap.remove( new KeyWrapper<Object>( entity ) );
		}
	}

	public void addEntityEntry(Object entity, EntityEntry entityEntry) {
		dirty = true;
		if ( ManagedEntity.class.isInstance( entity ) ) {
			backingMap.put( new KeyWrapper<Object>( entity ), null );
			( (ManagedEntity) entity ).hibernate_setEntityEntry( entityEntry );
		}
		else {
			backingMap.put( new KeyWrapper<Object>( entity ), entityEntry );
		}
	}

	public Map.Entry<Object, EntityEntry>[] reentrantSafeEntityEntries() {
		if ( dirty ) {
			reentrantSafeEntries = new MapEntryImpl[ backingMap.size() ];
			int i = 0;
			for ( Map.Entry<KeyWrapper,EntityEntry> mapEntry : backingMap.entrySet() ) {
				final Object entity = mapEntry.getKey().getRealKey();
				final EntityEntry entityEntry = ManagedEntity.class.isInstance( entity )
						? ( (ManagedEntity) entity ).hibernate_getEntityEntry()
						: mapEntry.getValue();
				reentrantSafeEntries[i++] = new MapEntryImpl( entity, entityEntry );
			}
			dirty = false;
		}
		return reentrantSafeEntries;
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		final int count = backingMap.size();
		log.tracef( "Starting serialization of [%s] EntityEntry entries", count );
		oos.writeInt( count );
		for ( Map.Entry<KeyWrapper,EntityEntry> mapEntry : backingMap.entrySet() ) {
			oos.writeObject( mapEntry.getKey().getRealKey() );
			mapEntry.getValue().serialize( oos );
		}
	}

	public static EntityEntryContext deserialize(ObjectInputStream ois, StatefulPersistenceContext rtn) throws IOException, ClassNotFoundException {
		final int count = ois.readInt();
		log.tracef( "Starting deserialization of [%s] EntityEntry entries", count );
		final LinkedHashMap<KeyWrapper,EntityEntry> backingMap = new LinkedHashMap<KeyWrapper, EntityEntry>( count );
		for ( int i = 0; i < count; i++ ) {
			final Object entity = ois.readObject();
			final EntityEntry entry = EntityEntry.deserialize( ois, rtn );
			backingMap.put( new KeyWrapper<Object>( entity ), entry );
		}
		return new EntityEntryContext( backingMap );
	}

	/**
	 * @deprecated Added to support (also deprecated) PersistenceContext.getEntityEntries method until it can be removed.  Safe to use for counts.
	 *
	 */
	@Deprecated
	public Map getEntityEntryMap() {
		return backingMap;
	}

	/**
	 * We need to base the identity on {@link System#identityHashCode(Object)} but
	 * attempt to lazily initialize and cache this value: being a native invocation
	 * it is an expensive value to retrieve.
	 */
	public static final class KeyWrapper<K> implements Serializable {
		private final K realKey;
		private int hash = 0;

		KeyWrapper(K realKey) {
			this.realKey = realKey;
		}

		@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
		@Override
		public boolean equals(Object other) {
			return realKey == ( (KeyWrapper) other ).realKey;
		}

		@Override
		public int hashCode() {
			if ( this.hash == 0 ) {
				//We consider "zero" as non-initialized value
				final int newHash = System.identityHashCode( realKey );
				if ( newHash == 0 ) {
					//So make sure we don't store zeros as it would trigger initialization again:
					//any value is fine as long as we're deterministic.
					this.hash = -1;
				}
				else {
					this.hash = newHash;
				}
			}
			return hash;
		}

		@Override
		public String toString() {
			return realKey.toString();
		}

		public K getRealKey() {
			return realKey;
		}
	}

	private static class MapEntryImpl implements Map.Entry<Object,EntityEntry> {
		private final Object key;
		private EntityEntry value;

		private MapEntryImpl(Object key, EntityEntry value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public Object getKey() {
			return key;
		}

		@Override
		public EntityEntry getValue() {
			return value;
		}

		@Override
		public EntityEntry setValue(EntityEntry value) {
			final EntityEntry old = this.value;
			this.value = value;
			return value;
		}
	}
}
