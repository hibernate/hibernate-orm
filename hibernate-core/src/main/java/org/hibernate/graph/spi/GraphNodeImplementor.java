/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.GraphNode;

/**
 * Integration version of the GraphNode contract
 *
 * @author Steve Ebersole
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
public interface GraphNodeImplementor<J> extends GraphNode<J> {
	@Override
	GraphNodeImplementor<J> makeCopy(boolean mutable);
}
