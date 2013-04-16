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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.spi.AttributeNodeImplementor;
import org.hibernate.loader.plan.internal.LoadPlanImpl;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.CopyContext;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.visit.ReturnGraphVisitationStrategy;
import org.hibernate.loader.plan.spi.visit.ReturnGraphVisitationStrategyAdapter;
import org.hibernate.loader.spi.LoadPlanAdvisor;

/**
 * @author Steve Ebersole
 */
public class EntityGraphBasedLoadPlanAdvisor implements LoadPlanAdvisor {
	private static final Logger log = Logger.getLogger( EntityGraphBasedLoadPlanAdvisor.class );

	private final EntityGraphImpl root;
	private final AdviceStyle adviceStyle;

	public EntityGraphBasedLoadPlanAdvisor(EntityGraphImpl root, AdviceStyle adviceStyle) {
		if ( root == null ) {
			throw new IllegalArgumentException( "EntityGraph cannot be null" );
		}
		this.root = root;
		this.adviceStyle = adviceStyle;
	}

	public LoadPlan advise(LoadPlan loadPlan) {
		if ( root == null ) {
			log.debug( "Skipping load plan advising: no entity graph was specified" );
		}
		else {
			// for now, lets assume that the graph and the load-plan returns have to match up
			EntityReturn entityReturn = findRootEntityReturn( loadPlan );
			if ( entityReturn == null ) {
				log.debug( "Skipping load plan advising: not able to find appropriate root entity return in load plan" );
			}
			else {
				final String entityName = entityReturn.getEntityPersister().getEntityName();
				if ( ! root.appliesTo( entityName ) ) {
					log.debugf(
							"Skipping load plan advising: entity types did not match : [%s] and [%s]",
							root.getEntityType().getName(),
							entityName
					);
				}
				else {
					// ok to apply the advice
					return applyAdvice( entityReturn );
				}
			}
		}

		// return the original load-plan
		return loadPlan;
	}

	private LoadPlan applyAdvice(final EntityReturn entityReturn) {
		final EntityReturn copy = entityReturn.makeCopy( new CopyContextImpl( entityReturn ) );
		return new LoadPlanImpl( copy );
	}

	private EntityReturn findRootEntityReturn(LoadPlan loadPlan) {
		EntityReturn rootEntityReturn = null;
		for ( Return rtn : loadPlan.getReturns() ) {
			if ( ! EntityReturn.class.isInstance( rtn ) ) {
				continue;
			}

			if ( rootEntityReturn != null ) {
				log.debug( "Multiple EntityReturns were found" );
				return null;
			}

			rootEntityReturn = (EntityReturn) rtn;
		}

		if ( rootEntityReturn == null ) {
			log.debug( "Unable to find root entity return in load plan" );
		}

		return rootEntityReturn;
	}

	public static enum AdviceStyle {
		FETCH,
		LOAD
	}

	public class CopyContextImpl implements CopyContext {
		private final ReturnGraphVisitationStrategyImpl strategy;

		public CopyContextImpl(EntityReturn entityReturn) {
			strategy = new ReturnGraphVisitationStrategyImpl( entityReturn );
		}

		@Override
		public ReturnGraphVisitationStrategy getReturnGraphVisitationStrategy() {
			return strategy;
		}
	}

	public class ReturnGraphVisitationStrategyImpl extends ReturnGraphVisitationStrategyAdapter {
		private ArrayDeque<NodeDescriptor> nodeStack = new ArrayDeque<NodeDescriptor>();

		public ReturnGraphVisitationStrategyImpl(EntityReturn entityReturn) {
			nodeStack.addFirst( new EntityReferenceDescriptor( entityReturn, new RootEntityGraphNode( root ) ) );
		}

		@Override
		public void finishingRootReturn(Return rootReturn) {
			nodeStack.removeFirst();
			super.finishingRootReturn( rootReturn );
		}

		@Override
		public void finishingFetches(FetchOwner fetchOwner) {
			nodeStack.peekFirst().applyMissingFetches();
			super.finishingFetches( fetchOwner );
		}

		@Override
		public void startingEntityFetch(EntityFetch entityFetch) {
			super.startingEntityFetch( entityFetch );

			final NodeDescriptor currentNode = nodeStack.peekFirst();
			final String attributeName = entityFetch.getOwnerPropertyName();
			final JpaGraphReference fetchedGraphReference = currentNode.attributeProcessed( attributeName );
			nodeStack.addFirst( new EntityReferenceDescriptor( entityFetch, fetchedGraphReference ) );
		}

