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
package org.hibernate.loader.plan.exec.process.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.exec.process.spi.EntityReferenceInitializer;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

import org.jboss.logging.Logger;

import static org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext.EntityReferenceProcessingState;

/**
 * @author Steve Ebersole
 */
public class EntityReferenceInitializerImpl implements EntityReferenceInitializer {
	private static final Logger log = CoreLogging.logger( EntityReferenceInitializerImpl.class );

	private final EntityReference entityReference;
	private final EntityReferenceAliases entityReferenceAliases;
	private final boolean isReturn;

	public EntityReferenceInitializerImpl(
			EntityReference entityReference,
			EntityReferenceAliases entityReferenceAliases) {
		this( entityReference, entityReferenceAliases, false );
	}

	public EntityReferenceInitializerImpl(
			EntityReference entityReference,
			EntityReferenceAliases entityReferenceAliases,
			boolean isRoot) {
		this.entityReference = entityReference;
		this.entityReferenceAliases = entityReferenceAliases;
		isReturn = isRoot;
	}

	@Override
	public EntityReference getEntityReference() {
		return entityReference;
	}

	@Override
	public void hydrateIdentifier(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException {

		final EntityReferenceProcessingState processingState = context.getProcessingState( entityReference );

		// get any previously registered identifier hydrated-state
		Object identifierHydratedForm = processingState.getIdentifierHydratedForm();
		if ( identifierHydratedForm == null ) {
			// if there is none, read it from the result set
			identifierHydratedForm = readIdentifierHydratedState( resultSet, context );

			// broadcast the fact that a hydrated identifier value just became associated with
			// this entity reference
			processingState.registerIdentifierHydratedForm( identifierHydratedForm );
		}
	}

	/**
	 * Read the identifier state for the entity reference for the currently processing row in the ResultSet
	 *
	 * @param resultSet The ResultSet being processed
	 * @param context The processing context
	 *
	 * @return The hydrated state
	 *
	 * @throws java.sql.SQLException Indicates a problem accessing the ResultSet
	 */
	private Object readIdentifierHydratedState(ResultSet resultSet, ResultSetProcessingContext context)
			throws SQLException {
		try {
			return entityReference.getEntityPersister().getIdentifierType().hydrate(
					resultSet,
					entityReferenceAliases.getColumnAliases().getSuffixedKeyAliases(),
					context.getSession(),
					null
			);
		}
		catch (Exception e) {
			throw new HibernateException(
					"Encountered problem trying to hydrate identifier for entity ["
							+ entityReference.getEntityPersister() + "]",
					e
			);
		}
	}

	@Override
	public void resolveEntityKey(ResultSet resultSet, ResultSetProcessingContextImpl context) {

		final EntityReferenceProcessingState processingState = context.getProcessingState( entityReference );

		// see if we already have an EntityKey associated with this EntityReference in the processing state.
		// if we do, this should have come from the optional entity identifier...
		final EntityKey entityKey = processingState.getEntityKey();
		if ( entityKey != null ) {
			log.debugf(
					"On call to EntityIdentifierReaderImpl#resolve, EntityKey was already known; " +
							"should only happen on root returns with an optional identifier specified"
			);
			return;
		}

		// Look for the hydrated form
		final Object identifierHydratedForm = processingState.getIdentifierHydratedForm();
		if ( identifierHydratedForm == null ) {
			// we need to register the missing identifier, but that happens later after all readers have had a chance
			// to resolve its EntityKey
			return;
		}

		final Type identifierType = entityReference.getEntityPersister().getIdentifierType();
		final Serializable resolvedId = (Serializable) identifierType.resolve(
				identifierHydratedForm,
				context.getSession(),
				null
		);
		if ( resolvedId != null ) {
			processingState.registerEntityKey(
					context.getSession().generateEntityKey( resolvedId, entityReference.getEntityPersister() )
			);
		}
	}

	@Override
	public void hydrateEntityState(ResultSet resultSet, ResultSetProcessingContextImpl context) {
		final EntityReferenceProcessingState processingState = context.getProcessingState( entityReference );

		// If there is no identifier for this entity reference for this row, nothing to do
		if ( processingState.isMissingIdentifier() ) {
			handleMissingIdentifier( context );
			return;
		}

		// make sure we have the EntityKey
		final EntityKey entityKey = processingState.getEntityKey();
		if ( entityKey == null ) {
			handleMissingIdentifier( context );
			return;
		}

		// Have we already hydrated this entity's state?
		if ( processingState.getEntityInstance() != null ) {
			return;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// In getting here, we know that:
		// 		1) We need to hydrate the entity state
		//		2) We have a valid EntityKey for the entity

		// see if we have an existing entry in the session for this EntityKey
		final Object existing = context.getSession().getEntityUsingInterceptor( entityKey );
		if ( existing != null ) {
			// It is previously associated with the Session, perform some checks
			if ( ! entityReference.getEntityPersister().isInstance( existing ) ) {
				throw new WrongClassException(
						"loaded object was of wrong class " + existing.getClass(),
						entityKey.getIdentifier(),
						entityReference.getEntityPersister().getEntityName()
				);
			}
			checkVersion( resultSet, context, entityKey, existing );

			// use the existing association as the hydrated state
			processingState.registerEntityInstance( existing );
			//context.registerHydratedEntity( entityReference, entityKey, existing );
			return;
		}

		// Otherwise, we need to load it from the ResultSet...

		// determine which entity instance to use.  Either the supplied one, or instantiate one
		Object optionalEntityInstance = null;
		if ( isReturn && context.shouldUseOptionalEntityInformation() ) {
			final EntityKey optionalEntityKey = ResultSetProcessorHelper.getOptionalObjectKey(
					context.getQueryParameters(),
					context.getSession()
			);
			if ( optionalEntityKey != null ) {
				if ( optionalEntityKey.equals( entityKey ) ) {
					optionalEntityInstance = context.getQueryParameters().getOptionalObject();
				}
			}
		}

		final String concreteEntityTypeName = getConcreteEntityTypeName( resultSet, context, entityKey );

		final Object entityInstance = optionalEntityInstance != null
				? optionalEntityInstance
				: context.getSession().instantiate( concreteEntityTypeName, entityKey.getIdentifier() );

		processingState.registerEntityInstance( entityInstance );

		// need to hydrate it.
		// grab its state from the ResultSet and keep it in the Session
		// (but don't yet initialize the object itself)
		// note that we acquire LockMode.READ even if it was not requested
		log.trace( "hydrating entity state" );
		final LockMode requestedLockMode = context.resolveLockMode( entityReference );
		final LockMode lockModeToAcquire = requestedLockMode == LockMode.NONE
				? LockMode.READ
				: requestedLockMode;

		loadFromResultSet(
				resultSet,
				context,
				entityInstance,
				concreteEntityTypeName,
				entityKey,
				lockModeToAcquire
		);
	}

	private void handleMissingIdentifier(ResultSetProcessingContext context) {
		if ( EntityFetch.class.isInstance( entityReference ) ) {
			final EntityFetch fetch = (EntityFetch) entityReference;
			final EntityType fetchedType = fetch.getFetchedType();
			if ( ! fetchedType.isOneToOne() ) {
				return;
			}

			final EntityReferenceProcessingState fetchOwnerState = context.getOwnerProcessingState( fetch );
			if ( fetchOwnerState == null ) {
				throw new IllegalStateException( "Could not locate fetch owner state" );
			}

			final EntityKey ownerEntityKey = fetchOwnerState.getEntityKey();
			if ( ownerEntityKey != null ) {
				context.getSession().getPersistenceContext().addNullProperty(
						ownerEntityKey,
						fetchedType.getPropertyName()
				);
			}
		}
	}

	private void loadFromResultSet(
			ResultSet resultSet,
			ResultSetProcessingContext context,
			Object entityInstance,
			String concreteEntityTypeName,
			EntityKey entityKey,
			LockMode lockModeToAcquire) {
		final Serializable id = entityKey.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable concreteEntityPersister = (Loadable) context.getSession().getFactory().getEntityPersister( concreteEntityTypeName );

		if ( log.isTraceEnabled() ) {
			log.tracev(
					"Initializing object from ResultSet: {0}",
					MessageHelper.infoString(
							concreteEntityPersister,
							id,
							context.getSession().getFactory()
					)
			);
		}

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(
				entityKey,
				entityInstance,
				concreteEntityPersister,
				lockModeToAcquire,
				!context.getLoadPlan().areLazyAttributesForceFetched(),
				context.getSession()
		);

		final EntityPersister rootEntityPersister = context.getSession().getFactory().getEntityPersister(
				concreteEntityPersister.getRootEntityName()
		);
		final Object[] values;
		try {
			values = concreteEntityPersister.hydrate(
					resultSet,
					id,
					entityInstance,
					(Loadable) entityReference.getEntityPersister(),
					concreteEntityPersister == rootEntityPersister
							? entityReferenceAliases.getColumnAliases().getSuffixedPropertyAliases()
							: entityReferenceAliases.getColumnAliases().getSuffixedPropertyAliases( concreteEntityPersister ),
					context.getLoadPlan().areLazyAttributesForceFetched(),
					context.getSession()
			);

			context.getProcessingState( entityReference ).registerHydratedState( values );
		}
		catch (SQLException e) {
			throw context.getSession().getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Could not read entity state from ResultSet : " + entityKey
			);
		}

		final Object rowId;
		try {
			rowId = concreteEntityPersister.hasRowId()
					? resultSet.getObject( entityReferenceAliases.getColumnAliases().getRowIdAlias() )
					: null;
		}
		catch (SQLException e) {
			throw context.getSession().getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Could not read entity row-id from ResultSet : " + entityKey
			);
		}

		final EntityType entityType = EntityFetch.class.isInstance( entityReference )
				? ( (EntityFetch) entityReference ).getFetchedType()
				: entityReference.getEntityPersister().getEntityMetamodel().getEntityType();

		if ( entityType != null ) {
			String ukName = entityType.getRHSUniqueKeyPropertyName();
			if ( ukName != null ) {
				final int index = ( (UniqueKeyLoadable) concreteEntityPersister ).getPropertyIndex( ukName );
				final Type type = concreteEntityPersister.getPropertyTypes()[index];

				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be

				EntityUniqueKey euk = new EntityUniqueKey(
						entityReference.getEntityPersister().getEntityName(),
						ukName,
						type.semiResolve( values[index], context.getSession(), entityInstance ),
						type,
						concreteEntityPersister.getEntityMode(),
						context.getSession().getFactory()
				);
				context.getSession().getPersistenceContext().addEntity( euk, entityInstance );
			}
		}

		TwoPhaseLoad.postHydrate(
				concreteEntityPersister,
				id,
				values,
				rowId,
				entityInstance,
				lockModeToAcquire,
				!context.getLoadPlan().areLazyAttributesForceFetched(),
				context.getSession()
		);

		context.registerHydratedEntity( entityReference, entityKey, entityInstance );
	}

