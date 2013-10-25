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
package org.hibernate.loader.collection.plan;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.plan2.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan2.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan2.exec.internal.AbstractLoadPlanBasedLoader;
import org.hibernate.loader.plan2.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan2.exec.spi.BasicCollectionLoadQueryDetails;
import org.hibernate.loader.plan2.exec.spi.CollectionLoadQueryDetails;
import org.hibernate.loader.plan2.exec.spi.OneToManyLoadQueryDetails;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * A CollectionInitializer implementation based on using LoadPlans
 *
 * @author Gail Badner
 */
public abstract class AbstractLoadPlanBasedCollectionInitializer
		extends AbstractLoadPlanBasedLoader  implements CollectionInitializer {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractLoadPlanBasedCollectionInitializer.class );

	private final QueryableCollection collectionPersister;
	private final LoadPlan plan;
	private final CollectionLoadQueryDetails staticLoadQuery;

	public AbstractLoadPlanBasedCollectionInitializer(
			QueryableCollection collectionPersister,
			QueryBuildingParameters buildingParameters) {
		super( collectionPersister.getFactory() );
		this.collectionPersister = collectionPersister;

		final FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy =
				new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
						collectionPersister.getFactory(),
						buildingParameters.getQueryInfluencers(),
						buildingParameters.getLockMode() != null
								? buildingParameters.getLockMode()
								: buildingParameters.getLockOptions().getLockMode()
		);

		this.plan = MetamodelDrivenLoadPlanBuilder.buildRootCollectionLoadPlan( strategy, collectionPersister );
		this.staticLoadQuery = collectionPersister.isOneToMany() ?
				OneToManyLoadQueryDetails.makeForBatching(
						plan,
						buildingParameters,
						collectionPersister.getFactory()
				) :
				BasicCollectionLoadQueryDetails.makeForBatching(
						plan,
						buildingParameters,
						collectionPersister.getFactory()
				);
	}

	@Override
	public void initialize(Serializable id, SessionImplementor session)
			throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debugf( "Loading collection: %s",
					MessageHelper.collectionInfoString( collectionPersister, id, getFactory() ) );
		}


		final Serializable[] ids = new Serializable[]{id};
		try {
			final QueryParameters qp = new QueryParameters();
			qp.setPositionalParameterTypes( new Type[]{ collectionPersister.getKeyType() } );
			qp.setPositionalParameterValues( ids );
			qp.setCollectionKeys( ids );

			executeLoad(
					session,
					qp,
					staticLoadQuery,
					true,
					null

			);
		}
		catch ( SQLException sqle ) {
			throw getFactory().getSQLExceptionHelper().convert(
					sqle,
					"could not initialize a collection: " +
							MessageHelper.collectionInfoString( collectionPersister, id, getFactory() ),
					staticLoadQuery.getSqlStatement()
			);
		}

		log.debug( "Done loading collection" );
	}

	protected QueryableCollection collectionPersister() {
		return collectionPersister;
	}

	@Override
	protected CollectionLoadQueryDetails getStaticLoadQuery() {
		return staticLoadQuery;
	}

	@Override
	protected int[] getNamedParameterLocs(String name) {
		throw new AssertionFailure("no named parameters");
	}

	@Override
	protected void autoDiscoverTypes(ResultSet rs) {
		throw new AssertionFailure("Auto discover types not supported in this loader");
	}
}
