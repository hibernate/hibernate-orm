/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.BatchingLoadQueryDetailsFactory;
import org.hibernate.loader.plan.exec.query.internal.QueryBuildingParametersImpl;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.param.ParameterBinder;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class Helper implements QueryBuildingParameters {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private static final QueryBuildingParameters queryBuildingParameters = new QueryBuildingParametersImpl(
			LoadQueryInfluencers.NONE,
			1,
			LockMode.NONE,
			null
	);

	private Helper() {
	}

	public LoadPlan buildLoadPlan(SessionFactoryImplementor sf, EntityPersister entityPersister) {
		final FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
				sf,
				queryBuildingParameters.getQueryInfluencers(),
				queryBuildingParameters.getLockMode()
		);
		return MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
	}

	public LoadQueryDetails buildLoadQueryDetails(EntityPersister entityPersister, SessionFactoryImplementor sf) {
		return buildLoadQueryDetails(
				buildLoadPlan( sf, entityPersister ),
				sf
		);
	}

	public LoadQueryDetails buildLoadQueryDetails(LoadPlan loadPlan, SessionFactoryImplementor sf) {
		return BatchingLoadQueryDetailsFactory.INSTANCE.makeEntityLoadQueryDetails(
				loadPlan,
				null,
				queryBuildingParameters,
				sf
		);
	}

	@Override
	public LoadQueryInfluencers getQueryInfluencers() {
		return queryBuildingParameters.getQueryInfluencers();
	}

	@Override
	public int getBatchSize() {
		return queryBuildingParameters.getBatchSize();
	}

	@Override
	public LockMode getLockMode() {
		return queryBuildingParameters.getLockMode();
	}

	@Override
	public LockOptions getLockOptions() {
		return queryBuildingParameters.getLockOptions();
	}

	public static NamedParameterContext parameterContext() {
		return name -> new int[0];
	}
}
