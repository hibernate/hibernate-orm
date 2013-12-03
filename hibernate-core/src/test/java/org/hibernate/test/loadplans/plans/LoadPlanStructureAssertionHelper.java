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
package org.hibernate.test.loadplans.plans;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.JoinWalker;
import org.hibernate.loader.entity.EntityJoinWalker;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.BatchingLoadQueryDetailsFactory;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.OuterJoinLoadable;

/**
 * Perform assertions based on a LoadPlan, specifically against the outputs/expectations of the legacy Loader approach.
 * <p/>
 * Mainly this is intended to be a transitory set of help since it is expected that Loader will go away replaced by
 * LoadPlans, QueryBuilders and ResultSetProcessors.  For now I want to make sure that the outputs (e.g., the SQL,
 * the extraction aliases) are the same given the same input.  That makes sure we have the best possibility of success
 * in designing and implementing the "replacement parts".
 *
 * @author Steve Ebersole
 */
public class LoadPlanStructureAssertionHelper {
	/**
	 * Singleton access to the helper
	 */
	public static final LoadPlanStructureAssertionHelper INSTANCE = new LoadPlanStructureAssertionHelper();

	/**
	 * Performs a basic comparison.  Builds a LoadPlan for the given persister and compares it against the
	 * expectations according to the Loader/Walker corollary.
	 *
	 * @param sf The SessionFactory
	 * @param persister The entity persister for which to build a LoadPlan and compare against the Loader/Walker
	 * expectations.
	 */
	public void performBasicComparison(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		// todo : allow these to be passed in by tests?
		final LoadQueryInfluencers influencers = LoadQueryInfluencers.NONE;
		final LockMode lockMode = LockMode.NONE;
		final int batchSize = 1;

		// legacy Loader-based contracts...
		final EntityJoinWalker walker = new EntityJoinWalker(
				persister,
				persister.getKeyColumnNames(),
				batchSize,
				lockMode,
				sf,
				influencers
		);
//		final EntityLoader loader = new EntityLoader( persister, lockMode, sf, influencers );

		LoadPlan plan = buildLoadPlan( sf, persister, influencers, lockMode );
		LoadQueryDetails details = BatchingLoadQueryDetailsFactory.makeEntityLoadQueryDetails(
				plan,
				persister.getKeyColumnNames(),
				new QueryBuildingParameters() {
					@Override
					public LoadQueryInfluencers getQueryInfluencers() {
						return influencers;
					}

					@Override
					public int getBatchSize() {
						return batchSize;
					}

					@Override
					public LockMode getLockMode() {
						return lockMode;
					}

					@Override
					public LockOptions getLockOptions() {
						return null;
					}
				}, sf
		);

		compare( walker, details );
	}

	public LoadPlan buildLoadPlan(
			SessionFactoryImplementor sf,
			OuterJoinLoadable persister,
			LoadQueryInfluencers influencers,
			LockMode lockMode) {
		FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
				sf,
				influencers,
				lockMode
				);
		return MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, persister );
	}

	public LoadPlan buildLoadPlan(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		return buildLoadPlan( sf, persister, LoadQueryInfluencers.NONE, LockMode.NONE );
	}

	private void compare(JoinWalker walker, LoadQueryDetails details) {
		System.out.println( "------ SQL -----------------------------------------------------------------" );
		System.out.println( "WALKER    : " + walker.getSQLString() );
		System.out.println( "LOAD-PLAN : " + details.getSqlStatement() );
		System.out.println( "----------------------------------------------------------------------------" );
		System.out.println( );
		System.out.println( "------ SUFFIXES ------------------------------------------------------------" );
		System.out.println( "WALKER    : " + StringHelper.join( ", ",  walker.getSuffixes() ) + " : "
									+ StringHelper.join( ", ", walker.getCollectionSuffixes() ) );
		System.out.println( "----------------------------------------------------------------------------" );
		System.out.println( );
	}
}