		@Override
		public void finishingEntityFetch(EntityFetch entityFetch) {
			nodeStack.removeFirst();
			super.finishingEntityFetch( entityFetch );
		}

		@Override
		public void startingCollectionFetch(CollectionFetch collectionFetch) {
			super.startingCollectionFetch( collectionFetch );    //To change body of overridden methods use File | Settings | File Templates.
		}

		@Override
		public void finishingCollectionFetch(CollectionFetch collectionFetch) {
			super.finishingCollectionFetch( collectionFetch );    //To change body of overridden methods use File | Settings | File Templates.
		}

		@Override
		public void startingCompositeFetch(CompositeFetch fetch) {
			super.startingCompositeFetch( fetch );    //To change body of overridden methods use File | Settings | File Templates.
		}

		@Override
		public void finishingCompositeFetch(CompositeFetch fetch) {
			super.finishingCompositeFetch( fetch );    //To change body of overridden methods use File | Settings | File Templates.
		}
	}

	private static interface NodeDescriptor {
		public JpaGraphReference attributeProcessed(String attributeName);

		public void applyMissingFetches();
	}

	private static abstract class AbstractNodeDescriptor implements NodeDescriptor {
		private final FetchOwner fetchOwner;
		private final JpaGraphReference jpaGraphReference;

		protected AbstractNodeDescriptor(FetchOwner fetchOwner, JpaGraphReference jpaGraphReference) {
			this.fetchOwner = fetchOwner;
			this.jpaGraphReference = jpaGraphReference;
		}

		@Override
		public JpaGraphReference attributeProcessed(String attributeName) {
			if ( jpaGraphReference != null ) {
				return jpaGraphReference.attributeProcessed( attributeName );
			}
			else {
				return null;
			}
		}

		@Override
		public void applyMissingFetches() {
			if ( jpaGraphReference != null ) {
				jpaGraphReference.applyMissingFetches( fetchOwner );
			}
		}
	}

	private static class EntityReferenceDescriptor extends AbstractNodeDescriptor {
		private EntityReferenceDescriptor(EntityReturn entityReturn, JpaGraphReference correspondingJpaGraphNode) {
			super( entityReturn, correspondingJpaGraphNode );
		}

		@SuppressWarnings("unchecked")
		public EntityReferenceDescriptor(EntityFetch entityFetch, JpaGraphReference jpaGraphReference) {
			super( entityFetch, jpaGraphReference );
		}
	}

	private static interface JpaGraphReference {
		public JpaGraphReference attributeProcessed(String attributeName);
		public void applyMissingFetches(FetchOwner fetchOwner);
	}

	private static class RootEntityGraphNode implements JpaGraphReference {
		private final Map<String,AttributeNodeImplementor> graphAttributeMap;

		private RootEntityGraphNode(EntityGraphImpl entityGraph) {
			graphAttributeMap = new HashMap<String, AttributeNodeImplementor>();

			final List<AttributeNodeImplementor<?>> explicitAttributeNodes = entityGraph.attributeImplementorNodes();
			if ( explicitAttributeNodes != null ) {
				for ( AttributeNodeImplementor node : explicitAttributeNodes ) {
					graphAttributeMap.put( node.getAttributeName(), node );
				}
			}
		}

		@Override
		public JpaGraphReference attributeProcessed(String attributeName) {
			final AttributeNodeImplementor attributeNode = graphAttributeMap.remove( attributeName );

			if ( attributeNode == null ) {
				return null;
			}

			return new SubGraphNode( attributeNode );
		}


		@Override
		public void applyMissingFetches(FetchOwner fetchOwner) {
			for ( AttributeNodeImplementor attributeNode : graphAttributeMap.values() ) {
				System.out.println( "Found unprocessed attribute node : " + attributeNode.getAttributeName() );
			}
		}
	}

	private static class SubGraphNode implements JpaGraphReference {
		protected SubGraphNode(AttributeNodeImplementor attributeNode) {
		}

		@Override
		public JpaGraphReference attributeProcessed(String attributeName) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void applyMissingFetches(FetchOwner fetchOwner) {
			//To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
