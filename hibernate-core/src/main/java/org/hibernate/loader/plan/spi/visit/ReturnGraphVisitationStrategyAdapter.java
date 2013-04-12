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
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;

/**
 * @author Steve Ebersole
 */
public class ReturnGraphVisitationStrategyAdapter implements ReturnGraphVisitationStrategy {
	public static final ReturnGraphVisitationStrategyAdapter INSTANCE = new ReturnGraphVisitationStrategyAdapter();

	@Override
	public void startingRootReturn(Return rootReturn) {
	}

	@Override
	public void finishingRootReturn(Return rootReturn) {
	}

	@Override
	public void handleScalarReturn(ScalarReturn scalarReturn) {
	}

	@Override
	public void handleEntityReturn(EntityReturn rootEntityReturn) {
	}

	@Override
	public void handleCollectionReturn(CollectionReturn rootCollectionReturn) {
	}

	@Override
	public void startingFetches(FetchOwner fetchOwner) {
	}

	@Override
	public void finishingFetches(FetchOwner fetchOwner) {
	}

	@Override
	public void startingEntityFetch(EntityFetch entityFetch) {
	}

	@Override
	public void finishingEntityFetch(EntityFetch entityFetch) {
	}

	@Override
	public void startingCollectionFetch(CollectionFetch collectionFetch) {
	}

	@Override
	public void finishingCollectionFetch(CollectionFetch collectionFetch) {
	}

	@Override
	public void startingCompositeFetch(CompositeFetch fetch) {
	}

	@Override
	public void finishingCompositeFetch(CompositeFetch fetch) {
	}
}