	private String getConcreteEntityTypeName(
			ResultSet resultSet,
			ResultSetProcessingContext context,
			EntityKey entityKey) {
		final Loadable loadable = (Loadable) entityReference.getEntityPersister();
		if ( ! loadable.hasSubclasses() ) {
			return entityReference.getEntityPersister().getEntityName();
		}

		final Object discriminatorValue;
		try {
			discriminatorValue = loadable.getDiscriminatorType().nullSafeGet(
					resultSet,
					entityReferenceAliases.getColumnAliases().getSuffixedDiscriminatorAlias(),
					context.getSession(),
					null
			);
		}
		catch (SQLException e) {
			throw context.getSession().getFactory().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Could not read discriminator value from ResultSet"
			);
		}

		final String result = loadable.getSubclassForDiscriminatorValue( discriminatorValue );

		if ( result == null ) {
			// whoops! we got an instance of another class hierarchy branch
			throw new WrongClassException(
					"Discriminator: " + discriminatorValue,
					entityKey.getIdentifier(),
					entityReference.getEntityPersister().getEntityName()
			);
		}

		return result;
	}

	private void checkVersion(
			ResultSet resultSet,
			ResultSetProcessingContext context,
			EntityKey entityKey,
			Object existing) {
		final LockMode requestedLockMode = context.resolveLockMode( entityReference );
		if ( requestedLockMode != LockMode.NONE ) {
			final LockMode currentLockMode = context.getSession().getPersistenceContext().getEntry( existing ).getLockMode();
			final boolean isVersionCheckNeeded = entityReference.getEntityPersister().isVersioned()
					&& currentLockMode.lessThan( requestedLockMode );

			// we don't need to worry about existing version being uninitialized because this block isn't called
			// by a re-entrant load (re-entrant loads *always* have lock mode NONE)
			if ( isVersionCheckNeeded ) {
				//we only check the version when *upgrading* lock modes
				checkVersion(
						context.getSession(),
						resultSet,
						entityReference.getEntityPersister(),
						entityReferenceAliases.getColumnAliases(),
						entityKey,
						existing
				);
				//we need to upgrade the lock mode to the mode requested
				context.getSession().getPersistenceContext().getEntry( existing ).setLockMode( requestedLockMode );
			}
		}
	}

	private void checkVersion(
			SessionImplementor session,
			ResultSet resultSet,
			EntityPersister persister,
			EntityAliases entityAliases,
			EntityKey entityKey,
			Object entityInstance) {
		final Object version = session.getPersistenceContext().getEntry( entityInstance ).getVersion();

		if ( version != null ) {
			//null version means the object is in the process of being loaded somewhere else in the ResultSet
			VersionType versionType = persister.getVersionType();
			final Object currentVersion;
			try {
				currentVersion = versionType.nullSafeGet(
						resultSet,
						entityAliases.getSuffixedVersionAliases(),
						session,
						null
				);
			}
			catch (SQLException e) {
				throw session.getFactory().getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"Could not read version value from result set"
				);
			}

			if ( !versionType.isEqual( version, currentVersion ) ) {
				if ( session.getFactory().getStatistics().isStatisticsEnabled() ) {
					session.getFactory().getStatisticsImplementor().optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), entityKey.getIdentifier() );
			}
		}
	}

	@Override
	public void finishUpRow(ResultSet resultSet, ResultSetProcessingContextImpl context) {
		// cant remember exactly what I was thinking here.  Maybe managing the row value caching stuff that is currently
		// done in ResultSetProcessingContextImpl.finishUpRow()
		//
		// anything else?
	}
}
