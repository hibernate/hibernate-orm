/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.loader.plan.build.internal.FetchGraphLoadPlanBuildingStrategy;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.internal.LoadGraphLoadPlanBuildingStrategy;
import org.hibernate.loader.plan.build.spi.LoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.AbstractLoadPlanBasedLoader;
import org.hibernate.loader.plan.exec.internal.BatchingLoadQueryDetailsFactory;
import org.hibernate.loader.plan.exec.internal.EntityLoadQueryDetails;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessorResolver;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * A UniqueEntityLoader implementation based on using LoadPlans
 *
 * @author Steve Ebersole
 */
public abstract class AbstractLoadPlanBasedEntityLoader extends AbstractLoadPlanBasedLoader implements UniqueEntityLoader {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractLoadPlanBasedEntityLoader.class );

	private final OuterJoinLoadable entityPersister;
	private final Type uniqueKeyType;
	private final String entityName;

	private final EntityLoadQueryDetails staticLoadQuery;

	public AbstractLoadPlanBasedEntityLoader(
			OuterJoinLoadable entityPersister,
			SessionFactoryImplementor factory,
			String[] uniqueKeyColumnNames,
			Type uniqueKeyType,
			QueryBuildingParameters buildingParameters,
			ResultSetProcessorResolver resultSetProcessorResolver) {
		super( factory );
		this.entityPersister = entityPersister;
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = entityPersister.getEntityName();

		final LoadPlanBuildingAssociationVisitationStrategy strategy;

		final EffectiveEntityGraph effectiveEntityGraph = buildingParameters.getQueryInfluencers().getEffectiveEntityGraph();
		if ( effectiveEntityGraph.getSemantic() == GraphSemantic.FETCH ) {
			strategy = new FetchGraphLoadPlanBuildingStrategy(
					factory,
					effectiveEntityGraph.getGraph(),
					buildingParameters.getQueryInfluencers(),
					buildingParameters.getLockOptions() != null ? buildingParameters.getLockOptions().getLockMode() : buildingParameters.getLockMode()
			);
		}
		else if ( effectiveEntityGraph.getSemantic() == GraphSemantic.LOAD ) {
			strategy = new LoadGraphLoadPlanBuildingStrategy(
					factory,
					effectiveEntityGraph.getGraph(),
					buildingParameters.getQueryInfluencers(),
					buildingParameters.getLockOptions() != null ? buildingParameters.getLockOptions().getLockMode() : buildingParameters.getLockMode()
			);
		}
		else {
			strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
					factory,
					buildingParameters.getQueryInfluencers(),
					buildingParameters.getLockOptions() != null ? buildingParameters.getLockOptions().getLockMode() : buildingParameters.getLockMode()
			);
		}

		final LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
		this.staticLoadQuery = BatchingLoadQueryDetailsFactory.INSTANCE.makeEntityLoadQueryDetails(
				plan,
				uniqueKeyColumnNames,
				buildingParameters,
				factory,
				resultSetProcessorResolver
		);
	}

	public AbstractLoadPlanBasedEntityLoader(
			OuterJoinLoadable entityPersister,
			SessionFactoryImplementor factory,
			String[] uniqueKeyColumnNames,
			Type uniqueKeyType,
			QueryBuildingParameters buildingParameters) {
		this(
				entityPersister,
				factory,
				uniqueKeyColumnNames,
				uniqueKeyType,
				buildingParameters,ResultSetProcessorResolver.DEFAULT
		);
	}

	protected AbstractLoadPlanBasedEntityLoader(
			OuterJoinLoadable entityPersister,
			SessionFactoryImplementor factory,
			EntityLoadQueryDetails entityLoaderQueryDetailsTemplate,
			Type uniqueKeyType,
			QueryBuildingParameters buildingParameters,
			ResultSetProcessorResolver resultSetProcessorResolver) {
		super( factory );
		this.entityPersister = entityPersister;
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = entityPersister.getEntityName();

		this.staticLoadQuery = BatchingLoadQueryDetailsFactory.INSTANCE.makeEntityLoadQueryDetails(
				entityLoaderQueryDetailsTemplate,
				buildingParameters,
				resultSetProcessorResolver
		);
	}

	protected AbstractLoadPlanBasedEntityLoader(
			OuterJoinLoadable entityPersister,
			SessionFactoryImplementor factory,
			EntityLoadQueryDetails entityLoaderQueryDetailsTemplate,
			Type uniqueKeyType,
			QueryBuildingParameters buildingParameters) {
		this(
				entityPersister,
				factory,
				entityLoaderQueryDetailsTemplate,
				uniqueKeyType,
				buildingParameters,
				ResultSetProcessorResolver.DEFAULT
		);
	}

	@Override
	protected LoadQueryDetails getStaticLoadQuery() {
		return staticLoadQuery;
	}

	protected String getEntityName() {
		return entityName;
	}

	/**
	 * Called by wrappers that batch load entities
	 * @param persister only needed for logging
	 * @param lockOptions
	 */
	public final List loadEntityBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			final LockOptions lockOptions) throws HibernateException {
		return loadEntityBatch( session, ids, idType, optionalObject, optionalEntityName, optionalId, persister, lockOptions, null );
	}

	public final List loadEntityBatch(
			final SharedSessionContractImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			final LockOptions lockOptions,
			final Boolean readOnly) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debugf( "Batch loading entity: %s", MessageHelper.infoString( persister, ids, getFactory() ) );
		}

		final Type[] types = new Type[ids.length];
		Arrays.fill( types, idType );
		List result;
		try {
			final QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( types );
			qp.setPositionalParameterValues( ids );
			qp.setLockOptions( lockOptions );
			if ( readOnly != null ) {
				qp.setReadOnly( readOnly );
			}
			result = executeLoad(
					session,
					qp,
					staticLoadQuery,
					false,
					null
			);
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not load an entity batch: " + MessageHelper.infoString( entityPersister, ids, getFactory() ),
					staticLoadQuery.getSqlStatement()
			);
		}

		log.debug( "Done entity batch load" );

		return result;

	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session) throws HibernateException {
		return load( id, optionalObject, session, (Boolean) null );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, Boolean readOnly) throws HibernateException {
		return load( id, optionalObject, session, LockOptions.NONE, readOnly );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions) {
		return load( id, optionalObject, session, lockOptions, null );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions, Boolean readOnly) {

		final Object result;
		try {
			final QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( new Type[] { entityPersister.getIdentifierType() } );
			qp.setPositionalParameterValues( new Object[] { id } );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( entityPersister.getEntityName() );
			qp.setOptionalId( id );
			qp.setLockOptions( lockOptions );
			if ( readOnly != null ) {
				qp.setReadOnly( readOnly );
			}
			final List results = executeLoad(
					session,
					qp,
					staticLoadQuery,
					false,
					null
			);
			result = extractEntityResult( results, id );
		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not load an entity: " + MessageHelper.infoString(
							entityPersister,
							id,
							entityPersister.getIdentifierType(),
							getFactory()
					),
					staticLoadQuery.getSqlStatement()
			);
		}

		log.debugf( "Done entity load : %s#%s", getEntityName(), id );
		return result;
	}

	/**
	 * @deprecated {@link #extractEntityResult(List, Serializable)} should be used instead.
	 */
	@Deprecated
	protected Object extractEntityResult(List results) {
		return extractEntityResult( results, null );
	}

	protected Object extractEntityResult(List results, Serializable id) {
		if ( results.size() == 0 ) {
			return null;
		}
		else if ( results.size() == 1 ) {
			return results.get( 0 );
		}
		else if ( staticLoadQuery.hasCollectionInitializers() ) {
			final Object row = results.get( 0 );
			if ( row.getClass().isArray() ) {
				// the logical type of the result list is List<Object[]>.  See if the contained
				// array contains just one element, and return that if so
				final Object[] rowArray = (Object[]) row;
				if ( rowArray.length == 1 ) {
					return rowArray[0];
				}
			}
			else {
				return row;
			}
		}

		if ( id == null ) {
			throw new HibernateException(
					"Unable to interpret given query results in terms of a load-entity query for " +
							entityName
			);
		}
		else {
			throw new HibernateException(
					"More than one row with the given identifier was found: " +
							id +
							", for class: " +
							entityName
			);
		}
	}

	protected int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure("no named parameters");
	}

	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure("Auto discover types not supported in this loader");
	}
}
