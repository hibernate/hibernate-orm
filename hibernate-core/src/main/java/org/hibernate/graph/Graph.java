/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

/**
 * A container for {@link AttributeNode}s.
 *
 * Acts as a "bridge" between JPA's {@link javax.persistence.EntityGraph} and {@link javax.persistence.Subgraph}
 *
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Graph<J> extends GraphNode<J> {
	/**
	 * Graphs apply only to ManagedTypes.  Returns the ManagedType being graphed here.
	 */
	ManagedType<J> getGraphedType();

	/**
	 * Create a named (if passed `name` != null) root Graph.  The `mutable`
	 * parameter controls whether the created Graph is mutable.
	 *
	 * @throws CannotBecomeEntityGraphException For named attributes
	 * that are not entity valued
	 */
	RootGraph<J> makeRootGraph(String name, boolean mutable) throws CannotBecomeEntityGraphException;

	/**
	 * Create a (mutable/immutable) SubGraph based on this Graph
	 */
	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	Graph<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling

	/**
	 * Ultimately only needed for implementing {@link javax.persistence.EntityGraph#getAttributeNodes()}
	 * and {@link javax.persistence.Subgraph#getAttributeNodes()}
	 */
	List<AttributeNode<?>> getGraphAttributeNodes();

	/**
	 * Find an already existing AttributeNode by attributeName within this
	 * container
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(String attributeName);

	/**
	 * Find an already existing AttributeNode by corresponding attribute
	 * reference, within this container
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(Attribute<? extends J, AJ> attribute);

	/**
	 * Get a list of all existing AttributeNodes within this container
	 */
	List<AttributeNode<?>> getAttributeNodeList();

	/**
	 * Add an AttributeNode (with no associated SubGraphNodes) to this container
	 * by attribute name
	 */
	<AJ> AttributeNode<AJ> addAttributeNode(String attributeName);

	/**
	 * Add an AttributeNode (with no associated SubGraphNode) to this container
	 * by Attribute reference
	 */
	<AJ> AttributeNode<AJ> addAttributeNode(Attribute<? extends J,AJ> attribute);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sub graph nodes

	/**
	 * Create a (mutable) SubGraphNode associated with the named AttributeNode.
	 * The created SubGraphNode is returned
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(String attributeName) throws CannotContainSubGraphException;

	<AJ> SubGraph<AJ> addSubGraph(String attributeName, Class<AJ> type) throws CannotContainSubGraphException;

	/**
	 * Create a (mutable) SubGraphNode associated with the AttributeNode for the given
	 * Attribute.  The created SubGraphNode is returned
	 *
	 * @apiNote If no such AttributeNode exists yet, it is created.
	 */
	<AJ> SubGraph<AJ> addSubGraph(Attribute<? extends J, AJ> attribute) throws CannotContainSubGraphException;

	<AJ> SubGraph<? extends AJ> addSubGraph(Attribute<? extends J, AJ> attribute, Class<? extends AJ> type) throws CannotContainSubGraphException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// key sub graph nodes

	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName) throws CannotContainSubGraphException;
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type) throws CannotContainSubGraphException;

	<AJ> SubGraph<AJ> addKeySubGraph(Attribute<? extends J,AJ> attribute) throws CannotContainSubGraphException;
	<AJ> SubGraph<? extends AJ> addKeySubGraph(Attribute<? extends J,AJ> attribute, Class<? extends AJ> type) throws CannotContainSubGraphException;
}
