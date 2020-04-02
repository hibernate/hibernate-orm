/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import java.util.Map;
import javax.persistence.Subgraph;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
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
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
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
	private final Stack<GraphImplementor> graphStack = new StandardStack<>();

	// Queue containing attributes being visited, used eventually to determine the fetch strategy.
	private final Stack<AttributeNodeImplementor> attributeStack = new StandardStack<>();

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
		graphStack.push( getRootEntityGraph() );
	}

	@Override
	public void finish() {
		super.finish();
		graphStack.pop();
		//applying a little internal stack checking
		if ( !graphStack.isEmpty() || !attributeStack.isEmpty() ) {
			throw new WalkingException(
					"Internal stack error [" + graphStack.depth() + ", " + attributeStack.depth() + "]"
			);
		}
	}

	/**
	 * I'm using the NULL-OBJECT pattern here.
	 * For attributes that don't exist in the EntityGraph,
	 * a predefined NULL-ATTRIBUTE-NODE is pushed to the stack.
	 *
	 * And for a nonexistent subgraph, a predefined NULL-SUBGRAPH is pushed to the stack.
	 *
	 * So, whenever we start visiting an attribute, there will be an attribute node pushed to the attribute stack,
	 * and a subgraph node pushed to the graph stack.
	 *
	 * when we finish visiting an attribute, these two will be popped from each stack.
	 */
	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		AttributeNodeImplementor<?> attributeNode = null;
		GraphImplementor<?> subGraphNode = null;

		final GraphImplementor<?> currentGraph = graphStack.getCurrent();
		if ( currentGraph != null ) {
			final String attrName = attributeDefinition.getName();
			attributeNode = currentGraph.findAttributeNode( attrName );

			if ( attributeNode != null ) {
				//here we need to check if there is a subgraph (or sub key graph if it is an indexed attribute )
				Map<Class, Subgraph> subGraphs = attributeNode.getSubgraphs();
				Class javaType = attributeDefinition.getType().getReturnedClass();
				if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
					subGraphNode = (GraphImplementor) subGraphs.get( javaType );
				}
			}
		}

		attributeStack.push( attributeNode );
		graphStack.push( subGraphNode );

		return super.startingAttribute( attributeDefinition );
	}


	@Override
	public void finishingAttribute(final AttributeDefinition attributeDefinition) {
		attributeStack.pop();
		graphStack.pop();

		super.finishingAttribute( attributeDefinition );
	}


	@Override
	public void startingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		AttributeNodeImplementor<?> attributeNode = attributeStack.getCurrent();
		GraphImplementor<?> subGraphNode = null;

		if ( attributeNode != null ) {
			Class javaType = elementDefinition.getType().getReturnedClass();
			Map<Class, Subgraph> subGraphs = attributeNode.getSubgraphs();
			if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
				subGraphNode = (GraphImplementor) subGraphs.get( javaType );
			}
		}

		graphStack.push( subGraphNode );

		super.startingCollectionElements( elementDefinition );
	}

	@Override
	public void finishingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		super.finishingCollectionElements( elementDefinition );
		graphStack.pop();
	}


	@Override
	public void startingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		AttributeNodeImplementor attributeNode = attributeStack.getCurrent();
		GraphImplementor subGraphNode = null;

		Map<Class, Subgraph> subGraphs = attributeNode.getKeySubgraphs();
		Class javaType = indexDefinition.getType().getReturnedClass();
		if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
			subGraphNode = (GraphImplementor) subGraphs.get( javaType );
		}

		graphStack.push( subGraphNode );

		super.startingCollectionIndex( indexDefinition );
	}

	@Override
	public void finishingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		super.finishingCollectionIndex( indexDefinition );
		graphStack.pop();
	}


	@Override
	protected boolean supportsRootCollectionReturns() {
		//entity graph doesn't support root collection.
		return false;
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
		final AttributeNodeImplementor currentAttrNode = attributeStack.getCurrent();
		return currentAttrNode != null
				? DEFAULT_EAGER
				: resolveImplicitFetchStrategyFromEntityGraph( attributeDefinition );
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

	abstract protected GraphImplementor getRootEntityGraph();

	@Override
	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition) {
		final FetchStrategy fetchStrategy = determineFetchStrategy( attributeDefinition );
		if ( fetchStrategy.getStyle() != FetchStyle.JOIN ) {
			return; // nothing to do
		}

		// Bi-directional association & the owning side was already visited.  If the current attribute node refers
		// to it, fetch.
		// ENTITY nature handled by super.
		final GraphImplementor currentGraph = graphStack.getCurrent();
		if ( attributeDefinition.getAssociationNature() == AssociationAttributeDefinition.AssociationNature.COLLECTION
				&& currentGraph != null
				&& currentGraph.findAttributeNode( attributeDefinition.getName() ) != null ) {
			currentSource().buildCollectionAttributeFetch( attributeDefinition, fetchStrategy );
		}
		
		super.foundCircularAssociation( attributeDefinition );
	}
}
