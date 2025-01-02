/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.List;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;

/**
 * Represents a {@link jakarta.persistence.metamodel.ManagedType managed type} in an
 * {@linkplain Graph entity graph}, acting as a container for:
 * <ul>
 * <li>{@link AttributeNode} references representing fetched attributes, and
 * <li><em>treated subgraphs</em>, each represented by a child instance of
 *     {@link SubGraph}.
 * </ul>
 * <p>
 * A treated (narrowed) subgraph allows fetching to be specified for any attribute of
 * any subtype of the type represented by this graph. The usual way to create a treated
 * subgraph is by calling {@link jakarta.persistence.EntityGraph#addTreatedSubgraph(Class)}
 * or {@link #addTreatedSubGraph(Class)}. There are various shortcut operations such as
 * {@link jakarta.persistence.EntityGraph#addTreatedSubgraph(Attribute, Class)} and
 * {@link #addSubGraph(PersistentAttribute, Class)} which combine creation of a subgraph
 * with creation of a treated subgraph.
 * <p>
 * Extends the JPA-defined {@link jakarta.persistence.Graph} with additional operations.
 *
 * @apiNote Historically, both {@link jakarta.persistence.EntityGraph} and this interface
 * declared operations with incorrect generic types, leading to unsound code. This was
 * rectified in JPA 3.2 and Hibernate 7, with possible breakage to older code.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @author Gavin King
 *
 * @see RootGraph
 * @see SubGraph
 * @see jakarta.persistence.EntityGraph
 * @see jakarta.persistence.Subgraph
 */
public interface Graph<J> extends GraphNode<J>, jakarta.persistence.Graph<J> {

	/**
	 * The {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
	 * of the node.
	 *
	 * @return the {@code ManagedType} being graphed here.
	 */
	ManagedDomainType<J> getGraphedType();

	/**
	 * Create a named {@linkplain RootGraph root graph} representing this node.
	 *
	 * @param mutable controls whether the resulting graph is mutable
	 *
	 * @throws CannotBecomeEntityGraphException If the named attribute is not entity-valued
	 *
	 * @deprecated This will be removed
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	RootGraph<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	/**
	 * Create a new {@linkplain SubGraph subgraph} representing this node.
	 *
	 * @deprecated This will be removed
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SubGraph<J> makeSubGraph(boolean mutable);

	/**
	 * Make a copy of this graph node, with the given mutability.
	 * <p>
	 * If this graph is immutable, and the argument is {@code false},
	 * simply return this instance.
	 */
	@Override
	Graph<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNodes

	/**
	 * All {@linkplain AttributeNode nodes} belonging to this container.
	 *
	 * @see #getAttributeNodes
	 */
	List<? extends AttributeNode<?>> getAttributeNodeList();

	/**
	 * Find an already existing AttributeNode by attributeName within
	 * this container
	 *
	 * @see #getAttributeNode(String)
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(String attributeName);

	/**
	 * Find an already existing AttributeNode by corresponding attribute
	 * reference, within this container.
	 *
	 * @see #getAttributeNode(Attribute)
	 */
	<AJ> AttributeNode<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	default <Y> AttributeNode<Y> getAttributeNode(String attributeName) {
		return findAttributeNode( attributeName );
	}

	@Override
	default <Y> AttributeNode<Y> getAttributeNode(Attribute<? super J, Y> attribute) {
		return findAttributeNode( (PersistentAttribute<? super J, Y>) attribute );
	}

	@Override
	default boolean hasAttributeNode(String attributeName) {
		return getAttributeNode( attributeName ) != null;
	}

	@Override
	default boolean hasAttributeNode(Attribute<? super J, ?> attribute) {
		return getAttributeNode( attribute ) != null;
	}

	/**
	 * Add an {@link AttributeNode} representing the given {@link PersistentAttribute}
	 * to this node of the graph without creating any associated {@link SubGraph}.
	 *
	 * @see #addAttributeNode(Attribute)
	 */
	<AJ> AttributeNode<AJ> addAttributeNode(PersistentAttribute<? super J,AJ> attribute);

