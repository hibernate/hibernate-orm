/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.bytecode.BytecodeLogging;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class EnhancementAsProxyLazinessInterceptor extends AbstractLazyLoadInterceptor {
	private final Set<String> identifierAttributeNames;
	private final CompositeType nonAggregatedCidMapper;

	private final EntityKey entityKey;

	private final boolean inLineDirtyChecking;
	private Set<String> writtenFieldNames;
	private Set<String> collectionAttributeNames;

	private Status status;

	private final boolean initializeBeforeWrite;

	public EnhancementAsProxyLazinessInterceptor(
			String entityName,
			Set<String> identifierAttributeNames,
			CompositeType nonAggregatedCidMapper,
			EntityKey entityKey,
			SharedSessionContractImplementor session) {
		super( entityName, session );

		this.identifierAttributeNames = identifierAttributeNames;
		assert identifierAttributeNames != null;

		this.nonAggregatedCidMapper = nonAggregatedCidMapper;
		assert nonAggregatedCidMapper != null || identifierAttributeNames.size() == 1;

		this.entityKey = entityKey;

		final EntityPersister entityPersister = session.getFactory().getMetamodel().entityPersister( entityName );
		if ( entityPersister.hasCollections() ) {
			Type[] propertyTypes = entityPersister.getPropertyTypes();
			collectionAttributeNames = new HashSet<>();
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				Type propertyType = propertyTypes[i];
				if ( propertyType.isCollectionType() ) {
					collectionAttributeNames.add( entityPersister.getPropertyNames()[i] );
				}
			}
		}

		this.inLineDirtyChecking = entityPersister.getEntityMode() == EntityMode.POJO
				&& SelfDirtinessTracker.class.isAssignableFrom( entityPersister.getMappedClass() );
		// if self-dirty tracking is enabled but DynamicUpdate is not enabled then we need to initialise the entity
		// 	because the pre-computed update statement contains even not dirty properties and so we need all the values
		initializeBeforeWrite = !( inLineDirtyChecking && entityPersister.getEntityMetamodel().isDynamicUpdate() );
		status = Status.UNINITIALIZED;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	protected Object handleRead(Object target, String attributeName, Object value) {
		// it is illegal for this interceptor to still be attached to the entity after initialization
		if ( isInitialized() ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		// the attribute being read is an entity-id attribute
		// 		- we already know the id, return that
		if ( identifierAttributeNames.contains( attributeName ) ) {
			return extractIdValue( target, attributeName );
		}

		// Use `performWork` to group together multiple Session accesses
		return EnhancementHelper.performWork(
				this,
				(session, isTempSession) -> {
					final Object[] writtenValues;

					final EntityPersister entityPersister = session.getFactory()
							.getMetamodel()
							.entityPersister( getEntityName() );
					final EntityTuplizer entityTuplizer = entityPersister.getEntityTuplizer();

					if ( writtenFieldNames != null && !writtenFieldNames.isEmpty() ) {

						// enhancement has dirty-tracking available and at least one attribute was explicitly set

						if ( writtenFieldNames.contains( attributeName ) ) {
							// the requested attribute was one of the attributes explicitly set, we can just return the explicitly set value
							return entityTuplizer.getPropertyValue( target, attributeName );
						}

						// otherwise we want to save all of the explicitly set values in anticipation of
						// 		the force initialization below so that we can "replay" them after the
						// 		initialization

						writtenValues = new Object[writtenFieldNames.size()];

						int index = 0;
						for ( String writtenFieldName : writtenFieldNames ) {
							writtenValues[index] = entityTuplizer.getPropertyValue( target, writtenFieldName );
							index++;
						}
					}
					else {
						writtenValues = null;
					}

					final Object initializedValue = forceInitialize(
							target,
							attributeName,
							session,
							isTempSession
					);

					setInitialized();

					if ( writtenValues != null ) {
						// here is the replaying of the explicitly set values we prepared above
						int index = 0;
						for ( String writtenFieldName : writtenFieldNames ) {
							entityTuplizer.setPropertyValue( target, writtenFieldName, writtenValues[index++] );
						}
						writtenFieldNames.clear();
					}

					return initializedValue;
				},
				getEntityName(),
				attributeName
		);
	}

	private Object extractIdValue(Object target, String attributeName) {
		// access to the id or part of it for non-aggregated cid
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
		BytecodeLogging.LOGGER.tracef(
				"EnhancementAsProxyLazinessInterceptor#forceInitialize : %s#%s -> %s )",
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				attributeName
		);

		return EnhancementHelper.performWork(
				this,
				(session, isTemporarySession) -> forceInitialize( target, attributeName, session, isTemporarySession ),
				getEntityName(),
				attributeName
		);
	}

	public Object forceInitialize(Object target, String attributeName, SharedSessionContractImplementor session, boolean isTemporarySession) {
		BytecodeLogging.LOGGER.tracef(
				"EnhancementAsProxyLazinessInterceptor#forceInitialize : %s#%s -> %s )",
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				attributeName
		);

		final EntityPersister persister = session.getFactory()
				.getMetamodel()
				.entityPersister( getEntityName() );

		if ( isTemporarySession ) {
			// Add an entry for this entity in the PC of the temp Session
			session.getPersistenceContextInternal().addEntity(
					target,
					org.hibernate.engine.spi.Status.READ_ONLY,
					// loaded state
					ArrayHelper.filledArray(
							LazyPropertyInitializer.UNFETCHED_PROPERTY,
							Object.class,
							persister.getPropertyTypes().length
					),
					entityKey,
					persister.getVersion( target ),
					LockMode.NONE,
					// we assume an entry exists in the db
					true,
					persister,
					true
			);
		}

		return persister.initializeEnhancedEntityUsedAsProxy(
				target,
				attributeName,
				session
		);
	}

	@Override
	protected Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue) {
		if ( isInitialized() ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		if ( identifierAttributeNames.contains( attributeName ) ) {
			// it is illegal for the identifier value to be changed.  Normally Hibernate
			// validates this during flush.  However, here it is dangerous to just allow the
			// new value to be set and continue on waiting for the flush for validation
			// because this interceptor manages the entity's entry in the PC itself.  So
			// just do the check here up-front
			final boolean changed;
			if ( nonAggregatedCidMapper == null ) {
				changed = ! entityKey.getPersister().getIdentifierType().isEqual( oldValue, newValue );
			}
			else {
				final int subAttrIndex = nonAggregatedCidMapper.getPropertyIndex( attributeName );
				final Type subAttrType = nonAggregatedCidMapper.getSubtypes()[subAttrIndex];
				changed = ! subAttrType.isEqual( oldValue, newValue );
			}

			if ( changed ) {
				throw new HibernateException(
						"identifier of an instance of " + entityKey.getEntityName() + " was altered from " + oldValue + " to " + newValue
				);
			}

			// otherwise, setId has been called but passing in the same value - just pass it through
			return newValue;
		}

		if ( initializeBeforeWrite
				|| ( collectionAttributeNames != null && collectionAttributeNames.contains( attributeName ) ) ) {
			// we need to force-initialize the proxy - the fetch group to which the `attributeName` belongs
			try {
				forceInitialize( target, attributeName );
			}
			finally {
				setInitialized();
			}

			if ( inLineDirtyChecking ) {
				( (SelfDirtinessTracker) target ).$$_hibernate_trackChange( attributeName );
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

			( (SelfDirtinessTracker) target ).$$_hibernate_trackChange( attributeName );
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
		return identifierAttributeNames.contains( fieldName );
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
		return writtenFieldNames != null && writtenFieldNames.size() != 0;
	}

	private enum Status {
		UNINITIALIZED,
		INITIALIZING,
		INITIALIZED
	}
}
