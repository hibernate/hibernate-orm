/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
 *
 */
package org.hibernate.loader.plan.build.internal;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.jboss.logging.Logger;

/**
 * Abstract strategy of building loadplan based on entity graph.
 *
 * The problem we're resolving here is, we have TWO trees to walk (only entity loading here):
 * <ul>
 * <ol>entity metadata and its associations</ol>
 * <ol>entity graph and attribute nodes</ol>
 * </ul>
 *
 * And most time, the entity graph tree is partial of entity metadata tree.
 *
 * So, the idea here is, we walk the entity metadata tree, just as how we build the static loadplan from mappings,
 * and we try to match the node to entity graph ( and subgraph ), if there is a match, then the attribute is fetched,
 * it is not, then depends on which property is used to apply this entity graph.
 *
 * @author Strong Liu <stliu@hibernate.org>
 * @author Brett Meyer
 */
public abstract class AbstractEntityGraphVisitationStrategy
		extends AbstractLoadPlanBuildingAssociationVisitationStrategy {
	private static final Logger LOG = CoreLogging.logger( AbstractEntityGraphVisitationStrategy.class );
	/**
	 * The JPA 2.1 SPEC's Entity Graph only defines _WHEN_ to load an attribute, it doesn't define _HOW_ to load it
	 * So I'm here just making an assumption that when it is EAGER, then we use JOIN, and when it is LAZY, then we use SELECT.
	 *
	 * NOTE: this may be changed in the near further, though ATM I have no idea how this will be changed to :)
	 * -- stliu
	 */
	protected static final FetchStrategy DEFAULT_EAGER = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	protected static final FetchStrategy DEFAULT_LAZY = new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT );
	protected final LoadQueryInfluencers loadQueryInfluencers;
	// Queue containing entity/sub graphs to be visited.
	private final ArrayDeque<GraphNodeImplementor> graphStack = new ArrayDeque<GraphNodeImplementor>();
	// Queue containing attributes being visited, used eventually to determine the fetch strategy.
	private final ArrayDeque<AttributeNodeImplementor> attributeStack = new ArrayDeque<AttributeNodeImplementor>();
	// Queue of maps containing the current graph node's attributes.  Used for fast lookup, instead of iterating
	// over graphStack.peekLast().attributeImplementorNodes().
	private final ArrayDeque<Map<String, AttributeNodeImplementor>> attributeMapStack
			= new ArrayDeque<Map<String, AttributeNodeImplementor>>();
	private EntityReturn rootEntityReturn;
	private final LockMode lockMode;

	protected AbstractEntityGraphVisitationStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,
			final LockMode lockMode) {
		super( sessionFactory );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockMode = lockMode;
	}

	@Override
	public void start() {
		super.start();
		graphStack.addLast( getRootEntityGraph() );
	}

	@Override
	public void finish() {
		super.finish();
		graphStack.removeLast();
		//applying a little internal stack checking
		if ( !graphStack.isEmpty() || !attributeStack.isEmpty() || !attributeMapStack.isEmpty() ) {
			throw new WalkingException( "Internal stack error" );
		}
	}

	@Override
	public void startingEntity(final EntityDefinition entityDefinition) {
		attributeMapStack.addLast( buildAttributeNodeMap() );
		super.startingEntity( entityDefinition );
	}

	/**
	 * Build "name" -- "attribute node" map from the current entity graph we're visiting.
	 */
	protected Map<String, AttributeNodeImplementor> buildAttributeNodeMap() {
		GraphNodeImplementor graphNode = graphStack.peekLast();
		List<AttributeNodeImplementor<?>> attributeNodeImplementors = graphNode.attributeImplementorNodes();
		Map<String, AttributeNodeImplementor> attributeNodeImplementorMap = attributeNodeImplementors.isEmpty() ? Collections
				.<String, AttributeNodeImplementor>emptyMap() : new HashMap<String, AttributeNodeImplementor>(
				attributeNodeImplementors.size()
		);
		for ( AttributeNodeImplementor attribute : attributeNodeImplementors ) {
			attributeNodeImplementorMap.put( attribute.getAttributeName(), attribute );
		}
		return attributeNodeImplementorMap;
	}

	@Override
	public void finishingEntity(final EntityDefinition entityDefinition) {
		attributeMapStack.removeLast();
		super.finishingEntity( entityDefinition );
	}

	/**
	 * I'm using NULL-OBJECT pattern here, for attributes that not existing in the EntityGraph,
	 * a predefined NULL-ATTRIBUTE-NODE is pushed to the stack.
	 *
	 * and for an not existing sub graph, a predefined NULL-SUBGRAPH is pushed to the stack.
	 *
	 * So, whenever we're start visiting an attribute, there will be a attribute node pushed to the attribute stack,
	 * and a subgraph node pushed to the graph stack.
	 *
	 * when we're finish visiting an attribute, these two will be poped from each stack.
	 */
	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		Map<String, AttributeNodeImplementor> attributeMap = attributeMapStack.peekLast();
		final String attrName = attributeDefinition.getName();
		AttributeNodeImplementor attributeNode = NON_EXIST_ATTRIBUTE_NODE;
		GraphNodeImplementor subGraphNode = NON_EXIST_SUBGRAPH_NODE;
		//the attribute is in the EntityGraph, so, let's continue
		if ( attributeMap.containsKey( attrName ) ) {
			attributeNode = attributeMap.get( attrName );
			//here we need to check if there is a subgraph (or sub key graph if it is an indexed attribute )
			Map<Class, Subgraph> subGraphs = attributeNode.getSubgraphs();
			Class javaType = attributeDefinition.getType().getReturnedClass();
			if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
				subGraphNode = (GraphNodeImplementor) subGraphs.get( javaType );
			}

		}
		attributeStack.addLast( attributeNode );
		graphStack.addLast( subGraphNode );
		return super.startingAttribute( attributeDefinition );
	}


	@Override
	public void finishingAttribute(final AttributeDefinition attributeDefinition) {
		attributeStack.removeLast();
		graphStack.removeLast();
		super.finishingAttribute( attributeDefinition );
	}


	@Override
	public void startingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		AttributeNodeImplementor attributeNode = attributeStack.peekLast();
		GraphNodeImplementor subGraphNode = NON_EXIST_SUBGRAPH_NODE;
		Map<Class, Subgraph> subGraphs = attributeNode.getSubgraphs();
		Class javaType = elementDefinition.getType().getReturnedClass();
		if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
			subGraphNode = (GraphNodeImplementor) subGraphs.get( javaType );
		}
		graphStack.addLast( subGraphNode );
		super.startingCollectionElements( elementDefinition );
	}

	@Override
	public void finishingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		super.finishingCollectionElements( elementDefinition );
		graphStack.removeLast();
	}


	@Override
	public void startingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		AttributeNodeImplementor attributeNode = attributeStack.peekLast();
		GraphNodeImplementor subGraphNode = NON_EXIST_SUBGRAPH_NODE;
		Map<Class, Subgraph> subGraphs = attributeNode.getKeySubgraphs();
		Class javaType = indexDefinition.getType().getReturnedClass();
		if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
			subGraphNode = (GraphNodeImplementor) subGraphs.get( javaType );
		}
		graphStack.addLast( subGraphNode );
		super.startingCollectionIndex( indexDefinition );
	}

	@Override
	public void finishingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		super.finishingCollectionIndex( indexDefinition );
		graphStack.removeLast();
	}


	@Override
	protected boolean supportsRootCollectionReturns() {
		return false; //entity graph doesn't support root collection.
	}


	@Override
	protected void addRootReturn(final Return rootReturn) {
		if ( this.rootEntityReturn != null ) {
			throw new HibernateException( "Root return already identified" );
		}
		if ( !( rootReturn instanceof EntityReturn ) ) {
			throw new HibernateException( "Load entity graph only supports EntityReturn" );
		}
		this.rootEntityReturn = (EntityReturn) rootReturn;
	}

	@Override
	protected FetchStrategy determineFetchStrategy(
			final AssociationAttributeDefinition attributeDefinition) {
		return attributeStack.peekLast() != NON_EXIST_ATTRIBUTE_NODE ? DEFAULT_EAGER : resolveImplicitFetchStrategyFromEntityGraph(
				attributeDefinition
		);
	}

	protected abstract FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition);

	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition, FetchStrategy fetchStrategy) {
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		final Integer maxFetchDepth = sessionFactory().getSettings().getMaximumFetchDepth();
		if ( maxFetchDepth != null && currentDepth() > maxFetchDepth ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
			// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		return fetchStrategy;
	}


	@Override
	public LoadPlan buildLoadPlan() {
		LOG.debug( "Building LoadPlan..." );
		return new LoadPlanImpl( rootEntityReturn, getQuerySpaces() );
	}

	abstract protected GraphNodeImplementor getRootEntityGraph();

	private static final AttributeNodeImplementor NON_EXIST_ATTRIBUTE_NODE = new AttributeNodeImplementor() {
		@Override
		public Attribute getAttribute() {
			return null;
		}

		@Override
		public AttributeNodeImplementor makeImmutableCopy() {
			return this;
		}

		@Override
		public String getAttributeName() {
			return null;
		}

		@Override
		public Map<Class, Subgraph> getSubgraphs() {
			return Collections.emptyMap();
		}

		@Override
		public Map<Class, Subgraph> getKeySubgraphs() {
			return Collections.emptyMap();
		}

		@Override
		public String toString() {
			return "Mocked NON-EXIST attribute node";
		}
	};
	private static final GraphNodeImplementor NON_EXIST_SUBGRAPH_NODE = new GraphNodeImplementor() {
		@Override
		public List<AttributeNodeImplementor<?>> attributeImplementorNodes() {
			return Collections.emptyList();
		}

		@Override
		public List<AttributeNode<?>> attributeNodes() {
			return Collections.emptyList();
		}
		
		@Override
		public boolean containsAttribute(String name) {
			return false;
		}
	};

	@Override
	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition) {
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
			return; // nothing to do
		}

		// Bi-directional association & the owning side was already visited.  If the current attribute node refers
		// to it, fetch.
		// ENTITY nature handled by super.
		final GraphNodeImplementor graphNode = graphStack.peekLast();
		if ( attributeDefinition.getAssociationNature() == AssociationAttributeDefinition.AssociationNature.COLLECTION
				&& ! graphNode.equals( NON_EXIST_SUBGRAPH_NODE)
				&& graphNode.containsAttribute( attributeDefinition.getName() )) {
			currentSource().buildCollectionAttributeFetch( attributeDefinition, fetchStrategy );
		}
		
		super.foundCircularAssociation( attributeDefinition );
	}
}
