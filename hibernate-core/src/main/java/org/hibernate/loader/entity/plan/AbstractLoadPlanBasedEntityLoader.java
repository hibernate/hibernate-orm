/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
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
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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

	private final LoadQueryDetails staticLoadQuery;

	public AbstractLoadPlanBasedEntityLoader(
			OuterJoinLoadable entityPersister,
			SessionFactoryImplementor factory,
			String[] uniqueKeyColumnNames,
			Type uniqueKeyType,
			QueryBuildingParameters buildingParameters) {
		super( factory );
		this.entityPersister = entityPersister;
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = entityPersister.getEntityName();

		final LoadPlanBuildingAssociationVisitationStrategy strategy;
		if ( buildingParameters.getQueryInfluencers().getFetchGraph() != null ) {
			strategy = new FetchGraphLoadPlanBuildingStrategy(
					factory, buildingParameters.getQueryInfluencers(),buildingParameters.getLockMode()
			);
		}
		else if ( buildingParameters.getQueryInfluencers().getLoadGraph() != null ) {
			strategy = new LoadGraphLoadPlanBuildingStrategy(
					factory, buildingParameters.getQueryInfluencers(),buildingParameters.getLockMode()
			);
		}
		else {
			strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
					factory, buildingParameters.getQueryInfluencers(),buildingParameters.getLockMode()
			);
		}

		final LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
		this.staticLoadQuery = BatchingLoadQueryDetailsFactory.makeEntityLoadQueryDetails(
				plan,
				uniqueKeyColumnNames,
				buildingParameters,
				factory
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
			final SessionImplementor session,
			final Serializable[] ids,
			final Type idType,
			final Object optionalObject,
			final String optionalEntityName,
			final Serializable optionalId,
			final EntityPersister persister,
			LockOptions lockOptions) throws HibernateException {

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
	@Deprecated
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) throws HibernateException {
		return load( id, optionalObject, session, LockOptions.NONE );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {

		final Object result;
		try {
			final QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( new Type[] { entityPersister.getIdentifierType() } );
			qp.setPositionalParameterValues( new Object[] { id } );
			qp.setOptionalObject( optionalObject );
			qp.setOptionalEntityName( entityPersister.getEntityName() );
			qp.setOptionalId( id );
			qp.setLockOptions( lockOptions );

			final List results = executeLoad(
					session,
					qp,
					staticLoadQuery,
					false,
					null
			);
			result = extractEntityResult( results );
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
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

	protected Object extractEntityResult(List results) {
		if ( results.size() == 0 ) {
			return null;
		}
		else if ( results.size() == 1 ) {
			return results.get( 0 );
		}
		else {
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

		throw new HibernateException( "Unable to interpret given query results in terms of a load-entity query" );
	}

	protected int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure("no named parameters");
	}

	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure("Auto discover types not supported in this loader");
	}
}
