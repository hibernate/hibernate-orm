/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.GraphSemantic;

/**
 * Contract for anything a fetch/load graph can be applied
 *
 * @author Steve Ebersole
 */
public interface AppliedGraph {
	/**
	 * The applied graph
	 */
	RootGraphImplementor<?> getGraph();

	/**
	 * The semantic (fetch/load) under which the graph should be applied
	 */
	GraphSemantic getSemantic();
}
