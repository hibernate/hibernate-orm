/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;

import static java.util.Collections.emptySet;
import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isSelfDirtinessTracker;

/**
 * Interceptor that loads attributes lazily
 *
 * @author Luis Barreiro
 * @author Steve Ebersole
 */
public class LazyAttributeLoadingInterceptor
		extends AbstractInterceptor
		implements BytecodeLazyAttributeInterceptor {

	private final Object identifier;
	private EntityRelatedState entityMeta;
	private Set<String> initializedLazyFields;

	public LazyAttributeLoadingInterceptor(
			EntityRelatedState entityMeta,
			Object identifier,
			SharedSessionContractImplementor session) {
		this.identifier = identifier;
		this.entityMeta = entityMeta;
		setSession( session );
	}

	@Override
	public String getEntityName() {
		return entityMeta.entityName;
	}

	@Override
	public Object getIdentifier() {
		return identifier;
	}

	@Override
	protected Object handleRead(Object target, String attributeName, Object value) {
		if ( !isAttributeLoaded( attributeName ) ) {
			final Object loadedValue = fetchAttribute( target, attributeName );
			attributeInitialized( attributeName );
			return loadedValue;
		}
		return value;
	}

	@Override
	protected Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue) {
		attributeInitialized( attributeName );
		return newValue;
	}

	/**
	 * Fetches the lazy attribute. The attribute does not get associated with the entity. (To be used by hibernate methods)
	 */
	public Object fetchAttribute(final Object target, final String attributeName) {
		return loadAttribute( target, attributeName );
	}

	protected Object loadAttribute(final Object target, final String attributeName) {
		return EnhancementHelper.performWork(
				this,
				(session, isTemporarySession) -> {
					final var persister =
							session.getFactory().getMappingMetamodel()
									.getEntityDescriptor( getEntityName() );

					if ( isTemporarySession ) {
						final Object id = persister.getIdentifier( target, session );

						// Add an entry for this entity in the PC of the temp Session
						// NOTE : a few arguments that would be nice to pass along here...
						//		1) loadedState if we know any
						final Object[] loadedState = null;
						//		2) does a row exist in the db for this entity?
						final boolean existsInDb = true;
						session.getPersistenceContextInternal().addEntity(
								target,
								Status.READ_ONLY,
								loadedState,
								session.generateEntityKey( id, persister ),
								persister.getVersion( target ),
								LockMode.NONE,
								existsInDb,
								persister,
								true
						);
					}

					final var initializer = (LazyPropertyInitializer) persister;
					final Object loadedValue =
							initializer.initializeLazyProperty( attributeName, target, session );

					takeCollectionSizeSnapshot( target, attributeName, loadedValue );
					return loadedValue;
				},
				getEntityName(),
				attributeName
		);
	}

	public boolean isAttributeLoaded(String fieldName) {
		return !isLazyAttribute( fieldName ) || isInitializedLazyField( fieldName );
	}

	private boolean isLazyAttribute(String fieldName) {
		return entityMeta.lazyFields.contains( fieldName );
	}

	private boolean isInitializedLazyField(String fieldName) {
		return initializedLazyFields != null && initializedLazyFields.contains( fieldName );
	}

	public boolean hasAnyUninitializedAttributes() {
		if ( entityMeta.lazyFields.isEmpty() ) {
			return false;
		}
		else if ( initializedLazyFields == null ) {
			return true;
		}
		else {
			for ( String fieldName : entityMeta.lazyFields ) {
				if ( !initializedLazyFields.contains( fieldName ) ) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
			+ "(entityName=" + getEntityName() + " ,lazyFields=" + entityMeta.lazyFields + ')';
	}

	private void takeCollectionSizeSnapshot(Object target, String fieldName, Object value) {
		if ( value instanceof Collection<?> collection && isSelfDirtinessTracker( target ) ) {
			// This must be called first, so that we remember that there is a collection out there,
			// even if we don't know its size (see below).
			var tracker = getCollectionTracker( target );
			if ( value instanceof PersistentCollection<?> persistentCollection
					&& !persistentCollection.wasInitialized() ) {
				// Cannot take a snapshot of an uninitialized collection.
				return;
			}
			tracker.add( fieldName, collection.size() );
		}
	}

	private static CollectionTracker getCollectionTracker(Object target) {
		final var selfDirtinessTracker = asSelfDirtinessTracker( target );
		final var tracker = selfDirtinessTracker.$$_hibernate_getCollectionTracker();
		if ( tracker == null ) {
			selfDirtinessTracker.$$_hibernate_clearDirtyAttributes();
			return selfDirtinessTracker.$$_hibernate_getCollectionTracker();
		}
		else {
			return tracker;
		}
	}

	@Override
	public void attributeInitialized(String name) {
		if ( isLazyAttribute( name ) ) {
			if ( initializedLazyFields == null ) {
				initializedLazyFields = new HashSet<>();
			}
			initializedLazyFields.add( name );
		}
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return initializedLazyFields == null ? emptySet() : initializedLazyFields;
	}

	public void addLazyFieldByGraph(String fieldName) {
		if ( entityMeta.shared ) {
			//We need to make a defensive copy first to not affect other entities of the same type;
			//as we create a copy we lose some of the efficacy of using a separate class to track this state,
			//but this is a corner case so we prefer to optimise for the common case.
			entityMeta = entityMeta.toNonSharedMutableState();
		}
		entityMeta.lazyFields.add( fieldName );
	}

	public void clearInitializedLazyFields() {
		initializedLazyFields = null;
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		entityMeta.serialize( oos );
		if ( initializedLazyFields == null ) {
			oos.writeInt( -1 );
		}
		else {
			oos.writeInt( initializedLazyFields.size() );
			for ( String field : initializedLazyFields ) {
				oos.writeUTF( field );
			}
		}
	}

	public static LazyAttributeLoadingInterceptor deserialize(
			ObjectInputStream ois,
			EntityHolder holder,
			SharedSessionContractImplementor session) throws IOException {
		final EntityRelatedState entityMeta = EntityRelatedState.deserialize( ois, holder );
		final var interceptor = new LazyAttributeLoadingInterceptor(
				entityMeta, holder.getEntityKey().getIdentifier(), session );
		final int size = ois.readInt();
		for ( int i = 0; i < size; i++ ) {
			interceptor.attributeInitialized( ois.readUTF() );
		}
		return interceptor;
	}

	/**
	 * This is an helper object to group all state which relates to a particular entity type,
	 * and which is needed for this interceptor.
	 * Grouping such state allows for upfront construction as a per-entity singleton:
	 * this reduces processing work on creation of an interceptor instance and is more
	 * efficient from a point of view of memory usage and memory layout.
	 */
	public static class EntityRelatedState {
		private final String entityName;
		private final Set<String> lazyFields;
		private final boolean shared;

		public EntityRelatedState(String entityName, Set<String> lazyFields) {
			this.entityName = entityName;
			this.lazyFields = lazyFields; //N.B. this is an immutable, compact set
			this.shared = true;
		}
		private EntityRelatedState(String entityName, Set<String> lazyFields, boolean shared) {
			this.entityName = entityName;
			this.lazyFields = lazyFields;
			this.shared = shared;
		}
		private EntityRelatedState toNonSharedMutableState() {
			return new EntityRelatedState( entityName, new HashSet<>( lazyFields ), false );
		}

		private void serialize(ObjectOutputStream oos) throws IOException {
			if ( shared ) {
				oos.writeInt( -1 );
			}
			else {
				oos.writeInt( lazyFields.size() );
				for ( String field : lazyFields ) {
					oos.writeUTF( field );
				}
			}
		}

		private static EntityRelatedState deserialize(ObjectInputStream ois, EntityHolder holder) throws IOException {
			final var persister = holder.getDescriptor();
			final String entityName = persister.getEntityName();
			final int size = ois.readInt();
			if ( size == -1 ) {
				return new EntityRelatedState(
						entityName,
						persister.getBytecodeEnhancementMetadata()
								.getLazyAttributesMetadata().getLazyAttributeNames() );
			}
			else {
				final Set<String> lazyFields = new HashSet<>( size );
				for ( int i = 0; i < size; i++ ) {
					lazyFields.add( ois.readUTF() );
				}
				return new EntityRelatedState( entityName, lazyFields, false );
			}
		}
	}
}
