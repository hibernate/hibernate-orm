/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph;

import java.util.List;

import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * A container for {@link AttributeNode} references.
 * <p>
 * Acts as a "bridge" between JPA's {@link jakarta.persistence.EntityGraph}
 * and {@link jakarta.persistence.Subgraph}.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface Graph<J> extends GraphNode<J> {

	/**
	 * Add a subgraph rooted at a plural attribute, allowing
	 * further nodes to be added to the subgraph.
	 *
	 * @apiNote This method is missing in JPA, and nodes cannot be
	 *          added in a typesafe way to subgraphs representing
	 *          fetched collections
	 *
	 * @since 6.3
	 */
	default <AJ> SubGraph<AJ> addPluralSubgraph(PluralAttribute<? extends J, ?, AJ> attribute) {
		return addSubGraph( attribute.getName() );
	}

	/**
	 * Graphs apply only to {@link jakarta.persistence.metamodel.ManagedType}s.
	 *
	 * @return the {@code ManagedType} being graphed here.
	 */
	ManagedDomainType<J> getGraphedType();

	/**
	 * Create a named root {@link Graph} if the given name is not null.
	 *
	 * @param mutable controls whether the resulting {@code Graph} is mutable
	 *
	 * @throws CannotBecomeEntityGraphException If the named attribute is not entity-valued
	 */
	RootGraph<J> makeRootGraph(String name, boolean mutable) throws CannotBecomeEntityGraphException;

	/**
	 * Create a (mutable or immutable) {@link SubGraph} rooted at this {@link Graph}.
	 */
	SubGraph<J> makeSubGraph(boolean mutable);

	@Override
	Graph<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling

	/**
	 * Ultimately only needed for implementing {@link jakarta.persistence.EntityGraph#getAttributeNodes()}
	 * and {@link jakarta.persistence.Subgraph#getAttributeNodes()}
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
	<AJ> AttributeNode<AJ> findAttributeNode(PersistentAttribute<? extends J, AJ> attribute);

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
	<AJ> AttributeNode<AJ> addAttributeNode(PersistentAttribute<? extends J,AJ> attribute);



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
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? extends J, AJ> attribute) throws CannotContainSubGraphException;

	<AJ> SubGraph<? extends AJ> addSubGraph(PersistentAttribute<? extends J, AJ> attribute, Class<? extends AJ> type) throws CannotContainSubGraphException;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// key sub graph nodes

	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName) throws CannotContainSubGraphException;
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type) throws CannotContainSubGraphException;

	<AJ> SubGraph<AJ> addKeySubGraph(PersistentAttribute<? extends J,AJ> attribute) throws CannotContainSubGraphException;
	<AJ> SubGraph<? extends AJ> addKeySubGraph(PersistentAttribute<? extends J,AJ> attribute, Class<? extends AJ> type) throws CannotContainSubGraphException;
}
