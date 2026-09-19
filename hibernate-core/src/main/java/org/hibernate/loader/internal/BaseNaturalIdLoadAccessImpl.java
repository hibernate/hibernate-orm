/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
import org.hibernate.internal.find.StatefulLoadAccessContext;
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
	private final StatefulLoadAccessContext context;
	private final EntityMappingType entityDescriptor;

	@Nullable
	private LockOptions lockOptions;
	private boolean synchronizationEnabled = true;

	@Nullable
	private Set<String> enabledFetchProfiles;
	@Nullable
	private Set<String> disabledFetchProfiles;

	@Nullable
	private RootGraphImplementor<T> rootGraph;
	@Nullable
	private GraphSemantic graphSemantic;

	protected BaseNaturalIdLoadAccessImpl(@Nonnull StatefulLoadAccessContext context, @Nonnull EntityMappingType entityDescriptor) {
		this.context = context;
		this.entityDescriptor = entityDescriptor;

		if ( entityDescriptor.getNaturalIdMapping() == null ) {
			throw new HibernateException(
					String.format( "Entity [%s] did not define a natural id", entityDescriptor.getEntityName() )
			);
		}
	}

	@Nullable
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Nonnull
	protected Object with(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	@Nonnull
	protected Object with(@Nonnull PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setScope( lockScope );
		return this;
	}


	@Nonnull
	protected Object with(@Nonnull Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	@Nonnull
	public Object with(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Nonnull
	public Object enableFetchProfile(@Nonnull String profileName) {
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

	@Nonnull
	public Object disableFetchProfile(@Nonnull String profileName) {
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

	@Nonnull
	public BaseNaturalIdLoadAccessImpl<T> with(@Nonnull LockOptions lockOptions) {
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

	@Nonnull
	private Object getCachedResolution(@Nullable Object normalizedNaturalIdValue) {
		final SessionImplementor session = context.getSession();

		performAnyNeededCrossReferenceSynchronizations( synchronizationEnabled, entityDescriptor, session );

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		return session.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.findCachedIdByNaturalId( normalizedNaturalIdValue, entityPersister() );
	}

	@Nullable
	protected final T doGetReference(@Nullable Object normalizedNaturalIdValue) {
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

	@Nullable
	protected final T doLoad(@Nullable Object normalizedNaturalIdValue) {
		final Object cachedResolution = getCachedResolution( normalizedNaturalIdValue );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}
		else {
			final var session = context.getSession();
			final var influencers = session.getLoadQueryInfluencers();
			final var fetchProfiles = influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final var effectiveEntityGraph =
					rootGraph == null
							? null
							: influencers.applyEntityGraph( rootGraph, castNonNull( graphSemantic ) );
			try {
				@SuppressWarnings("unchecked")
				final T loaded = cachedResolution != null
						? identifierLoadAccess().load( cachedResolution )
						: (T) entityPersister().getNaturalIdLoader()
								.load( normalizedNaturalIdValue, this, session );
				if ( loaded != null ) {
					final var persistenceContext = session.getPersistenceContextInternal();
					final var lazyInitializer = HibernateProxy.extractLazyInitializer( loaded );
					final var entity = lazyInitializer != null ? lazyInitializer.getImplementation() : loaded;
					final var entry = persistenceContext.getEntry( entity );
					assert entry != null;
					if ( entry.getStatus() == Status.DELETED ) {
						return null;
					}
				}
				return loaded;
			}
			finally {
				context.delayedAfterCompletion();
				if ( effectiveEntityGraph != null ) {
					effectiveEntityGraph.clear();
				}
				influencers.setEnabledFetchProfileNames( fetchProfiles );
			}
		}
	}

	@Nonnull
	protected final IdentifierLoadAccess<T> identifierLoadAccess() {
		final IdentifierLoadAccessImpl<T> loadAccess =
				new IdentifierLoadAccessImpl<>( context, entityPersister() );
		if ( lockOptions != null ) {
			loadAccess.with( lockOptions );
		}
		return loadAccess;
	}

	@Nonnull
	protected StatefulLoadAccessContext getContext() {
		return context;
	}

	@Nonnull
	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	@Nonnull
	protected EntityPersister entityPersister() {
		return entityDescriptor.getEntityPersister();
	}
}
