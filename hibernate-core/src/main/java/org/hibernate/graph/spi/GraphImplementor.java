/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.Graph;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import jakarta.persistence.metamodel.Attribute;

/**
 * Integration version of the {@link Graph} contract
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface GraphImplementor<J> extends Graph<J>, GraphNodeImplementor<J> {

	void merge(GraphImplementor<J> other);

	@Override @Deprecated(forRemoval = true)
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	@Override @Deprecated(forRemoval = true)
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	@Override
	GraphImplementor<J> makeCopy(boolean mutable);

	@Override
	default boolean hasAttributeNode(String attributeName) {
		return getAttributeNode( attributeName ) != null;
	}

	@Override
	default boolean hasAttributeNode(Attribute<? super J, ?> attribute) {
		return getAttributeNode( attribute ) != null;
	}

	List<AttributeNodeImplementor<?>> getAttributeNodeImplementors();

	Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?>> getAttributeNodesByAttribute();

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String name);

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute);

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName);

	@Override
	<Y> AttributeNodeImplementor<Y> addAttributeNode(Attribute<? super J, Y> attribute);

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute);

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
}
