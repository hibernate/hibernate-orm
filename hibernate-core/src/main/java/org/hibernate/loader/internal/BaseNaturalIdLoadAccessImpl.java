/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;

/**
 * Base support for loading by {@linkplain org.hibernate.annotations.NaturalId natural id}.
 *
 * @author Steve Ebersole
 */
public abstract class BaseNaturalIdLoadAccessImpl<T> implements NaturalIdLoadOptions {
	private final LoadAccessContext context;
	private final EntityMappingType entityDescriptor;

	private LockOptions lockOptions;
	private boolean synchronizationEnabled = true;

	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	protected BaseNaturalIdLoadAccessImpl(LoadAccessContext context, EntityMappingType entityDescriptor) {
		this.context = context;
		this.entityDescriptor = entityDescriptor;

		if ( entityDescriptor.getNaturalIdMapping() == null ) {
			throw new HibernateException(
					String.format( "Entity [%s] did not define a natural id", entityDescriptor.getEntityName() )
			);
		}
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	protected Object with(LockMode lockMode, PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}


	protected Object with(Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	public Object with(EntityGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	public Object enableFetchProfile(String profileName) {
		if ( !context.getSession().getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	public Object disableFetchProfile(String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	public boolean isSynchronizationEnabled() {
		return synchronizationEnabled;
	}

	public BaseNaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	protected void synchronizationEnabled(boolean synchronizationEnabled) {
		this.synchronizationEnabled = synchronizationEnabled;
	}
//
//	protected final Object resolveNaturalId(Map<String, Object> naturalIdParameters) {
//		performAnyNeededCrossReferenceSynchronizations();
//
//		final Object resolvedId = entityPersister()
//				.getNaturalIdLoader()
//				.resolveNaturalIdToId( naturalIdParameters, context.getSession() );
//
//		return resolvedId == INVALID_NATURAL_ID_REFERENCE
//				? null
//				: resolvedId;
//	}

	private Object getCachedResolution(Object normalizedNaturalIdValue) {
		final SessionImplementor session = context.getSession();

		performAnyNeededCrossReferenceSynchronizations( synchronizationEnabled, entityDescriptor, session );

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		return session.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.findCachedIdByNaturalId( normalizedNaturalIdValue, entityPersister() );
	}

	protected final T doGetReference(Object normalizedNaturalIdValue) {
		final Object cachedResolution = getCachedResolution( normalizedNaturalIdValue );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			// the entity is deleted, although not yet flushed - return null
			return null;
		}
		else if ( cachedResolution != null ) {
			return identifierLoadAccess().getReference( cachedResolution );
		}
		else {
			final Object idFromDatabase =
					entityPersister().getNaturalIdLoader()
							.resolveNaturalIdToId( normalizedNaturalIdValue, context.getSession() );
			return idFromDatabase == null ? null : identifierLoadAccess().getReference( idFromDatabase );
		}
	}

	protected final T doLoad(Object normalizedNaturalIdValue) {
		final Object cachedResolution = getCachedResolution( normalizedNaturalIdValue );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}
		else {
			final var session = context.getSession();
			final var influencers = session.getLoadQueryInfluencers();
			final var fetchProfiles = influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final var effectiveEntityGraph = influencers.applyEntityGraph( rootGraph, graphSemantic );
			try {
				@SuppressWarnings("unchecked")
				final T loaded = cachedResolution != null
						? identifierLoadAccess().load(cachedResolution)
						: (T) entityPersister().getNaturalIdLoader()
								.load( normalizedNaturalIdValue, this, session );
				if ( loaded != null ) {
					final var persistenceContext = session.getPersistenceContextInternal();
					final var lazyInitializer = HibernateProxy.extractLazyInitializer( loaded );
					final var entry = lazyInitializer != null
							? persistenceContext.getEntry( lazyInitializer.getImplementation() )
							: persistenceContext.getEntry( loaded );
					assert entry != null;
					if ( entry.getStatus() == Status.DELETED ) {
						return null;
					}
				}
				return loaded;
			}
			finally {
				context.delayedAfterCompletion();
				effectiveEntityGraph.clear();
				influencers.setEnabledFetchProfileNames( fetchProfiles );
			}
		}
	}

	protected final IdentifierLoadAccess<T> identifierLoadAccess() {
		final IdentifierLoadAccessImpl<T> loadAccess =
				new IdentifierLoadAccessImpl<>( context, entityPersister() );
		if ( lockOptions != null ) {
			loadAccess.with( lockOptions );
		}
		return loadAccess;
	}

	protected LoadAccessContext getContext() {
		return context;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	protected EntityPersister entityPersister() {
		return entityDescriptor.getEntityPersister();
	}
}
