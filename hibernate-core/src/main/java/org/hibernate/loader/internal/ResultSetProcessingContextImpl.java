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
package org.hibernate.loader.internal;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.engine.internal.TwoPhaseLoad;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.LoadPlanVisitationStrategyAdapter;
import org.hibernate.loader.plan.spi.LoadPlanVisitor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.loader.spi.NamedParameterContext;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessingContextImpl implements ResultSetProcessingContext {
	private static final Logger LOG = Logger.getLogger( ResultSetProcessingContextImpl.class );

	private final ResultSet resultSet;
	private final SessionImplementor session;
	private final LoadPlan loadPlan;
	private final boolean readOnly;
	private final QueryParameters queryParameters;
	private final NamedParameterContext namedParameterContext;
	private final boolean hadSubselectFetches;

	private final EntityKey dictatedRootEntityKey;

	private List<HydratedEntityRegistration> currentRowHydratedEntityRegistrationList;

	private Map<EntityPersister,Set<EntityKey>> subselectLoadableEntityKeyMap;
	private List<HydratedEntityRegistration> hydratedEntityRegistrationList;

	public ResultSetProcessingContextImpl(
			ResultSet resultSet,
			SessionImplementor session,
			LoadPlan loadPlan,
			boolean readOnly,
			boolean useOptionalEntityKey,
			QueryParameters queryParameters,
			NamedParameterContext namedParameterContext,
			boolean hadSubselectFetches) {
		this.resultSet = resultSet;
		this.session = session;
		this.loadPlan = loadPlan;
		this.readOnly = readOnly;
		this.queryParameters = queryParameters;
		this.namedParameterContext = namedParameterContext;
		this.hadSubselectFetches = hadSubselectFetches;

		if ( useOptionalEntityKey ) {
			this.dictatedRootEntityKey = ResultSetProcessorHelper.getOptionalObjectKey( queryParameters, session );
			if ( this.dictatedRootEntityKey == null ) {
				throw new HibernateException( "Unable to resolve optional entity-key" );
			}
		}
		else {
			this.dictatedRootEntityKey = null;
		}
	}

	@Override
	public SessionImplementor getSession() {
		return session;
	}

	@Override
	public QueryParameters getQueryParameters() {
		return queryParameters;
	}

	@Override
	public EntityKey getDictatedRootEntityKey() {
		return dictatedRootEntityKey;
	}

	private Map<EntityReference,IdentifierResolutionContext> identifierResolutionContextMap;

	@Override
	public IdentifierResolutionContext getIdentifierResolutionContext(final EntityReference entityReference) {
		if ( identifierResolutionContextMap == null ) {
			identifierResolutionContextMap = new HashMap<EntityReference, IdentifierResolutionContext>();
		}
		IdentifierResolutionContext context = identifierResolutionContextMap.get( entityReference );
		if ( context == null ) {
			context = new IdentifierResolutionContext() {
				private Serializable hydratedForm;
				private EntityKey entityKey;

				@Override
				public EntityReference getEntityReference() {
					return entityReference;
				}

				@Override
				public void registerHydratedForm(Serializable hydratedForm) {
					if ( this.hydratedForm != null ) {
						// this could be bad...
					}
					this.hydratedForm = hydratedForm;
				}

				@Override
				public Serializable getHydratedForm() {
					return hydratedForm;
				}

				@Override
				public void registerEntityKey(EntityKey entityKey) {
					if ( this.entityKey != null ) {
						// again, could be trouble...
					}
					this.entityKey = entityKey;
				}

				@Override
				public EntityKey getEntityKey() {
					return entityKey;
				}
			};
			identifierResolutionContextMap.put( entityReference, context );
		}

		return context;
	}

	@Override
	public void checkVersion(
			ResultSet resultSet,
			EntityPersister persister,
			EntityAliases entityAliases,
			EntityKey entityKey,
			Object entityInstance) throws SQLException {
		final Object version = session.getPersistenceContext().getEntry( entityInstance ).getVersion();

		if ( version != null ) {
			//null version means the object is in the process of being loaded somewhere else in the ResultSet
			VersionType versionType = persister.getVersionType();
			Object currentVersion = versionType.nullSafeGet(
					resultSet,
					entityAliases.getSuffixedVersionAliases(),
					session,
					null
			);
			if ( !versionType.isEqual(version, currentVersion) ) {
				if ( session.getFactory().getStatistics().isStatisticsEnabled() ) {
					session.getFactory().getStatisticsImplementor()
							.optimisticFailure( persister.getEntityName() );
				}
				throw new StaleObjectStateException( persister.getEntityName(), entityKey.getIdentifier() );
			}
		}
	}

	@Override
	public String getConcreteEntityTypeName(
			final ResultSet rs,
			final EntityPersister persister,
			final EntityAliases entityAliases,
			final EntityKey entityKey) throws SQLException {

		final Loadable loadable = (Loadable) persister;
		if ( ! loadable.hasSubclasses() ) {
			return persister.getEntityName();
		}

		final Object discriminatorValue = loadable.getDiscriminatorType().nullSafeGet(
				rs,
				entityAliases.getSuffixedDiscriminatorAlias(),
				session,
				null
		);

		final String result = loadable.getSubclassForDiscriminatorValue( discriminatorValue );

		if ( result == null ) {
			// whoops! we got an instance of another class hierarchy branch
			throw new WrongClassException(
					"Discriminator: " + discriminatorValue,
					entityKey.getIdentifier(),
					persister.getEntityName()
			);
		}

		return result;
	}

	@Override
	public void loadFromResultSet(
			ResultSet resultSet,
			Object entityInstance,
			String concreteEntityTypeName,
			EntityKey entityKey,
			EntityAliases entityAliases,
			LockMode acquiredLockMode,
			EntityPersister rootPersister,
			boolean eagerFetch,
			EntityType associationType) throws SQLException {

		final Serializable id = entityKey.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable persister = (Loadable) getSession().getFactory().getEntityPersister( concreteEntityTypeName );

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Initializing object from ResultSet: {0}",
					MessageHelper.infoString(
							persister,
							id,
							getSession().getFactory()
					)
			);
		}

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity(
				entityKey,
				entityInstance,
				persister,
				acquiredLockMode,
				!eagerFetch,
				session
		);

		// This is not very nice (and quite slow):
		final String[][] cols = persister == rootPersister ?
				entityAliases.getSuffixedPropertyAliases() :
				entityAliases.getSuffixedPropertyAliases(persister);

		final Object[] values = persister.hydrate(
				resultSet,
				id,
				entityInstance,
				(Loadable) rootPersister,
				cols,
				eagerFetch,
				session
		);

		final Object rowId = persister.hasRowId() ? resultSet.getObject( entityAliases.getRowIdAlias() ) : null;

		if ( associationType != null ) {
			String ukName = associationType.getRHSUniqueKeyPropertyName();
			if ( ukName != null ) {
				final int index = ( (UniqueKeyLoadable) persister ).getPropertyIndex( ukName );
				final Type type = persister.getPropertyTypes()[index];

				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be

				EntityUniqueKey euk = new EntityUniqueKey(
						rootPersister.getEntityName(), //polymorphism comment above
						ukName,
						type.semiResolve( values[index], session, entityInstance ),
						type,
						persister.getEntityMode(),
						session.getFactory()
				);
				session.getPersistenceContext().addEntity( euk, entityInstance );
			}
		}

		TwoPhaseLoad.postHydrate(
				persister,
				id,
				values,
				rowId,
				entityInstance,
				acquiredLockMode,
				!eagerFetch,
				session
		);

	}

	@Override
	public void registerHydratedEntity(EntityPersister persister, EntityKey entityKey, Object entityInstance) {
		if ( currentRowHydratedEntityRegistrationList == null ) {
			currentRowHydratedEntityRegistrationList = new ArrayList<HydratedEntityRegistration>();
		}
		currentRowHydratedEntityRegistrationList.add( new HydratedEntityRegistration( persister, entityKey, entityInstance ) );
	}

	/**
	 * Package-protected
	 */
	void finishUpRow() {
		if ( currentRowHydratedEntityRegistrationList == null ) {
			return;
		}


		// managing the running list of registrations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( hydratedEntityRegistrationList == null ) {
			hydratedEntityRegistrationList = new ArrayList<HydratedEntityRegistration>();
		}
		hydratedEntityRegistrationList.addAll( currentRowHydratedEntityRegistrationList );


		// managing the map forms needed for subselect fetch generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( ! hadSubselectFetches ) {
			return;
		}
		if ( subselectLoadableEntityKeyMap == null ) {
			subselectLoadableEntityKeyMap = new HashMap<EntityPersister, Set<EntityKey>>();
		}
		for ( HydratedEntityRegistration registration : currentRowHydratedEntityRegistrationList ) {
			Set<EntityKey> entityKeys = subselectLoadableEntityKeyMap.get( registration.persister );
			if ( entityKeys == null ) {
				entityKeys = new HashSet<EntityKey>();
				subselectLoadableEntityKeyMap.put( registration.persister, entityKeys );
			}
			entityKeys.add( registration.key );
		}

		// release the currentRowHydratedEntityRegistrationList entries
		currentRowHydratedEntityRegistrationList.clear();
	}

	/**
	 * Package-protected
	 *
	 * @param afterLoadActionList List of after-load actions to perform
	 */
	void finishUp(List<AfterLoadAction> afterLoadActionList) {
		initializeEntitiesAndCollections( afterLoadActionList );
		createSubselects();

		if ( hydratedEntityRegistrationList != null ) {
			hydratedEntityRegistrationList.clear();
			hydratedEntityRegistrationList = null;
		}

		if ( subselectLoadableEntityKeyMap != null ) {
			subselectLoadableEntityKeyMap.clear();
			subselectLoadableEntityKeyMap = null;
		}
	}

	private void initializeEntitiesAndCollections(List<AfterLoadAction> afterLoadActionList) {
		// for arrays, we should end the collection load before resolving the entities, since the
		// actual array instances are not instantiated during loading
		finishLoadingArrays();


		// IMPORTANT: reuse the same event instances for performance!
		final PreLoadEvent preLoadEvent;
		final PostLoadEvent postLoadEvent;
		if ( session.isEventSource() ) {
			preLoadEvent = new PreLoadEvent( (EventSource) session );
			postLoadEvent = new PostLoadEvent( (EventSource) session );
		}
		else {
			preLoadEvent = null;
			postLoadEvent = null;
		}

		// now finish loading the entities (2-phase load)
		performTwoPhaseLoad( preLoadEvent, postLoadEvent );

		// now we can finalize loading collections
		finishLoadingCollections();

		// finally, perform post-load operations
		postLoad( postLoadEvent, afterLoadActionList );
	}

	private void finishLoadingArrays() {
		LoadPlanVisitor.visit(
				loadPlan,
				new LoadPlanVisitationStrategyAdapter() {
					@Override
					public void handleCollectionReturn(CollectionReturn rootCollectionReturn) {
						endLoadingArray( rootCollectionReturn.getCollectionPersister() );
					}

					@Override
					public void startingCollectionFetch(CollectionFetch collectionFetch) {
						endLoadingArray( collectionFetch.getCollectionPersister() );
					}

					private void endLoadingArray(CollectionPersister persister) {
						if ( persister.isArray() ) {
							session.getPersistenceContext()
									.getLoadContexts()
									.getCollectionLoadContext( resultSet )
									.endLoadingCollections( persister );
						}
					}
				}
		);
	}

	private void performTwoPhaseLoad(PreLoadEvent preLoadEvent, PostLoadEvent postLoadEvent) {
		final int numberOfHydratedObjects = hydratedEntityRegistrationList == null
				? 0
				: hydratedEntityRegistrationList.size();
		LOG.tracev( "Total objects hydrated: {0}", numberOfHydratedObjects );

		if ( hydratedEntityRegistrationList == null ) {
			return;
		}

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrationList ) {
			TwoPhaseLoad.initializeEntity( registration.instance, readOnly, session, preLoadEvent, postLoadEvent );
		}
	}

	private void finishLoadingCollections() {
		LoadPlanVisitor.visit(
				loadPlan,
				new LoadPlanVisitationStrategyAdapter() {
					@Override
					public void handleCollectionReturn(CollectionReturn rootCollectionReturn) {
						endLoadingArray( rootCollectionReturn.getCollectionPersister() );
					}

					@Override
					public void startingCollectionFetch(CollectionFetch collectionFetch) {
						endLoadingArray( collectionFetch.getCollectionPersister() );
					}

					private void endLoadingArray(CollectionPersister persister) {
						if ( ! persister.isArray() ) {
							session.getPersistenceContext()
									.getLoadContexts()
									.getCollectionLoadContext( resultSet )
									.endLoadingCollections( persister );
						}
					}
				}
		);
	}

	private void postLoad(PostLoadEvent postLoadEvent, List<AfterLoadAction> afterLoadActionList) {
		// Until this entire method is refactored w/ polymorphism, postLoad was
		// split off from initializeEntity.  It *must* occur after
		// endCollectionLoad to ensure the collection is in the
		// persistence context.
		if ( hydratedEntityRegistrationList == null ) {
			return;
		}

		for ( HydratedEntityRegistration registration : hydratedEntityRegistrationList ) {
			TwoPhaseLoad.postLoad( registration.instance, session, postLoadEvent );
			if ( afterLoadActionList != null ) {
				for ( AfterLoadAction afterLoadAction : afterLoadActionList ) {
					afterLoadAction.afterLoad( session, registration.instance, (Loadable) registration.persister );
				}
			}
		}
	}

	private void createSubselects() {
		if ( subselectLoadableEntityKeyMap.size() <= 1 ) {
			// if we only returned one entity, query by key is more efficient; so do nothing here
			return;
		}

		final Map<String, int[]> namedParameterLocMap =
				ResultSetProcessorHelper.buildNamedParameterLocMap( queryParameters, namedParameterContext );

		for ( Map.Entry<EntityPersister, Set<EntityKey>> entry : subselectLoadableEntityKeyMap.entrySet() ) {
			if ( ! entry.getKey().hasSubselectLoadableCollections() ) {
				continue;
			}

			SubselectFetch subselectFetch = new SubselectFetch(
					//getSQLString(),
					null, // aliases[i],
					(Loadable) entry.getKey(),
					queryParameters,
					entry.getValue(),
					namedParameterLocMap
			);

			for ( EntityKey key : entry.getValue() ) {
				session.getPersistenceContext().getBatchFetchQueue().addSubselect( key, subselectFetch );
			}

		}
	}

	private static class HydratedEntityRegistration {
		private final EntityPersister persister;
		private final EntityKey key;
		private final Object instance;

		private HydratedEntityRegistration(EntityPersister persister, EntityKey key, Object instance) {
			this.persister = persister;
			this.key = key;
			this.instance = instance;
		}
	}
}
