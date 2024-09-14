/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockOptions;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;

/**
 * Base support for load-by-natural-id
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

	public Object with(RootGraph<T> graph, GraphSemantic semantic) {
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

	protected void performAnyNeededCrossReferenceSynchronizations() {
		if ( !synchronizationEnabled ) {
			// synchronization (this process) was disabled
			return;
		}

		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( !naturalIdMapping.isMutable() ) {
			// only mutable natural-ids need this processing
			return;
		}

		final SessionImplementor session = context.getSession();

		if ( ! session.isTransactionInProgress() ) {
			// not in a transaction so skip synchronization
			return;
		}

		final PersistenceContext persistenceContext = context.getSession().getPersistenceContextInternal();
		final Collection<?> cachedPkResolutions =
				persistenceContext.getNaturalIdResolutions()
						.getCachedPkResolutions( entityPersister() );
		final boolean loggerDebugEnabled = LoaderLogging.LOADER_LOGGER.isDebugEnabled();
		for ( Object pk : cachedPkResolutions ) {
			final EntityKey entityKey = context.getSession().generateEntityKey( pk, entityPersister() );
			final Object entity = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( entity );

			if ( entry == null ) {
				if ( loggerDebugEnabled ) {
					LoaderLogging.LOADER_LOGGER.debugf(
							"Cached natural-id/pk resolution linked to null EntityEntry in persistence context : %s#%s",
							entityDescriptor.getEntityName(),
							pk
					);
				}
				continue;
			}

			if ( !entry.requiresDirtyCheck( entity ) ) {
				continue;
			}

			// MANAGED is the only status we care about here...
			if ( entry.getStatus() != Status.MANAGED ) {
				continue;
			}

			persistenceContext.getNaturalIdResolutions().handleSynchronization( pk, entity, entityPersister() );
		}
	}

	@SuppressWarnings( "unchecked" )
	protected final T doGetReference(Object normalizedNaturalIdValue) {
		performAnyNeededCrossReferenceSynchronizations();

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		final SessionImplementor session = context.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Object cachedResolution =
				persistenceContext.getNaturalIdResolutions()
						.findCachedIdByNaturalId( normalizedNaturalIdValue, entityPersister() );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			// the entity is deleted, although not yet flushed - return null
			return null;
		}
		else {
			if ( cachedResolution != null ) {
				return (T) getIdentifierLoadAccess().getReference( cachedResolution );
			}
			else {
				LoaderLogging.LOADER_LOGGER.debugf(
						"Selecting entity identifier by natural-id for `#getReference` handling - %s : %s",
						entityPersister().getEntityName(),
						normalizedNaturalIdValue
				);
				final Object idFromDatabase =
						entityPersister().getNaturalIdLoader()
								.resolveNaturalIdToId( normalizedNaturalIdValue, session );
				return idFromDatabase == null ? null : (T) getIdentifierLoadAccess().getReference( idFromDatabase );
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected final T doLoad(Object normalizedNaturalIdValue) {
		performAnyNeededCrossReferenceSynchronizations();

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		final SessionImplementor session = context.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Object cachedResolution =
				persistenceContext.getNaturalIdResolutions()
						.findCachedIdByNaturalId( normalizedNaturalIdValue, entityPersister() );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}
		else {
			final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
			final HashSet<String> fetchProfiles =
					influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final EffectiveEntityGraph effectiveEntityGraph =
					session.getLoadQueryInfluencers().applyEntityGraph( rootGraph, graphSemantic);
			try {
				final T loaded = cachedResolution != null
						? (T) getIdentifierLoadAccess().load(cachedResolution)
						: (T) entityPersister().getNaturalIdLoader().load( normalizedNaturalIdValue, this, session );
				if ( loaded != null ) {
					final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( loaded );
					final EntityEntry entry = lazyInitializer != null
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

	protected final IdentifierLoadAccess<?> getIdentifierLoadAccess() {
		final IdentifierLoadAccessImpl<?> loadAccess = new IdentifierLoadAccessImpl<>( context, entityPersister() );
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
