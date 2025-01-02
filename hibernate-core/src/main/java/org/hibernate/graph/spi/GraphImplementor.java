/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.Map;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.Graph;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;


/**
 * Integration version of the {@link Graph} contract
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
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
	List<? extends AttributeNodeImplementor<?>> getAttributeNodeList();

	Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?>> getNodes();

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String name);

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subType);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute);

	@Override
	<AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subtype);

	<AJ> SubGraphImplementor<AJ> addTreatedSubgraph(PersistentAttribute<? super J, ? super AJ> attribute, ManagedType<AJ> subtype);

	@Incubating
	<AJ> SubGraphImplementor<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, Class<AJ> type);

	@Incubating
	<AJ> SubGraphImplementor<AJ> addTreatedElementSubgraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, ManagedType<AJ> type);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> subtype);

	<AJ> SubGraphImplementor<AJ> addTreatedMapKeySubgraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, ManagedType<AJ> subtype);

	@Override
	<Y extends J> SubGraphImplementor<Y> addTreatedSubgraph(Class<Y> type);

	<Y extends J> SubGraphImplementor<Y> addTreatedSubgraph(ManagedType<Y> type);

	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphs();

	@Override
	default <Y> AttributeNode<Y> getAttributeNode(String attributeName) {
		return findAttributeNode( attributeName );
	}

	@Override
	default <Y> AttributeNode<Y> getAttributeNode(Attribute<? super J, Y> attribute) {
		return findAttributeNode( (PersistentAttribute<? super J, Y>) attribute );
	}

	@Override
	default <Y> AttributeNode<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
		return addAttributeNode( (PersistentAttribute<? super J, Y>) attribute );
	}

	@Override
	default <X> SubGraphImplementor<X> addSubgraph(String attributeName, Class<X> type) {
		return addSubGraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	default <X> SubGraphImplementor<X> addSubgraph(Attribute<? super J, X> attribute) {
		return addSubGraph( (PersistentAttribute<? super J, X>) attribute );
	}

	@Override
	default <Y> SubGraphImplementor<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		return addSubGraph( (PersistentAttribute<? super J, ? super Y>) attribute ).addTreatedSubgraph( type );
	}

	@Override
	default <AJ> SubGraph<AJ> addTreatedSubgraph(Attribute<? super J, ? super AJ> attribute, ManagedType<AJ> type) {
		return addSubGraph( (PersistentAttribute<? super J, ? super AJ>) attribute ).addTreatedSubgraph( type );
	}

	@Override
	default <E> SubGraphImplementor<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type) {
		return addElementSubGraph( (PluralPersistentAttribute<? super J, ?, ? super E>) attribute, type );
	}

	@Override
	default <AJ> SubGraph<AJ> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super AJ> attribute, ManagedType<AJ> type) {
		return addTreatedElementSubgraph( (PluralPersistentAttribute<? super J, ?, ? super AJ>) attribute, type );
	}

	@Override
	default <X> SubGraphImplementor<X> addKeySubgraph(String attributeName) {
		return addKeySubGraph( attributeName );
	}

	@Override
	default <X> SubGraphImplementor<X> addKeySubgraph(String attributeName, Class<X> type) {
		return addKeySubGraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	default <K> SubGraphImplementor<K> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super K, ?> attribute, Class<K> type) {
		return addKeySubGraph( (MapPersistentAttribute<? super J, ? super K, ?>) attribute, type );
	}

	@Override
	default <AJ> SubGraph<AJ> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super AJ, ?> attribute, ManagedType<AJ> type) {
		return addTreatedMapKeySubgraph( (MapPersistentAttribute<? super J, ? super AJ, ?>) attribute, type );
	}

	@Override
	default boolean hasAttributeNode(String attributeName) {
		return getAttributeNode( attributeName ) != null;
	}

	@Override
	default boolean hasAttributeNode(Attribute<? super J, ?> attribute) {
		return getAttributeNode( attribute ) != null;
	}
}
