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
package org.hibernate.loader.plan.spi;

/**
 * @author Steve Ebersole
 */
public class LoadPlanVisitationStrategyAdapter implements LoadPlanVisitationStrategy {
	@Override
	public void start(LoadPlan loadPlan) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finish(LoadPlan loadPlan) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void startingRootReturn(Return rootReturn) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishingRootReturn(Return rootReturn) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void handleScalarReturn(ScalarReturn scalarReturn) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void handleEntityReturn(EntityReturn rootEntityReturn) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void handleCollectionReturn(CollectionReturn rootCollectionReturn) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void startingFetches(FetchOwner fetchOwner) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishingFetches(FetchOwner fetchOwner) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void startingEntityFetch(EntityFetch entityFetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishingEntityFetch(EntityFetch entityFetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void startingCollectionFetch(CollectionFetch collectionFetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishingCollectionFetch(CollectionFetch collectionFetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void startingCompositeFetch(CompositeFetch fetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void finishingCompositeFetch(CompositeFetch fetch) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
