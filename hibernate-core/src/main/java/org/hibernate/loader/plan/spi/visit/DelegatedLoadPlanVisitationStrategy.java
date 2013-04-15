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
package org.hibernate.loader.plan.spi.visit;

import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;

/**
 * @author Steve Ebersole
 */
public class DelegatedLoadPlanVisitationStrategy implements LoadPlanVisitationStrategy {
	private final ReturnGraphVisitationStrategy returnGraphVisitationStrategy;

	public DelegatedLoadPlanVisitationStrategy(ReturnGraphVisitationStrategy returnGraphVisitationStrategy) {
		this.returnGraphVisitationStrategy = returnGraphVisitationStrategy;
	}

	@Override
	public void start(LoadPlan loadPlan) {
	}

	@Override
	public void finish(LoadPlan loadPlan) {
	}

	@Override
	public void startingRootReturn(Return rootReturn) {
		returnGraphVisitationStrategy.startingRootReturn( rootReturn );
	}

	@Override
	public void finishingRootReturn(Return rootReturn) {
		returnGraphVisitationStrategy.finishingRootReturn( rootReturn );
	}

	@Override
	public void handleScalarReturn(ScalarReturn scalarReturn) {
		returnGraphVisitationStrategy.handleScalarReturn( scalarReturn );
	}

	@Override
	public void handleEntityReturn(EntityReturn rootEntityReturn) {
		returnGraphVisitationStrategy.handleEntityReturn( rootEntityReturn );
	}

	@Override
	public void handleCollectionReturn(CollectionReturn rootCollectionReturn) {
		returnGraphVisitationStrategy.handleCollectionReturn( rootCollectionReturn );
	}

	@Override
	public void startingFetches(FetchOwner fetchOwner) {
		returnGraphVisitationStrategy.startingFetches( fetchOwner );
	}

	@Override
	public void finishingFetches(FetchOwner fetchOwner) {
		returnGraphVisitationStrategy.finishingFetches( fetchOwner );
	}

	@Override
	public void startingEntityFetch(EntityFetch fetch) {
		returnGraphVisitationStrategy.startingEntityFetch( fetch );
	}

	@Override
	public void finishingEntityFetch(EntityFetch fetch) {
		returnGraphVisitationStrategy.finishingEntityFetch( fetch );
	}

	@Override
	public void startingCollectionFetch(CollectionFetch fetch) {
		returnGraphVisitationStrategy.startingCollectionFetch( fetch );
	}

	@Override
	public void finishingCollectionFetch(CollectionFetch fetch) {
		returnGraphVisitationStrategy.finishingCollectionFetch( fetch );
	}

	@Override
	public void startingCompositeFetch(CompositeFetch fetch) {
		returnGraphVisitationStrategy.startingCompositeFetch( fetch );
	}

	@Override
	public void finishingCompositeFetch(CompositeFetch fetch) {
		returnGraphVisitationStrategy.finishingCompositeFetch( fetch );
	}
}
