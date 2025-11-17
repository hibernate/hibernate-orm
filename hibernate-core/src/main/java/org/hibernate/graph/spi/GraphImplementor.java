/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.Map;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.Internal;
import org.hibernate.graph.Graph;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;


/**
 * Integration version of the {@link Graph} contract.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 * @author Gavin King
 */
public interface GraphImplementor<J> extends Graph<J>, GraphNodeImplementor<J> {

	void merge(GraphImplementor<J> other);

	@Internal
	void mergeInternal(GraphImplementor<J> graph);

	@Override
	@Deprecated(forRemoval = true)
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable);

	@Override
	@Deprecated(forRemoval = true)
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	@Override
	GraphImplementor<J> makeCopy(boolean mutable);

	@Override
	List<? extends AttributeNodeImplementor<?,?,?>> getAttributeNodeList();

	Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?,?,?>> getNodes();

	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getTreatedSubgraphs();

	@Override
	<Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(String attributeName);

	@Override
	<Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(Attribute<? super J, Y> attribute);

	@Override
	<AJ> AttributeNodeImplementor<AJ,?,?> findAttributeNode(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ,?,?> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	<AJ> AttributeNodeImplementor<AJ,?,?> findOrCreateAttributeNode(String name);

	<AJ> AttributeNodeImplementor<AJ,?,?> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	<AJ> AttributeNodeImplementor<AJ,?,?> addAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	<Y> AttributeNodeImplementor<Y,?,?> addAttributeNode(Attribute<? super J, Y> attribute);

	@Override
	<Y extends J> SubGraphImplementor<Y> addTreatedSubgraph(Class<Y> type);

	<Y extends J> SubGraphImplementor<Y> addTreatedSubgraph(ManagedType<Y> type);

	@Override
	<X> SubGraphImplementor<X> addSubgraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subType);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> subtype);

	@Override
	<X> SubGraphImplementor<X> addSubgraph(String attributeName, Class<X> type);

	@Override
	<X> SubGraphImplementor<X> addSubgraph(Attribute<? super J, X> attribute);

	@Override
	<Y> SubGraphImplementor<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type);

	@Override
	<AJ> SubGraphImplementor<AJ> addTreatedSubgraph(Attribute<? super J, ? super AJ> attribute, ManagedType<AJ> type);

	@Override
	<E> SubGraphImplementor<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type);

	@Override
	<AJ> SubGraph<AJ> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super AJ> attribute, ManagedType<AJ> type);

	@Override
	<X> SubGraphImplementor<X> addKeySubgraph(String attributeName);

	@Override
	<X> SubGraphImplementor<X> addKeySubgraph(String attributeName, Class<X> type);

	@Override
	<K> SubGraphImplementor<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type);

	@Override
	<AJ> SubGraphImplementor<AJ> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super AJ, ?> attribute, ManagedType<AJ> type);

	@Override
	default boolean hasAttributeNode(String attributeName) {
		return getAttributeNode( attributeName ) != null;
	}

	@Override
	default boolean hasAttributeNode(Attribute<? super J, ?> attribute) {
		return getAttributeNode( attribute ) != null;
	}
}
