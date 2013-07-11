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
package org.hibernate.loader;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.exec.query.internal.EntityLoadQueryBuilderImpl;
import org.hibernate.loader.plan.exec.query.spi.QueryBuildingParameters;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.EntityLoadQueryDetails;
import org.hibernate.loader.plan.internal.SingleRootReturnLoadPlanBuilderStrategy;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.build.MetadataDrivenLoadPlanBuilder;
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
		final SingleRootReturnLoadPlanBuilderStrategy strategy = new SingleRootReturnLoadPlanBuilderStrategy(
				sf,
				LoadQueryInfluencers.NONE
		);
		return MetadataDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, entityPersister );
	}

	public EntityLoadQueryDetails buildLoadQueryDetails(LoadPlan loadPlan, SessionFactoryImplementor sf) {
		return EntityLoadQueryDetails.makeForBatching(
				loadPlan,
				null,
				this,
				sf
		);
	}

	public String generateSql(SessionFactoryImplementor sf, LoadPlan plan, AliasResolutionContext aliasResolutionContext) {
		return EntityLoadQueryBuilderImpl.INSTANCE.generateSql(
				plan,
				sf,
				this,
				aliasResolutionContext
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
