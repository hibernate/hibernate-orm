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
package org.hibernate.test.loadplans.process;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.BatchingLoadQueryDetailsFactory;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class Helper implements QueryBuildingParameters {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
	}

	public LoadPlan buildLoadPlan(SessionFactoryImplementor sf, EntityPersister entityPersister) {
		final FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
				sf,
				LoadQueryInfluencers.NONE,
				LockMode.NONE
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
		return BatchingLoadQueryDetailsFactory.makeEntityLoadQueryDetails(
				loadPlan,
				null,
				this,
				sf
		);
	}

	@Override
	public LoadQueryInfluencers getQueryInfluencers() {
		return LoadQueryInfluencers.NONE;
	}

	@Override
	public int getBatchSize() {
		return 1;
	}

	@Override
	public LockMode getLockMode() {
		return null;
	}

	@Override
	public LockOptions getLockOptions() {
		return null;
	}
}
