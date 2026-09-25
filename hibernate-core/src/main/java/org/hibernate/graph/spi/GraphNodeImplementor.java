package org.hibernate.graph.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.graph.GraphNode;

/**
 * Integration version of the {@link GraphNode} contract
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public interface GraphNodeImplementor<J> extends GraphNode<J> {
	@Override
	@Nonnull
	GraphNodeImplementor<J> makeCopy(boolean mutable);
}
