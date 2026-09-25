package org.hibernate.graph;

/**
 * Common operations of {@link AttributeNode} and {@link Graph}.
 *
 * @author Steve Ebersole
 *
 * @see AttributeNode
 * @see Graph
 */
public interface GraphNode<J> {
	boolean isMutable();

	GraphNode<J> makeCopy(boolean mutable);
}
