/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.sql.results.spi.LoadingEntityEntry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {
	private static final Logger log = Logger.getLogger( JdbcValuesSourceProcessingStateStandardImpl.class );

	private final ExecutionContext executionContext;
	private final JdbcValuesSourceProcessingOptions processingOptions;

	private Map<EntityKey,LoadingEntityEntry> loadingEntityMap;
	private Map<CollectionKey,LoadingCollectionEntry> loadingCollectionMap;

	private FetchContext fetchContext;

	private final PreLoadEvent preLoadEvent;
	private final PostLoadEvent postLoadEvent;

	// todo (6.0) : "loading collections" as well?

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions) {
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;

		preLoadEvent  = new PreLoadEvent( (EventSource) executionContext.getSession() );
		postLoadEvent  = new PostLoadEvent( (EventSource) executionContext.getSession() );

		fetchContext = resolveFetchContext( executionContext.getQueryOptions().getAppliedGraph() );
	}

	private FetchContext resolveFetchContext(AppliedGraph appliedGraph) {
		if ( appliedGraph != null ) {
			return new FetchContextImpl( appliedGraph );
		}

		return null;
	}

	@Override
	public ExecutionContext getExecutionContext() {
		return executionContext;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public JdbcValuesSourceProcessingOptions getProcessingOptions() {
		return processingOptions;
	}

	@Override
	public PreLoadEvent getPreLoadEvent() {
		return preLoadEvent;
	}

	@Override
	public PostLoadEvent getPostLoadEvent() {
		return postLoadEvent;
	}

	@Override
	public boolean fetching(PersistentAttributeDescriptor attribute) {
		if ( fetchContext == null ) {
			return true;
		}
		return fetchContext.fetching( attribute );
	}

	public interface FetchContext {
		boolean fetching(PersistentAttributeDescriptor attribute);
	}

	private class FetchContextImpl implements FetchContext {
		private final GraphSemantic graphSemantic;

		private GraphImplementor currentContainer;

		public FetchContextImpl(AppliedGraph appliedGraph) {
			this( appliedGraph.getGraph(), appliedGraph.getSemantic() );
		}

		public FetchContextImpl(RootGraphImplementor<?> appliedGraph, GraphSemantic graphSemantic) {
			currentContainer = appliedGraph;
			this.graphSemantic = graphSemantic;
		}

		@Override
		public boolean fetching(PersistentAttributeDescriptor attribute) {
			if ( ! Fetchable.class.isInstance( attribute ) ) {
				// basic attributes are always fetched for now - that matches behavior of
				// Hibernate prior to 6.  Eventually though we want to hook this in with
				// bytecode-enhanced laziness
				return true;
			}

			// if there is an entity-graph, see if it says we should eagerly load the attribute
			// else see if the attribute is configured for join fetching
			//
			// ^^ so long as we do not exceed max-fetch-depth

			final AttributeNodeImplementor attributeNode = currentContainer.findAttributeNode( attribute.getAttributeName() );

//			if ( entityGraphSaysToEagerLoad( attributeNode ) ) {
//				prepareForFetch( attributeNode )
//			}
//
//			if ( attributeNode == null ) {
//				return false;
//			}
//
//			final Map subGraphs = attributeNode.subGraphs();
//			if ( subGraphs == null || subGraphs.isEmpty() ) {
//				return
//			}
//			if ( !shouldFetch ) {
//				return false;
//			}
//
////			final Fetchable fetchable = (Fetchable) attribute;
////			fetchable.generateFetch( ... )
//
//
//			final AttributeNodeImplementor attributeNode = (fetchNodeStack.getCurrent().findAttributeNode( attribute.getAttributeName() )fetchNodeStack.getCurrent().findAttributeNode( attribute.getAttributeName() );
//			fetchNodeStack.push( . );

			return false;
		}
	}

	@Override
	public void registerLoadingEntity(
			EntityKey entityKey,
			LoadingEntityEntry loadingEntry) {
		if ( loadingEntityMap == null ) {
			loadingEntityMap = new HashMap<>();
		}

		loadingEntityMap.put( entityKey, loadingEntry );
	}

	@Override
	public LoadingEntityEntry findLoadingEntityLocally(EntityKey entityKey) {
		return loadingEntityMap == null ? null : loadingEntityMap.get( entityKey );
	}

	@Override
	public LoadingCollectionEntry findLoadingCollectionLocally(CollectionKey key) {
		if ( loadingCollectionMap == null ) {
			return null;
		}

		return loadingCollectionMap.get( key );
	}

	@Override
	public void registerLoadingCollection(CollectionKey key, LoadingCollectionEntry loadingCollectionEntry) {
		if ( loadingCollectionMap == null ) {
			loadingCollectionMap = new HashMap<>();
		}

		loadingCollectionMap.put( key, loadingCollectionEntry );
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return executionContext.getSession();
	}

	@Override
	public void finishUp() {
		try {
			// for arrays, we should end the collection load beforeQuery resolving the entities, since the
			// actual array instances are not instantiated during loading
			finishLoadingArrays();

			// now finish loading the entities (2-phase load)
			performTwoPhaseLoad();

			// now we can finalize loading collections
			finishLoadingCollections();
		}
		finally {
			executionContext.getSession().getPersistenceContext().getLoadContexts().deregister( this );
		}
	}

	private void finishLoadingArrays() {
//		for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
//			arrayReferenceInitializer.endLoading( context );
//		}
	}


	private void performTwoPhaseLoad() {
		if ( loadingEntityMap == null ) {
			return;
		}

		log.tracev( "Total objects hydrated: {0}", loadingEntityMap.size() );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isReadOnly() {
		if ( getQueryOptions().isReadOnly() != null ) {
			return getQueryOptions().isReadOnly();
		}

		if ( executionContext.getSession() instanceof EventSource ) {
			return executionContext.getSession().isDefaultReadOnly();
		}

		return false;
	}


	private void finishLoadingCollections() {
		if ( loadingCollectionMap != null ) {
			for ( LoadingCollectionEntry loadingCollectionEntry : loadingCollectionMap.values() ) {
				loadingCollectionEntry.finishLoading( getExecutionContext() );
			}

			loadingCollectionMap.clear();
		}
	}

}
