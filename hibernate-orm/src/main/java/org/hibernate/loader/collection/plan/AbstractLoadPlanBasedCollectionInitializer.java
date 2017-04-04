/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.collection.plan;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.AbstractLoadPlanBasedLoader;
import org.hibernate.loader.plan.exec.internal.BatchingLoadQueryDetailsFactory;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * An abstract {@link CollectionInitializer} implementation based on using LoadPlans
 *
 * @author Gail Badner
 */
public abstract class AbstractLoadPlanBasedCollectionInitializer
		extends AbstractLoadPlanBasedLoader  implements CollectionInitializer {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractLoadPlanBasedCollectionInitializer.class );

	private final QueryableCollection collectionPersister;
	private final LoadQueryDetails staticLoadQuery;
	private final LockOptions lockOptions;

	public AbstractLoadPlanBasedCollectionInitializer(
			QueryableCollection collectionPersister,
			QueryBuildingParameters buildingParameters) {
		super( collectionPersister.getFactory() );
		this.collectionPersister = collectionPersister;
		this.lockOptions = buildingParameters.getLockMode() != null
				? new LockOptions( buildingParameters.getLockMode() )
				: buildingParameters.getLockOptions();

		final FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy =
				new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
						collectionPersister.getFactory(),
						buildingParameters.getQueryInfluencers(),
						this.lockOptions.getLockMode()
		);

		final LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootCollectionLoadPlan( strategy, collectionPersister );
		this.staticLoadQuery = BatchingLoadQueryDetailsFactory.INSTANCE.makeCollectionLoadQueryDetails(
				collectionPersister,
				plan,
				buildingParameters
		);
	}

	@Override
	public void initialize(Serializable id, SharedSessionContractImplementor session)
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

			qp.setLockOptions( lockOptions );

			executeLoad(
					session,
					qp,
					staticLoadQuery,
					true,
					null

			);
		}
		catch ( SQLException sqle ) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
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
	protected LoadQueryDetails getStaticLoadQuery() {
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
