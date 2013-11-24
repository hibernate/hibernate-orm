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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessingContextImpl implements ResultSetProcessingContext {
	private static final Logger LOG = Logger.getLogger( ResultSetProcessingContextImpl.class );

	private final ResultSet resultSet;
	private final SessionImplementor session;
	private final LoadPlan loadPlan;
	private final boolean readOnly;
	private final boolean shouldUseOptionalEntityInformation;
	private final boolean forceFetchLazyAttributes;
	private final boolean shouldReturnProxies;
	private final QueryParameters queryParameters;
	private final NamedParameterContext namedParameterContext;
	private final boolean hadSubselectFetches;

	private List<HydratedEntityRegistration> currentRowHydratedEntityRegistrationList;

	private Map<EntityPersister,Set<EntityKey>> subselectLoadableEntityKeyMap;
	private List<HydratedEntityRegistration> hydratedEntityRegistrationList;

	/**
	 * Builds a ResultSetProcessingContextImpl
	 *
	 * @param resultSet
	 * @param session
	 * @param loadPlan
	 * @param readOnly
	 * @param shouldUseOptionalEntityInformation There are times when the "optional entity information" on
	 * QueryParameters should be used and times when they should not.  Collection initializers, batch loaders, etc
	 * are times when it should NOT be used.
	 * @param forceFetchLazyAttributes
	 * @param shouldReturnProxies
	 * @param queryParameters
	 * @param namedParameterContext
	 * @param hadSubselectFetches
	 */
	public ResultSetProcessingContextImpl(
			final ResultSet resultSet,
			final SessionImplementor session,
			final LoadPlan loadPlan,
			final boolean readOnly,
			final boolean shouldUseOptionalEntityInformation,
			final boolean forceFetchLazyAttributes,
			final boolean shouldReturnProxies,
			final QueryParameters queryParameters,
			final NamedParameterContext namedParameterContext,
			final boolean hadSubselectFetches) {
		this.resultSet = resultSet;
		this.session = session;
		this.loadPlan = loadPlan;
		this.readOnly = readOnly;
		this.shouldUseOptionalEntityInformation = shouldUseOptionalEntityInformation;
		this.forceFetchLazyAttributes = forceFetchLazyAttributes;
		this.shouldReturnProxies = shouldReturnProxies;
		this.queryParameters = queryParameters;
		this.namedParameterContext = namedParameterContext;
		this.hadSubselectFetches = hadSubselectFetches;

		if ( shouldUseOptionalEntityInformation ) {
			if ( queryParameters.getOptionalId() != null ) {
				// make sure we have only one return
				if ( loadPlan.getReturns().size() > 1 ) {
					throw new IllegalStateException( "Cannot specify 'optional entity' values with multi-return load plans" );
				}
			}
		}
	}

	@Override
	public SessionImplementor getSession() {
		return session;
	}

	@Override
	public boolean shouldUseOptionalEntityInformation() {
		return shouldUseOptionalEntityInformation;
	}

	@Override
	public QueryParameters getQueryParameters() {
		return queryParameters;
	}

	@Override
	public boolean shouldReturnProxies() {
		return shouldReturnProxies;
	}

	@Override
	public LoadPlan getLoadPlan() {
		return loadPlan;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	@Override
	public LockMode resolveLockMode(EntityReference entityReference) {
		if ( queryParameters.getLockOptions() != null && queryParameters.getLockOptions()
				.getLockMode() != null ) {
			return queryParameters.getLockOptions().getLockMode();
		}
		return LockMode.READ;
	}

	private Map<EntityReference,EntityReferenceProcessingState> identifierResolutionContextMap;

	@Override
	public EntityReferenceProcessingState getProcessingState(final EntityReference entityReference) {
		if ( identifierResolutionContextMap == null ) {
			identifierResolutionContextMap = new IdentityHashMap<EntityReference, EntityReferenceProcessingState>();
		}

		EntityReferenceProcessingState context = identifierResolutionContextMap.get( entityReference );
		if ( context == null ) {
			context = new EntityReferenceProcessingState() {
				private boolean wasMissingIdentifier;
				private Object identifierHydratedForm;
				private EntityKey entityKey;
				private Object[] hydratedState;
				private Object entityInstance;

				@Override
				public EntityReference getEntityReference() {
					return entityReference;
				}

				@Override
				public void registerMissingIdentifier() {
					if ( !EntityFetch.class.isInstance( entityReference ) ) {
						throw new IllegalStateException( "Missing return row identifier" );
					}
					ResultSetProcessingContextImpl.this.registerNonExists( (EntityFetch) entityReference );
					wasMissingIdentifier = true;
				}

				@Override
				public boolean isMissingIdentifier() {
					return wasMissingIdentifier;
				}

				@Override
				public void registerIdentifierHydratedForm(Object identifierHydratedForm) {
					this.identifierHydratedForm = identifierHydratedForm;
				}

				@Override
				public Object getIdentifierHydratedForm() {
					return identifierHydratedForm;
				}

				@Override
				public void registerEntityKey(EntityKey entityKey) {
					this.entityKey = entityKey;
				}

				@Override
				public EntityKey getEntityKey() {
					return entityKey;
				}

				@Override
				public void registerHydratedState(Object[] hydratedState) {
					this.hydratedState = hydratedState;
				}

				@Override
				public Object[] getHydratedState() {
					return hydratedState;
				}

				@Override
				public void registerEntityInstance(Object entityInstance) {
					this.entityInstance = entityInstance;
				}

				@Override
				public Object getEntityInstance() {
					return entityInstance;
				}
			};
			identifierResolutionContextMap.put( entityReference, context );
		}

		return context;
	}

	private void registerNonExists(EntityFetch fetch) {
		final EntityType fetchedType = fetch.getFetchedType();
		if ( ! fetchedType.isOneToOne() ) {
			return;
		}

		final EntityReferenceProcessingState fetchOwnerState = getOwnerProcessingState( fetch );
		if ( fetchOwnerState == null ) {
			throw new IllegalStateException( "Could not locate fetch owner state" );
		}

		final EntityKey ownerEntityKey = fetchOwnerState.getEntityKey();
		if ( ownerEntityKey == null ) {
			throw new IllegalStateException( "Could not locate fetch owner EntityKey" );
		}

		session.getPersistenceContext().addNullProperty(
				ownerEntityKey,
				fetchedType.getPropertyName()
		);
	}

	@Override
	public EntityReferenceProcessingState getOwnerProcessingState(Fetch fetch) {
		return getProcessingState( fetch.getSource().resolveEntityReference() );
	}

	@Override
	public void registerHydratedEntity(EntityReference entityReference, EntityKey entityKey, Object entityInstance) {
		if ( currentRowHydratedEntityRegistrationList == null ) {
			currentRowHydratedEntityRegistrationList = new ArrayList<HydratedEntityRegistration>();
		}
		currentRowHydratedEntityRegistrationList.add(
				new HydratedEntityRegistration(
						entityReference,
						entityKey,
						entityInstance
				)
		);
	}

	/**
	 * Package-protected
	 */
	void finishUpRow() {
		if ( currentRowHydratedEntityRegistrationList == null ) {
			if ( identifierResolutionContextMap != null ) {
				identifierResolutionContextMap.clear();
			}
			return;
		}


		// managing the running list of registrations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( hydratedEntityRegistrationList == null ) {
			hydratedEntityRegistrationList = new ArrayList<HydratedEntityRegistration>();
		}
		hydratedEntityRegistrationList.addAll( currentRowHydratedEntityRegistrationList );


		// managing the map forms needed for subselect fetch generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( hadSubselectFetches ) {
			if ( subselectLoadableEntityKeyMap == null ) {
				subselectLoadableEntityKeyMap = new HashMap<EntityPersister, Set<EntityKey>>();
			}
			for ( HydratedEntityRegistration registration : currentRowHydratedEntityRegistrationList ) {
				Set<EntityKey> entityKeys = subselectLoadableEntityKeyMap.get( registration.getEntityReference()
																					   .getEntityPersister() );
				if ( entityKeys == null ) {
					entityKeys = new HashSet<EntityKey>();
					subselectLoadableEntityKeyMap.put( registration.getEntityReference().getEntityPersister(), entityKeys );
				}
				entityKeys.add( registration.getKey() );
			}
		}

		// release the currentRowHydratedEntityRegistrationList entries
		currentRowHydratedEntityRegistrationList.clear();

		identifierResolutionContextMap.clear();
	}

	public List<HydratedEntityRegistration> getHydratedEntityRegistrationList() {
		return hydratedEntityRegistrationList;
	}

	/**
	 * Package-protected
	 */
	void wrapUp() {
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

	private void createSubselects() {
		if ( subselectLoadableEntityKeyMap == null || subselectLoadableEntityKeyMap.size() <= 1 ) {
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

	public boolean isReadOnly() {
		return readOnly;
	}
}
