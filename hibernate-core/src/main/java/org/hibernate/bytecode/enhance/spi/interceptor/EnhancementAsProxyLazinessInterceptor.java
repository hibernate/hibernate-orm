/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static java.util.Collections.unmodifiableSet;
import static org.hibernate.bytecode.enhance.spi.interceptor.BytecodeInterceptorLogging.BYTECODE_INTERCEPTOR_LOGGER;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isSelfDirtinessTrackerType;
import static org.hibernate.internal.util.collections.CollectionHelper.toSmallSet;

/**
 * @author Steve Ebersole
 */
public class EnhancementAsProxyLazinessInterceptor extends AbstractInterceptor implements BytecodeLazyAttributeInterceptor {

	private final EntityKey entityKey;
	private final EntityRelatedState meta;
	private Set<String> writtenFieldNames;
	private Status status;

	public EnhancementAsProxyLazinessInterceptor(
			EntityRelatedState meta,
			EntityKey entityKey,
			SharedSessionContractImplementor session) {
		this.entityKey = entityKey;
		this.meta = meta;
		status = Status.UNINITIALIZED;
		setSession( session );
	}

	@Override
	public String getEntityName() {
		return meta.entityName;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	protected Object handleRead(Object target, String attributeName, Object value) {
		// it's illegal for this interceptor to still be attached to the entity after initialization
		if ( isInitialized() ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		// the attribute being read is an entity-id attribute
		// 		- we already know the id, return that
		if ( meta.identifierAttributeNames.contains( attributeName ) ) {
			return extractIdValue( target, attributeName );
		}

		// Use `performWork` to group together multiple Session accesses
		return EnhancementHelper.performWork(
				this,
				(session, isTempSession) -> read( target, attributeName, session, isTempSession ),
				getEntityName(),
				attributeName
		);
	}

	private Object read(
			Object target, String attributeName, SharedSessionContractImplementor session, Boolean isTempSession) {
		final Object[] writtenAttributeValues;
		final AttributeMapping[] writtenAttributeMappings;

		if ( writtenFieldNames != null && !writtenFieldNames.isEmpty() ) {

			// enhancement has dirty-tracking available and at least one attribute was explicitly set
			final EntityPersister entityPersister = meta.persister;
			if ( writtenFieldNames.contains( attributeName ) ) {
				// the requested attribute was one of the attributes explicitly set,
				// we can just return the explicitly-set value
				return entityPersister.getPropertyValue( target, attributeName );
			}

			// otherwise we want to save all the explicitly-set values in anticipation of
			// 		the force initialization below so that we can "replay" them after the
			// 		initialization

			writtenAttributeValues = new Object[writtenFieldNames.size()];
			writtenAttributeMappings = new AttributeMapping[writtenFieldNames.size()];

			int index = 0;
			for ( String writtenFieldName : writtenFieldNames ) {
				writtenAttributeMappings[index] = entityPersister.findAttributeMapping( writtenFieldName );
				writtenAttributeValues[index] = writtenAttributeMappings[index].getValue(target);
				index++;
			}
		}
		else {
			writtenAttributeValues = null;
			writtenAttributeMappings = null;
		}

		final Object initializedValue = forceInitialize( target, attributeName, session, isTempSession );

		setInitialized();

		if ( writtenAttributeValues != null ) {
			// here is the replaying of the explicitly set values we prepared above
			for ( int i = 0; i < writtenAttributeMappings.length; i++ ) {
				final AttributeMapping attribute = writtenAttributeMappings[i];
				attribute.setValue(target, writtenAttributeValues[i] );
				if ( meta.inLineDirtyChecking ) {
					asSelfDirtinessTracker(target).$$_hibernate_trackChange( attribute.getAttributeName() );
				}
			}
			writtenFieldNames.clear();
		}

		return initializedValue;
	}

	private Object extractIdValue(Object target, String attributeName) {
		// access to the id or part of it for non-aggregated cid
		final CompositeType nonAggregatedCidMapper = meta.nonAggregatedCidMapper;
		if ( nonAggregatedCidMapper == null ) {
			return getIdentifier();
		}
		else {
			return nonAggregatedCidMapper.getPropertyValue(
					target,
					nonAggregatedCidMapper.getPropertyIndex( attributeName ),
					getLinkedSession()
			);
		}
	}

	public Object forceInitialize(Object target, String attributeName) {
		if ( BYTECODE_INTERCEPTOR_LOGGER.isTraceEnabled() ) {
			BYTECODE_INTERCEPTOR_LOGGER.enhancementAsProxyLazinessForceInitialize(
					entityKey.getEntityName(),
					entityKey.getIdentifier(),
					attributeName
			);
		}

		return EnhancementHelper.performWork(
				this,
				(session, isTemporarySession) -> forceInitialize( target, attributeName, session, isTemporarySession ),
				getEntityName(),
				attributeName
		);
	}

	public Object forceInitialize(
			Object target,
			String attributeName,
			SharedSessionContractImplementor session,
			boolean isTemporarySession) {
		if ( BYTECODE_INTERCEPTOR_LOGGER.isTraceEnabled() ) {
			BYTECODE_INTERCEPTOR_LOGGER.enhancementAsProxyLazinessForceInitialize(
					entityKey.getEntityName(),
					entityKey.getIdentifier(),
					attributeName
			);
		}

		if ( isTemporarySession ) {
			// Add an entry for this entity in the PC of the temp Session
			session.getPersistenceContext()
					.addEnhancedProxy( entityKey, asPersistentAttributeInterceptable( target ) );
		}

		return meta.persister.initializeEnhancedEntityUsedAsProxy( target, attributeName, session );
	}

	@Override
	protected Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue) {
		if ( isInitialized() ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		if ( meta.identifierAttributeNames.contains( attributeName ) ) {
			// it is illegal for the identifier value to be changed.  Normally Hibernate
			// validates this during flush.  However, here it's dangerous to just allow the
			// new value to be set and continue on waiting for the flush for validation
			// because this interceptor manages the entity's entry in the PC itself.  So
			// just do the check here up-front
			final boolean changed;
			if ( meta.nonAggregatedCidMapper == null ) {
				changed = ! entityKey.getPersister().getIdentifierType().isEqual( oldValue, newValue );
			}
			else {
				final int subAttrIndex = meta.nonAggregatedCidMapper.getPropertyIndex( attributeName );
				final Type subAttrType = meta.nonAggregatedCidMapper.getSubtypes()[subAttrIndex];
				changed = ! subAttrType.isEqual( oldValue, newValue );
			}

			if ( changed ) {
				throw new HibernateException( "identifier of an instance of " + entityKey.getEntityName()
						+ " was altered from " + oldValue + " to " + newValue );
			}

			// otherwise, setId has been called but passing in the same value - just pass it through
			return newValue;
		}

		if ( meta.initializeBeforeWrite
				|| meta.collectionAttributeNames != null && meta.collectionAttributeNames.contains( attributeName ) ) {
			// we need to force-initialize the proxy - the fetch group to which the `attributeName` belongs
			try {
				forceInitialize( target, attributeName );
			}
			finally {
				setInitialized();
			}

			if ( meta.inLineDirtyChecking ) {
				asSelfDirtinessTracker( target ).$$_hibernate_trackChange( attributeName );
			}
		}
		else {
			// because of the entity being enhanced with `org.hibernate.engine.spi.SelfDirtinessTracker`
			// we can skip forcing the initialization.  However,  in the case of a subsequent read we
			// need to know which attributes had been explicitly set so that we can re-play the setters
			// after the force-initialization there
			if ( writtenFieldNames == null ) {
				writtenFieldNames = new HashSet<>();
			}
			writtenFieldNames.add( attributeName );

			asSelfDirtinessTracker( target ).$$_hibernate_trackChange( attributeName );
		}

		return newValue;
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return Collections.emptySet();
	}

	@Override
	public void attributeInitialized(String name) {
		if ( status == Status.INITIALIZED ) {
			throw new UnsupportedOperationException( "Expected call to EnhancementAsProxyLazinessInterceptor#attributeInitialized" );
		}
	}

	@Override
	public boolean isAttributeLoaded(String fieldName) {
		if ( isInitialized() ) {
			throw new UnsupportedOperationException( "Call to EnhancementAsProxyLazinessInterceptor#isAttributeLoaded on an interceptor which is marked as initialized" );
		}
		// Only fields from the identifier are loaded (until it's initialized)
		return meta.identifierAttributeNames.contains( fieldName );
	}

	@Override
	public boolean hasAnyUninitializedAttributes() {
		if ( isInitialized() ) {
			throw new UnsupportedOperationException( "Call to EnhancementAsProxyLazinessInterceptor#hasAnyUninitializedAttributes on an interceptor which is marked as initialized" );
		}
		return true;
	}

	@Override
	public Object getIdentifier() {
		return entityKey.getIdentifier();
	}

	public boolean isInitializing() {
		return status == Status.INITIALIZING;
	}

	public void setInitializing() {
		status = Status.INITIALIZING;
	}

	//Mostly useful for testing
	public boolean isInitialized() {
		return status == Status.INITIALIZED;
	}

	private void setInitialized() {
		status = Status.INITIALIZED;
	}

	public boolean hasWrittenFieldNames() {
		return writtenFieldNames != null && !writtenFieldNames.isEmpty();
	}

	private enum Status {
		UNINITIALIZED,
		INITIALIZING,
		INITIALIZED
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
		private final CompositeType nonAggregatedCidMapper;
		private final Set<String> identifierAttributeNames;
		private final Set<String> collectionAttributeNames;
		private final boolean initializeBeforeWrite;
		private final boolean inLineDirtyChecking;
		private final EntityPersister persister;

		public EntityRelatedState(EntityPersister persister,
								CompositeType nonAggregatedCidMapper,
								Set<String> identifierAttributeNames) {
			this.identifierAttributeNames = identifierAttributeNames;
			this.entityName = persister.getEntityName();
			this.nonAggregatedCidMapper = nonAggregatedCidMapper;
			this.persister = persister;
			assert nonAggregatedCidMapper != null || identifierAttributeNames.size() == 1;

			if ( persister.hasCollections() ) {
				final Set<String> tmpCollectionAttributeNames = new HashSet<>();
				final Type[] propertyTypes = persister.getPropertyTypes();
				final String[] propertyNames = persister.getPropertyNames();
				for ( int i = 0; i < propertyTypes.length; i++ ) {
					Type propertyType = propertyTypes[i];
					if ( propertyType instanceof CollectionType ) {
						tmpCollectionAttributeNames.add( propertyNames[i] );
					}
				}
				this.collectionAttributeNames = toSmallSet( unmodifiableSet( tmpCollectionAttributeNames ) );
			}
			else {
				this.collectionAttributeNames = Collections.emptySet();
			}

			this.inLineDirtyChecking = isSelfDirtinessTrackerType( persister.getMappedClass() );
			// if self-dirty tracking is enabled but DynamicUpdate is not enabled then we need to
			// initialize the entity because the precomputed update statement contains even not
			// dirty properties. And so we need all the values we have to initialize. Or, if it's
			// versioned, we need to fetch the current version.
			this.initializeBeforeWrite =
					!inLineDirtyChecking
						|| !persister.isDynamicUpdate()
						|| persister.isVersioned();
		}
	}
}
