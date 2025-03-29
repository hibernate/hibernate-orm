/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import java.util.List;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Represents a {@linkplain jakarta.persistence.metamodel.ManagedType managed type} in an
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
 * or {@link #addTreatedSubgraph(Class)}. There are various shortcut operations such as
 * {@link jakarta.persistence.EntityGraph#addTreatedSubgraph(Attribute, Class)} which
 * combine creation of a subgraph with creation of a treated subgraph.
 * <p>
 * Extends the JPA-defined {@link jakarta.persistence.Graph} with additional operations.
 * <p>
 * There are a range of ways to define {@code Graph}s:
 * <ul>
 * <li>programmatically, beginning with {@link org.hibernate.Session#createEntityGraph(Class)},
 * <li>using the {@link jakarta.persistence.NamedEntityGraph @NamedEntityGraph} annotation, or
 * <li>using the mini-language understood by {@link GraphParser}.
 * </ul>
 * <p>
 * When a graph is defined programmatically, the new graph is usually instantiated by calling
 * {@link jakarta.persistence.EntityManager#createEntityGraph(Class)}. However, this requires a
 * reference to and {@code EntityManager}, which might not always be convenient. An alternative
 * is provided by {@link EntityGraphs#createGraph(jakarta.persistence.metamodel.EntityType)},
 * where the {@code EntityType} may be obtained from the static metamodel:
 * <pre>
 * EntityGraph&lt;Book&gt; graph = EntityGraphs.createGraph(Book_.class_);
 * </pre>
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
 * @see GraphParser
 * @see EntityGraphs
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
	 * Find an existing {@link AttributeNode} by name within this container.
	 *
	 * @since 7.0
	 */
	@Override
	<Y> AttributeNode<Y> getAttributeNode(String attributeName);

	/**
	 * Find an existing {@link AttributeNode} by corresponding attribute
	 * reference, within this container.
	 *
	 * @since 7.0
	 */
	@Override
	<Y> AttributeNode<Y> getAttributeNode(Attribute<? super J, Y> attribute);

	/**
	 * Find an existing {@link AttributeNode} by name within this container.
	 *
	 * @deprecated Use {@link #getAttributeNode(String)}
	 */
	@Deprecated(since = "7.0")
	<AJ> AttributeNode<AJ> findAttributeNode(String attributeName);

	/**
	 * Find an existing {@link AttributeNode} by corresponding attribute
	 * reference, within this container.
	 *
	 * @deprecated Use {@link #getAttributeNode(Attribute)}
	 */
	@Deprecated(since = "7.0")
	<AJ> AttributeNode<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	/**
	 * Add an {@link AttributeNode} representing the given {@link Attribute} to
	 * this node of the graph without creating any associated {@link SubGraph}.
	 */
	@Override
	<Y> AttributeNode<Y> addAttributeNode(Attribute<? super J, Y> attribute);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Treated subgraphs

	/**
	 * Create and return a new (mutable) {@link SubGraph} representing
	 * the given subtype of the type of this node, or return an existing
	 * such {@link SubGraph} if there is one.
	 *
	 * @see jakarta.persistence.EntityGraph#addTreatedSubgraph(Class)
	 *
	 * @since 7.0
	 */
	<Y extends J> SubGraph<Y> addTreatedSubgraph(Class<Y> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} representing
	 * the given subtype of the type of this node, or return an existing
	 * such {@link SubGraph} if there is one.
	 *
	 * @since 7.0
	 */
	@Incubating
	<Y extends J> SubGraph<Y> addTreatedSubgraph(ManagedType<Y> type);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute subgraphs

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 */
	@Override
	<X> SubGraph<X> addSubgraph(String attributeName);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, and with the given type, which may be
	 * a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 * @param type A subtype of the attribute type
	 */
	@Override
	<X> SubGraph<X> addSubgraph(String attributeName, Class<X> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 *
	 * @deprecated Use {@link #addSubgraph(String)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addSubGraph(String attributeName)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the named {@link Attribute}, and with the given type, which may be
	 * a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 * @param type A subtype of the attribute type
	 *
	 * @deprecated Use {@link #addSubgraph(String, Class)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addSubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link Attribute} of the represented type, or return an
	 * existing such {@link SubGraph} if there is one.
	 *
	 * @param attribute An attribute of the represented type
	 *
	 * @see #addSubgraph(Attribute)
	 */
	@Override
	<X> SubGraph<X> addSubgraph(Attribute<? super J, X> attribute)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link Attribute}, and with the given type, which may be
	 * a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attribute An attribute of the represented type
	 * @param type A subtype of the attribute type
	 *
	 * @see #addSubgraph(Attribute, Class)
	 *
	 * @since 7.0
	 */
	@Override
	<Y> SubGraph<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link Attribute}, and with the given type, which may be
	 * a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attribute An attribute of the represented type
	 * @param type A subtype of the attribute type
	 *
	 * @since 7.0
	 */
	@Incubating
	<AJ> SubGraph<AJ> addTreatedSubgraph(Attribute<? super J, ? super AJ> attribute, ManagedType<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link Attribute} of the represented type, or return an
	 * existing such {@link SubGraph} if there is one.
	 *
	 * @param attribute An attribute of the represented type
	 *
	 * @see #addSubgraph(Attribute)
	 *
	 * @deprecated Use {@link #addSubgraph(Attribute)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the given {@link PersistentAttribute}, and with the given type,
	 * which may be a subtype of the attribute type, or return an existing
	 * such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attribute An attribute of the represented type
	 * @param type A subtype of the attribute type
	 *
	 * @see #addSubgraph(Attribute, Class)
	 *
	 * @deprecated Use {@link #addTreatedSubgraph(Attribute, Class)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> type)
			throws CannotContainSubGraphException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Element subgraphs

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the named {@link PluralAttribute}, or return an
	 * existing such {@link SubGraph} if there is one.
	 *
	 * @param attributeName The name of a collection-valued attribute of the
	 *                      represented type
	 */
	@Override
	<X> SubGraph<X> addElementSubgraph(String attributeName);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the named {@link PluralAttribute}, and with the given
	 * type, which may be a subtype of the element type, or return an existing
	 * such {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the attribute type, the
	 * result is a treated subgraph.
	 *
	 * @param attributeName The name of a collection-valued attribute of the
	 *                      represented type
	 * @param type A subtype of the element type
	 */
	@Override
	<X> SubGraph<X> addElementSubgraph(String attributeName, Class<X> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @param attribute A collection-valued attribute of the represented type
	 *
	 * @since 7.0
	 */
	@Override
	<E> SubGraph<E> addElementSubgraph(PluralAttribute<? super J, ?, E> attribute);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, and with the given type, which
	 * may be a subtype of the element type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the element type, the result
	 * is a treated subgraph.
	 *
	 * @param attribute A collection-valued attribute of the represented type
	 * @param type A subtype of the element type
	 *
	 * @since 7.0
	 */
	@Override
	<E> SubGraph<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, and with the given type, which
	 * may be a subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the element type, the result
	 * is a treated subgraph.
	 *
	 * @param attribute A collection-valued attribute of the represented type
	 * @param type A subtype of the element type
	 *
	 * @since 7.0
	 */
	@Incubating
	<AJ> SubGraph<AJ> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super AJ> attribute, ManagedType<AJ> type)
			throws CannotContainSubGraphException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Key subgraphs

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 */
	@Override
	<X> SubGraph<X> addKeySubgraph(String attributeName);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @param attributeName The name of a map-valued attribute of the
	 *                     represented type
	 * @param type A subtype of the key type
	 */
	@Override
	<X> SubGraph<X> addKeySubgraph(String attributeName, Class<X> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @param attributeName The name of an attribute of the represented type
	 *
	 * @deprecated Use {@link #addKeySubgraph(String)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param attributeName The name of a map-valued attribute of the
	 *                      represented type
	 * @param type A subtype of the key type
	 *
	 * @deprecated Use {@link #addKeySubgraph(String, Class)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addKeySubGraph(String attributeName, Class<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, or return an existing such {@link SubGraph}
	 * if there is one.
	 *
	 * @param attribute A map-valued attribute of the represented type
	 */
	@Override
	<K> SubGraph<K> addMapKeySubgraph(MapAttribute<? super J, K, ?> attribute);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param attribute A map-valued attribute of the represented type
	 * @param type A subtype of the key type
	 */
	@Override
	<K> SubGraph<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type);

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param attribute A map-valued attribute of the represented type
	 * @param type A subtype of the key type
	 *
	 * @since 7.0
	 */
	@Incubating
	<AJ> SubGraph<AJ> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super AJ, ?> attribute, ManagedType<AJ> type)
			throws CannotContainSubGraphException;

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the key of the named map, and with the given type, which may be a
	 * subtype of the attribute type, or return an existing such
	 * {@link SubGraph} if there is one.
	 * <p>
	 * If the given type is a proper subtype of the key type, the result
	 * is a treated subgraph.
	 *
	 * @param attribute A map-valued attribute of the represented type
	 * @param type A subtype of the key type
	 *
	 * @deprecated Use {@link #addTreatedMapKeySubgraph(MapAttribute, Class)}
	 */
	@Deprecated(since = "7.0")
	<AJ> SubGraph<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> type)
			throws CannotContainSubGraphException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc deprecated

	/**
	 * Create and return a new (mutable) {@link SubGraph} associated with
	 * the element of the given collection, or return an existing such
	 * {@link SubGraph} if there is one.
	 *
	 * @deprecated {@link #addElementSubgraph(PluralAttribute)} was added
	 *             in JPA 3.2, and so this method is no longer needed
	 *
	 * @since 6.3
	 *
	 * @see #addElementSubgraph(PluralAttribute)
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default <AJ> SubGraph<AJ> addPluralSubgraph(PluralAttribute<? super J, ?, AJ> attribute) {
		return addSubGraph( attribute.getName(), attribute.getBindableJavaType() );
	}

	@Override @Deprecated(forRemoval = true)
	default <X> SubGraph<? extends X> addSubgraph(Attribute<? super J, X> attribute, Class<? extends X> type) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute ).addTreatedSubgraph( type );
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
