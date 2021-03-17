/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

/**
 * Commonality between {@link AttributeNode} and
 * {@link Graph}.
 *
 * @author Steve Ebersole
 */
public interface GraphNode<J> {
	boolean isMutable();

	GraphNode<J> makeCopy(boolean mutable);
}
