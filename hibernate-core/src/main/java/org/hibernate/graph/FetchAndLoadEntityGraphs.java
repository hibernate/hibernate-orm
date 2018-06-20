/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Internally used class to carry a pair of graphs (fetch and load), 
 * a result of {@link EntityGraphParser#parsePreQueryGraphDescriptors(EntityManager, Class, ParseBuffer)}.
 * 
 * @author asusnjar
 *
 * @param <T> Root entity type of both graphs.
 */
class FetchAndLoadEntityGraphs<T> {

	final EntityGraph<T> fetchGraph;
	final EntityGraph<T> loadGraph;

	public FetchAndLoadEntityGraphs(EntityGraph<T> fetchGraph, EntityGraph<T> loadGraph) {
		this.fetchGraph = fetchGraph;
		this.loadGraph = loadGraph;
	}

	public FetchAndLoadEntityGraphs(EntityManager em, Class<T> rootType, CharSequence fetchGraph, CharSequence loadGraph) {
		this( EntityGraphParser.parse( em, rootType, fetchGraph ), EntityGraphParser.parse( em, rootType, loadGraph ) );
	}

	public FetchAndLoadEntityGraphs(EntityManager em, Class<T> rootType, CharSequence fetchGraph) {
		this( em, rootType, fetchGraph, null );
	}

	public EntityGraph<T> getFetchGraph() {
		return fetchGraph;
	}

	public EntityGraph<T> getLoadGraph() {
		return loadGraph;
	}

	public void applyTo(Query query) {
		if ( fetchGraph != null ) {
			query.setHint( EntityGraphs.HINT_FETCHGRAPH, fetchGraph );
		}
		if ( loadGraph != null ) {
			query.setHint( EntityGraphs.HINT_LOADGRAPH, loadGraph );
		}
	}

	public <R> R run(Session session, Supplier<R> supplier) {
		LoadQueryInfluencers influencers = ( (SessionImplementor) session ).getLoadQueryInfluencers();
		EntityGraph<?> preexistingFetchGraph = influencers.getFetchGraph();
		EntityGraph<?> preexistingLoadGraph = influencers.getLoadGraph();

		try {
			influencers.setFetchGraph( fetchGraph );
			influencers.setLoadGraph( loadGraph );
			return supplier.get();
		}
		finally {
			influencers.setFetchGraph( preexistingFetchGraph );
			influencers.setLoadGraph( preexistingLoadGraph );
		}
	}

	/**
	 * @deprecated  as {@link org.hibernate.Query} is deprecated.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Deprecated
	public List<T> list(Session session, @SuppressWarnings("rawtypes") org.hibernate.Query query) {
		return (List<T>) run( session, () -> query.list() );
	}

	/**
	 * @deprecated  as {@link org.hibernate.Query} is deprecated.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Deprecated
	public Iterator<T> iterate(Session session, @SuppressWarnings("rawtypes") org.hibernate.Query query) {
		return (Iterator<T>) run( session, () -> query.iterate() );
	}

	/**
	 * @deprecated  as {@link org.hibernate.Query} is deprecated.
	 */
	@SuppressWarnings("deprecation")
	@Deprecated
	public ScrollableResults scroll(Session session, @SuppressWarnings("rawtypes") org.hibernate.Query query) {
		return run( session, () -> query.scroll() );
	}

	public boolean hasFetchGraph() {
		return fetchGraph != null;
	}

	public boolean hasLoadGraph() {
		return loadGraph != null;
	}
}
