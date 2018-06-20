/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.graph;

import java.util.Iterator;
import java.util.List;

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

	private <R> R run(Session session, Supplier<R> supplier) {
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

	public Iterator<T> iterate(Session session, final org.hibernate.Query query) {
		return run( session, new Supplier<Iterator<T>>() {
			@SuppressWarnings("unchecked")
			public Iterator<T> get() {
				return query.iterate();
			}
		});
	}

	public ScrollableResults scroll(Session session, final org.hibernate.Query query) {
		return run( session, new Supplier<ScrollableResults>() {
			public ScrollableResults get() {
				return query.scroll();
			}
		});
	}

	public boolean hasFetchGraph() {
		return fetchGraph != null;
	}

	public boolean hasLoadGraph() {
		return loadGraph != null;
	}
	
	private interface Supplier<T> {
		public T get();
	}
}
