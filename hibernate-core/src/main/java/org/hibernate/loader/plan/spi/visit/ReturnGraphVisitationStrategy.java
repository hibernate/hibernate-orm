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
 * A strategy for visiting a root {@link Return} and fetches it defines.
 *
 * @author Steve Ebersole
 */
public interface ReturnGraphVisitationStrategy {
	/**
	 * Notification that a new root return branch is being started.  Will be followed by calls
	 * to one of the following based on the type of return:<ul>
	 *     <li>{@link #handleScalarReturn}</li>
	 *     <li>{@link #handleEntityReturn}</li>
	 *     <li>{@link #handleCollectionReturn}</li>
	 * </ul>
	 *
	 * @param rootReturn The root return at the root of the branch.
	 */
	public void startingRootReturn(Return rootReturn);

	/**
	 * Notification that we are finishing up processing a root return branch
	 *
	 * @param rootReturn The RootReturn we are finishing up processing.
	 */
	public void finishingRootReturn(Return rootReturn);

	/**
	 * Notification that a scalar return is being processed.  Will be surrounded by calls to
	 * {@link #startingRootReturn} and {@link #finishingRootReturn}
	 *
	 * @param scalarReturn The scalar return
	 */
	public void handleScalarReturn(ScalarReturn scalarReturn);

	/**
	 * Notification that a root entity return is being processed.  Will be surrounded by calls to
	 * {@link #startingRootReturn} and {@link #finishingRootReturn}
	 *
	 * @param rootEntityReturn The root entity return
	 */
	public void handleEntityReturn(EntityReturn rootEntityReturn);

	/**
	 * Notification that a root collection return is being processed.  Will be surrounded by calls to
	 * {@link #startingRootReturn} and {@link #finishingRootReturn}
	 *
	 * @param rootCollectionReturn The root collection return
	 */
	public void handleCollectionReturn(CollectionReturn rootCollectionReturn);

	/**
	 * Notification that we are about to start processing the fetches for the given fetch owner.
	 *
	 * @param fetchOwner The fetch owner.
	 */
	public void startingFetches(FetchOwner fetchOwner);

	/**
	 * Notification that we are finishing up processing the fetches for the given fetch owner.
	 *
	 * @param fetchOwner The fetch owner.
	 */
	public void finishingFetches(FetchOwner fetchOwner);

	/**
	 * Notification we are starting the processing of an entity fetch
	 *
	 * @param entityFetch The entity fetch
	 */
	public void startingEntityFetch(EntityFetch entityFetch);

	/**
	 * Notification that we are finishing up the processing of an entity fetch
	 *
	 * @param entityFetch The entity fetch
	 */
	public void finishingEntityFetch(EntityFetch entityFetch);

	/**
	 * Notification we are starting the processing of a collection fetch
	 *
	 * @param collectionFetch The collection fetch
	 */
	public void startingCollectionFetch(CollectionFetch collectionFetch);

	/**
	 * Notification that we are finishing up the processing of a collection fetch
	 *
	 * @param collectionFetch The collection fetch
	 */
	public void finishingCollectionFetch(CollectionFetch collectionFetch);

	/**
	 * Notification we are starting the processing of a component fetch
	 *
	 * @param fetch The composite fetch
	 */
	public void startingCompositeFetch(CompositeFetch fetch);

	/**
	 * Notification that we are finishing up the processing of a composite fetch
	 *
	 * @param fetch The composite fetch
	 */
	public void finishingCompositeFetch(CompositeFetch fetch);
}
