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
package org.hibernate.jpa.graph.internal.advisor;

import java.util.ArrayDeque;

import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.visit.ReturnGraphVisitationStrategyAdapter;

/**
 * The visitor strategy for visiting the return graph of the load plan being advised.
 *
 * @author Steve Ebersole
 */
public class ReturnGraphVisitationStrategyImpl extends ReturnGraphVisitationStrategyAdapter {
	private ArrayDeque<AdviceNodeDescriptor> nodeStack = new ArrayDeque<AdviceNodeDescriptor>();

	public ReturnGraphVisitationStrategyImpl(EntityReturn entityReturn, EntityGraphImpl jpaRoot) {
		nodeStack.addFirst( new AdviceNodeDescriptorEntityReference( entityReturn, new JpaGraphRootEntityReference( jpaRoot ) ) );
	}

	@Override
	public void finishingRootReturn(Return rootReturn) {
		nodeStack.removeFirst();
	}

	@Override
	public void finishingFetches(FetchOwner fetchOwner) {
		nodeStack.peekFirst().applyMissingFetches();
	}

	@Override
	public void startingEntityFetch(EntityFetch entityFetch) {
		final AdviceNodeDescriptor currentNode = nodeStack.peekFirst();
		final String attributeName = entityFetch.getOwnerPropertyName();
		final JpaGraphReference fetchedGraphReference = currentNode.attributeProcessed( attributeName );

		nodeStack.addFirst( new AdviceNodeDescriptorEntityReference( entityFetch, fetchedGraphReference ) );
	}

	@Override
	public void finishingEntityFetch(EntityFetch entityFetch) {
		nodeStack.removeFirst();
	}

	@Override
	public void startingCollectionFetch(CollectionFetch collectionFetch) {
		final AdviceNodeDescriptor currentNode = nodeStack.peekFirst();
		final String attributeName = collectionFetch.getOwnerPropertyName();
		final JpaGraphReference fetchedGraphReference = currentNode.attributeProcessed( attributeName );

		nodeStack.addFirst( new AdviceNodeDescriptorCollectionReference( collectionFetch, fetchedGraphReference ) );
	}

	@Override
	public void finishingCollectionFetch(CollectionFetch collectionFetch) {
		nodeStack.removeFirst();
	}

	@Override
	public void startingCompositeFetch(CompositeFetch fetch) {
		final AdviceNodeDescriptor currentNode = nodeStack.peekFirst();
		final String attributeName = fetch.getOwnerPropertyName();
		final JpaGraphReference fetchedGraphReference = currentNode.attributeProcessed( attributeName );

		nodeStack.addFirst( new AdviceNodeDescriptorCompositeReference( fetch, fetchedGraphReference ) );
	}

	@Override
	public void finishingCompositeFetch(CompositeFetch fetch) {
		nodeStack.removeFirst();
	}

}
