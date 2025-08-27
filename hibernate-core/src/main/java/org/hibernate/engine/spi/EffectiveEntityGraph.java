/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Think of this as the composite modeling of a graph and the semantic.
 * <p>
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

	private @Nullable GraphSemantic semantic;
	private @Nullable RootGraphImplementor<?> graph;

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
	public EffectiveEntityGraph(boolean allowOverwrite) {
		this.allowOverwrite = allowOverwrite;
	}

	@Override
	public @Nullable GraphSemantic getSemantic() {
		return semantic;
	}

	@Override
	public @Nullable RootGraphImplementor<?> getGraph() {
		return graph;
	}

	/**
	 * Apply the graph and semantic.  The semantic is required.  The graph
	 * may be null, but that should generally be considered mis-use.
	 *
	 * @throws IllegalArgumentException Thrown if the semantic is null
	 * @throws IllegalStateException If previous state is still available (hasn't been cleared).
	 */
	public void applyGraph(RootGraphImplementor<?> graph, @Nullable GraphSemantic semantic) {
		if ( semantic == null ) {
			throw new IllegalArgumentException( "Graph semantic cannot be null" );
		}

		verifyWriteability();

		log.tracef( "Setting effective graph state [%s] : %s", semantic.name(), graph );

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
	 * <p>
	 * The semantic is required.  The graph
	 * may be null, but that should generally be considered mis-use.
	 *
	 * @throws IllegalArgumentException If both kinds of graphs were present in the properties/hints
	 * @throws IllegalStateException If previous state is still available (hasn't been cleared).
	 */
	public void applyConfiguredGraph(@Nullable Map<String,?> properties) {
		if ( properties == null || properties.isEmpty() ) {
			return;
		}

		var fetchHint = (RootGraphImplementor<?>) properties.get( GraphSemantic.FETCH.getJpaHintName() );
		var loadHint = (RootGraphImplementor<?>) properties.get( GraphSemantic.LOAD.getJpaHintName() );
		if ( fetchHint == null ) {
			fetchHint = (RootGraphImplementor<?>) properties.get( GraphSemantic.FETCH.getJakartaHintName() );
		}
		if ( loadHint == null ) {
			loadHint = (RootGraphImplementor<?>) properties.get( GraphSemantic.LOAD.getJakartaHintName() );
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
		else if ( loadHint != null ) {
			applyGraph( loadHint, GraphSemantic.LOAD );
		}
	}

	public void clear() {
		this.semantic = null;
		this.graph = null;
	}
}
