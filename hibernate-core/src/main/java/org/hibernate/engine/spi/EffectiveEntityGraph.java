/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;

import org.jboss.logging.Logger;

/**
 * Think of this as the composite modeling of a graph
 * and the semantic.
 *
 * Its graph and semantic can be obtained by {@link #getGraph()} and
 * {@link #getSemantic()}
 *
 * They can be managed by calls to {@link #applyGraph}, {@link #applyConfiguredGraph}
 * and {@link #clear}
 *
 * @author Steve Ebersole
 */
public class EffectiveEntityGraph implements AppliedGraph, Serializable {
	private static final Logger log = Logger.getLogger( EffectiveEntityGraph.class );

	private final boolean allowOverwrite;

	private GraphSemantic semantic;
	private RootGraphImplementor<?> graph;

	/**
	 * @implSpec I explicitly made this constructor package protected
	 * because we may need to pass in the SessionFactory or JpaCompliance
	 * etc to be able to know what to do in {@link #applyConfiguredGraph(Map)}
	 * when the incoming properties contain both a
	 * {@link GraphSemantic#FETCH} and a {@link GraphSemantic#LOAD}.
	 * In other words, we may need to add some constructor argument here so
	 * we want to control what can call it
	 */
	@Incubating
	public EffectiveEntityGraph() {
		this( false );
	}

	/**
	 * @implSpec See {@link #EffectiveEntityGraph}
	 */
	@Incubating
	@SuppressWarnings("WeakerAccess")
	public EffectiveEntityGraph(boolean allowOverwrite) {
		this.allowOverwrite = allowOverwrite;
	}

	@Override
	public GraphSemantic getSemantic() {
		return semantic;
	}

	@Override
	public RootGraphImplementor<?> getGraph() {
		return graph;
	}

	/**
	 * Apply the graph and semantic.  The semantic is required.  The graph
	 * may be null, but that should generally be considered mis-use.
	 *
	 * @throws IllegalArgumentException Thrown if the semantic is null
	 * @throws IllegalStateException If previous state is still available (hasn't been cleared).
	 */
	public void applyGraph(RootGraphImplementor<?> graph, GraphSemantic semantic) {
		if ( semantic == null ) {
			throw new IllegalArgumentException( "Graph semantic cannot be null" );
		}

		verifyWriteability();

		log.debugf( "Setting effective graph state [%s] : %s", semantic.name(), graph );

		this.semantic = semantic;
		this.graph = graph;
	}

	private void verifyWriteability() {
		if ( ! allowOverwrite ) {
			if ( semantic != null ) {
				throw new IllegalStateException( "Cannot overwrite existing state, should clear previous state first" );
			}
		}
	}

	/**
	 * Apply a graph and semantic based on configuration properties or hints
	 * based on {@link GraphSemantic#getJpaHintName()} for {@link GraphSemantic#LOAD} or
	 * {@link GraphSemantic#FETCH}.
	 *
	 * The semantic is required.  The graph
	 * may be null, but that should generally be considered mis-use.
	 *
	 * @throws IllegalArgumentException If both kinds of graphs were present in the properties/hints
	 * @throws IllegalStateException If previous state is still available (hasn't been cleared).
	 */
	public void applyConfiguredGraph(Map<String,?> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}

		final RootGraphImplementor fetchHint = (RootGraphImplementor) properties.get( GraphSemantic.FETCH.getJpaHintName() );
		final RootGraphImplementor loadHint = (RootGraphImplementor) properties.get( GraphSemantic.LOAD.getJpaHintName() );

		if ( fetchHint == null && loadHint == null ) {
			log.debugf( "Neither LOAD nor FETCH graph were found in properties" );
			return;
		}

		if ( fetchHint != null ) {
			if ( loadHint != null ) {
				// can't have both
				throw new IllegalArgumentException(
						"Passed properties contained both a LOAD and a FETCH graph which is illegal - " +
								"only one should be passed"
				);
			}

			applyGraph( fetchHint, GraphSemantic.FETCH );
		}
		else {
			applyGraph( loadHint, GraphSemantic.LOAD );
		}
	}

	public void clear() {
		this.semantic = null;
		this.graph = null;
	}
}
