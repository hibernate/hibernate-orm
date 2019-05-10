/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Collections;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.bytecode.BytecodeLogger;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EnhancementAsProxyLazinessInterceptor extends AbstractLazyLoadInterceptor {
	private final Set<String> identifierAttributeNames;
	private final CompositeType nonAggregatedCidMapper;

	private final EntityKey entityKey;

	private boolean initialized;

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
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	protected Object handleRead(Object target, String attributeName, Object value) {
		if ( initialized ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		if ( identifierAttributeNames.contains( attributeName ) ) {
			return extractIdValue( target, attributeName );
		}

		try {
			return forceInitialize( target, attributeName );
		}
		finally {
			initialized = true;
		}
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
		BytecodeLogger.LOGGER.tracef(
				"EnhancementAsProxyLazinessInterceptor#forceInitialize : %s#%s -> %s )",
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				attributeName
		);

		return EnhancementHelper.performWork(
				this,
				(session, isTemporarySession) -> {
					final EntityPersister persister = session.getFactory()
							.getMetamodel()
							.entityPersister( getEntityName() );

					if ( isTemporarySession ) {
						// Add an entry for this entity in the PC of the temp Session
						// NOTE : a few arguments that would be nice to pass along here...
						//		1) loadedState if we know any - since this is an uninitialized "proxy",
						//		all attributes are not yet fetched
						final Object[] loadedState = ArrayHelper.filledArray(
								LazyPropertyInitializer.UNFETCHED_PROPERTY,
								Object.class,
								persister.getPropertyTypes().length
						);
						//		2) does a row exist in the db for this entity?
						final boolean existsInDb = true;
						session.getPersistenceContext().addEntity(
								target,
								Status.READ_ONLY,
								loadedState,
								entityKey,
								persister.getVersion( target ),
								LockMode.NONE,
								existsInDb,
								persister,
								true
						);
					}

					return persister.initializeEnhancedEntityUsedAsProxy(
							target,
							attributeName,
							session
					);
				},
				getEntityName(),
				attributeName
		);
	}

	@Override
	protected Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue) {
		if ( initialized ) {
			throw new IllegalStateException( "EnhancementAsProxyLazinessInterceptor interception on an initialized instance" );
		}

		// skip initialization on call to identifier attribute setter
		if ( ! identifierAttributeNames.contains( attributeName ) ) {
			try {
				forceInitialize( target, attributeName );
			}
			finally {
				initialized = true;
			}
		}

		return newValue;
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return Collections.emptySet();
	}

	@Override
	public void attributeInitialized(String name) {
		if ( initialized ) {
			throw new UnsupportedOperationException( "Expected call to EnhancementAsProxyLazinessInterceptor#attributeInitialized" );
		}
	}

	@Override
	public Object getIdentifier() {
		return entityKey.getIdentifier();
	}
}
