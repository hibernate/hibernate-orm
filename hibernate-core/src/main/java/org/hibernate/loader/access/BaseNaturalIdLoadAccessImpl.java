package org.hibernate.loader.access;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.loader.ast.spi.NaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.spi.PersistenceContext.NaturalIdHelper.INVALID_NATURAL_ID_REFERENCE;

/**
 * @author Steve Ebersole
 */
public abstract class BaseNaturalIdLoadAccessImpl<T> implements NaturalIdLoadOptions {
	private final LoadAccessContext context;
	private final EntityMappingType entityDescriptor;

	private LockOptions lockOptions;
	private boolean synchronizationEnabled = true;

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

	protected final Object resolveNaturalId(Map<String, Object> naturalIdParameters) {
		performAnyNeededCrossReferenceSynchronizations();

		final Object resolvedId = entityPersister()
				.getNaturalIdLoader()
				.resolveNaturalIdToId( naturalIdParameters, context.getSession() );

		return resolvedId == INVALID_NATURAL_ID_REFERENCE
				? null
				: resolvedId;
	}

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
		for ( Object pk : persistenceContext.getNaturalIdHelper().getCachedPkResolutions( entityPersister() ) ) {
			final EntityKey entityKey = context.getSession().generateEntityKey( pk, entityPersister() );
			final Object entity = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( entity );

			if ( entry == null ) {
				if ( LoaderLogging.DEBUG_ENABLED ) {
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

			persistenceContext.getNaturalIdHelper().handleSynchronization(
					entityPersister(),
					pk,
					entity
			);
		}
	}

	@SuppressWarnings( "unchecked" )
	protected final T doGetReference(Object normalizedNaturalIdValue) {
		performAnyNeededCrossReferenceSynchronizations();

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		final SessionImplementor session = context.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Object cachedResolution = persistenceContext.getNaturalIdHelper().findCachedNaturalIdResolution(
				entityPersister(),
				normalizedNaturalIdValue
		);

		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			// the entity is deleted, although not yet flushed - return null
			return null;
		}

		if ( cachedResolution != null ) {
			return (T) getIdentifierLoadAccess().getReference( cachedResolution );
		}

		LoaderLogging.LOADER_LOGGER.debugf(
				"Selecting entity identifier by natural-id for `#getReference` handling - %s : %s",
				entityPersister().getEntityName(),
				normalizedNaturalIdValue
		);

		final Object idFromDatabase = entityPersister().getNaturalIdLoader().resolveNaturalIdToId( normalizedNaturalIdValue, session );
		if ( idFromDatabase != null ) {
			return (T) getIdentifierLoadAccess().getReference( idFromDatabase );
		}

		return null;
	}

	protected final T doLoad(Object normalizedNaturalIdValue) {
		performAnyNeededCrossReferenceSynchronizations();

		context.checkOpenOrWaitingForAutoClose();
		context.pulseTransactionCoordinator();

		final SessionImplementor session = context.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();

		final Object cachedResolution = persistenceContext.getNaturalIdHelper().findCachedNaturalIdResolution(
				entityPersister(),
				normalizedNaturalIdValue
		);

		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}

		try {
			final T loaded;

			if ( cachedResolution != null ) {
				loaded = (T) getIdentifierLoadAccess().load( cachedResolution );
			} else {
				loaded = (T) entityPersister().getNaturalIdLoader().load( normalizedNaturalIdValue, this, session );
			}

			if ( loaded != null ) {
				final EntityEntry entry = persistenceContext.getEntry( loaded );
				assert entry != null;
				if ( entry.getStatus() == Status.DELETED ) {
					return null;
				}
			}

			return loaded;
		}
		finally {
			context.delayedAfterCompletion();
		}
	}

	protected final IdentifierLoadAccess<?> getIdentifierLoadAccess() {
		final IdentifierLoadAccessImpl<?> identifierLoadAccess = new IdentifierLoadAccessImpl<>( context, entityPersister() );
		if ( this.lockOptions != null ) {
			identifierLoadAccess.with( lockOptions );
		}
		return identifierLoadAccess;
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
