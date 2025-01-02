/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.Graph;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
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

	@Override @Deprecated(forRemoval = true)
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	@Override @Deprecated(forRemoval = true)
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
	<AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, ManagedDomainType<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, Class<AJ> type);

	@Override
	<AJ> SubGraphImplementor<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, ManagedDomainType<AJ> type);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> subtype);

	@Override
	<AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, ManagedDomainType<AJ> subtype);

	@Override
	<Y extends J> SubGraphImplementor<Y> addTreatedSubGraph(Class<Y> type);

	<Y extends J> SubGraphImplementor<Y> addTreatedSubGraph(ManagedDomainType<Y> type);

	Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphs();
}
