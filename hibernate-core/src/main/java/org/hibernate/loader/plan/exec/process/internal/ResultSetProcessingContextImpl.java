/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ResultSetProcessingContextImpl implements ResultSetProcessingContext {
	private static final Logger LOG = CoreLogging.logger( ResultSetProcessingContextImpl.class );

	private final ResultSet resultSet;
	private final SharedSessionContractImplementor session;
	private final LoadPlan loadPlan;
	private final AliasResolutionContext aliasResolutionContext;
	private final boolean readOnly;
	private final boolean shouldUseOptionalEntityInformation;
	private final boolean shouldReturnProxies;
	private final QueryParameters queryParameters;
	private final NamedParameterContext namedParameterContext;
	private final boolean hadSubselectFetches;

	private List<HydratedEntityRegistration> currentRowHydratedEntityRegistrationList;

	private Map<EntityReference,Set<EntityKey>> subselectLoadableEntityKeyMap;
	private List<HydratedEntityRegistration> hydratedEntityRegistrationList;
	private int nRowsRead = 0;

	private Map<EntityReference,EntityReferenceProcessingState> identifierResolutionContextMap;

	/**
	 * Builds a ResultSetProcessingContextImpl
	 *
	 * @param shouldUseOptionalEntityInformation There are times when the "optional entity information" on
	 * QueryParameters should be used and times when they should not.  Collection initializers, batch loaders, etc
	 * are times when it should NOT be used.
	 */
	public ResultSetProcessingContextImpl(
			final ResultSet resultSet,
			final SharedSessionContractImplementor session,
			final LoadPlan loadPlan,
			final AliasResolutionContext aliasResolutionContext,
			final boolean readOnly,
			final boolean shouldUseOptionalEntityInformation,
			final boolean shouldReturnProxies,
			final QueryParameters queryParameters,
			final NamedParameterContext namedParameterContext,
			final boolean hadSubselectFetches) {
		this.resultSet = resultSet;
		this.session = session;
		this.loadPlan = loadPlan;
		this.aliasResolutionContext = aliasResolutionContext;
		this.readOnly = readOnly;
		this.shouldUseOptionalEntityInformation = shouldUseOptionalEntityInformation;
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
	public SharedSessionContractImplementor getSession() {
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
		return LockMode.NONE;
	}

	@Override
	public EntityReferenceProcessingState getProcessingState(final EntityReference entityReference) {
		EntityReferenceProcessingState context;
		if ( identifierResolutionContextMap == null ) {
			//The default expected size of IdentityHashMap is 21, which is likely to allocate larger arrays than what is typically necessary.
			//Reducing to 5, as a reasonable estimate for typical use: any larger query can better justify the need to resize,
			//while single loads shouldn't pay such an high cost.
			//This can save a lot of memory as it reduces the internal table of IdentityHashMap from a 64 slot array, to 16 slots:
			//that's a 75% memory cost reduction for usage patterns which do many individual loads.
			identifierResolutionContextMap = new IdentityHashMap<>(5);
			context = null;
		}
		else {
			context = identifierResolutionContextMap.get( entityReference );
		}

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

		session.getPersistenceContextInternal().addNullProperty(
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
			currentRowHydratedEntityRegistrationList = new ArrayList<>();
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
		nRowsRead++;

		if ( currentRowHydratedEntityRegistrationList == null ) {
			if ( identifierResolutionContextMap != null ) {
				identifierResolutionContextMap.clear();
			}
			return;
		}


		// managing the running list of registrations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		final int sizeHint = currentRowHydratedEntityRegistrationList.size();
		if ( hydratedEntityRegistrationList == null ) {
			hydratedEntityRegistrationList = new ArrayList<>( sizeHint );
		}
		hydratedEntityRegistrationList.addAll( currentRowHydratedEntityRegistrationList );


		// managing the map forms needed for subselect fetch generation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( hadSubselectFetches ) {
			if ( subselectLoadableEntityKeyMap == null ) {
				subselectLoadableEntityKeyMap = new HashMap<>();
			}
			for ( HydratedEntityRegistration registration : currentRowHydratedEntityRegistrationList ) {
				Set<EntityKey> entityKeys = subselectLoadableEntityKeyMap.get(
						registration.getEntityReference()
				);
				if ( entityKeys == null ) {
					entityKeys = new HashSet<>();
					subselectLoadableEntityKeyMap.put( registration.getEntityReference(), entityKeys );
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

	public void wrapUp() {
		createSubselects();

		if ( hydratedEntityRegistrationList != null ) {
			hydratedEntityRegistrationList = null;
		}

		if ( subselectLoadableEntityKeyMap != null ) {
			subselectLoadableEntityKeyMap = null;
		}
	}

	private void createSubselects() {
		if ( subselectLoadableEntityKeyMap == null || nRowsRead <= 1 ) {
			LOG.tracef(
					"Skipping create subselects because there are fewer than 2 results, so query by key is more efficient.",
					getClass().getName()
			);
			return; // early return
		}

		final Map<String, int[]> namedParameterLocMap =
				ResultSetProcessorHelper.buildNamedParameterLocMap( queryParameters, namedParameterContext );

		final String subselectQueryString = SubselectFetch.createSubselectFetchQueryFragment( queryParameters );
		for ( Map.Entry<EntityReference, Set<EntityKey>> entry : subselectLoadableEntityKeyMap.entrySet() ) {
			if ( ! entry.getKey().getEntityPersister().hasSubselectLoadableCollections() ) {
				continue;
			}

			SubselectFetch subselectFetch = new SubselectFetch(
					subselectQueryString,
					aliasResolutionContext.resolveSqlTableAliasFromQuerySpaceUid( entry.getKey().getQuerySpaceUid() ),
					(Loadable) entry.getKey().getEntityPersister(),
					queryParameters,
					entry.getValue(),
					namedParameterLocMap
			);

			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			for ( EntityKey key : entry.getValue() ) {
				persistenceContext.getBatchFetchQueue().addSubselect( key, subselectFetch );
			}
		}
	}

	public boolean isReadOnly() {
		return readOnly;
	}
}