	@Override
	default <Y> AttributeNode<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
		return addAttributeNode( (PersistentAttribute<? super J,Y>) attribute );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Subgraphs

	/**
	 * Create and return a new (mutable) {@link SubGraph} representing
	 * the given subtype of the type of this node, or return an existing
	 * such {@link SubGraph} if there is one.
	 *
	 * @see jakarta.persistence.EntityGraph#addTreatedSubgraph(Class)
	 */
	<Y extends J> SubGraph<Y> addTreatedSubGraph(Class<Y> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} representing
	 * the given subtype of the type of this node, or return an existing
	 * such {@link SubGraph} if there is one.
	 */
	<Y extends J> SubGraph<Y> addTreatedSubGraph(ManagedDomainType<Y> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @see #addSubgraph(String)
	 */
	@Deprecated
	<AJ> SubGraph<AJ> addSubGraph(String attributeName)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, and with the given type, which may be
	 * a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @see #addSubgraph(String, Class)
	 */
	<AJ> SubGraph<AJ> addSubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link PersistentAttribute}, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @see #addSubgraph(Attribute)
	 */
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link PersistentAttribute}, and with the given type,
	 * which may be a subtype of the attribute type, or return an existing
	 * such {@link SubGraph} if there is one.
	 *
	 * @see #addSubgraph(Attribute, Class)
	 */
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link PersistentAttribute}, and with the given type,
	 * which may be a subtype of the attribute type, or return an existing
	 * such {@link SubGraph} if there is one.
	 */
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, ManagedDomainType<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, and with the given type, which
	 * may be a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @see #addTreatedElementSubgraph(PluralAttribute, Class)
	 */
	<AJ> SubGraph<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, and with the given type, which
	 * may be a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 */
	<AJ> SubGraph<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, ManagedDomainType<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @see #addKeySubgraph(String)
	 */
	@Deprecated
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @see #addKeySubgraph(String, Class)
	 */
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @see #addTreatedMapKeySubgraph(MapAttribute, Class)
	 */
	<AJ> SubGraph<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 */
	<AJ> SubGraph<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, ManagedDomainType<AJ> type)
			throws CannotContainSubGraphException;

	@Override
	default <Y> SubGraph<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		return addSubGraph( (PersistentAttribute<? super J, ? super Y>) attribute ).addTreatedSubGraph( type );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(Attribute<? super J, X> attribute) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute );
	}

	@Override
	default <X> SubGraph<? extends X> addSubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute ).addTreatedSubGraph( type );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name) {
		return addSubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addSubgraph(String name, Class<X> type) {
		return addSubGraph( name ).addTreatedSubGraph( type );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name) {
		return addKeySubGraph( name );
	}

	@Override
	default <X> SubGraph<X> addKeySubgraph(String name, Class<X> type) {
		return addKeySubGraph( name ).addTreatedSubGraph( type );
	}

	/**
	 * Add a subgraph rooted at a plural attribute, allowing further nodes
	 * to be added to the subgraph.
	 *
	 * @apiNote {@link #addElementSubgraph(PluralAttribute)} was added
	 *          in JPA 3.2, and so this method is no longer really needed
	 *
	 * @since 6.3
	 *
	 * @see #addElementSubgraph(PluralAttribute)
	 */
	default <AJ> SubGraph<AJ> addPluralSubgraph(PluralAttribute<? super J, ?, AJ> attribute) {
		return addSubGraph( attribute.getName(), attribute.getBindableJavaType() );
	}

	@Override @Deprecated(forRemoval = true)
	default <X> SubGraph<X> addKeySubgraph(Attribute<? super J, X> attribute) {
		throw new UnsupportedOperationException( "This operation will be removed in JPA 4" );
	}

	@Override @Deprecated(forRemoval = true)
	default <X> SubGraph<? extends X> addKeySubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		throw new UnsupportedOperationException( "This operation will be removed in JPA 4" );
	}
}
