/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.Graph;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * Integration version of the {@link Graph} contract
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface GraphImplementor<J> extends Graph<J>, GraphNodeImplementor<J> {

	void merge(GraphImplementor<? extends J> other);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant returns

	@Override
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable)
			throws CannotBecomeEntityGraphException;

	@Override
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	@Override
	GraphImplementor<J> makeCopy(boolean mutable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode access

	default void visitAttributeNodes(Consumer<AttributeNodeImplementor<?>> consumer) {
		getAttributeNodeImplementors().forEach( consumer );
	}

	AttributeNodeImplementor<?> addAttributeNode(AttributeNodeImplementor<?> makeCopy);

	List<AttributeNodeImplementor<?>> getAttributeNodeImplementors();

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	default List<AttributeNode<?>> getAttributeNodeList() {
		return (List) getAttributeNodeImplementors();
	}

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName);

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? extends J, AJ> attribute);

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName)
			throws CannotContainSubGraphException;

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? extends J, AJ> attribute)
			throws CannotContainSubGraphException;

	@SuppressWarnings("unchecked")
	default <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String name) {
		PersistentAttribute<? super J, ?> attribute = getGraphedType().getAttribute( name );
		if ( attribute instanceof SqmPathSource && ( (SqmPathSource<?>) attribute ).isGeneric() ) {
			attribute = getGraphedType().findConcreteGenericAttribute( name );
		}

		return findOrCreateAttributeNode( (PersistentAttribute<? extends J, AJ>) attribute );
	}

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? extends J, AJ> attribute);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sub graph nodes

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName)
			throws CannotContainSubGraphException {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeSubGraph();
	}

	@Override
	default <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subType)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attributeName ).makeSubGraph( subType );
	}

	@Override
	default <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? extends J, AJ> attribute)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeSubGraph();
	}

	@Override
	default <AJ> SubGraphImplementor<? extends AJ> addSubGraph(
			PersistentAttribute<? extends J, AJ> attribute,
			Class<? extends AJ> subType) throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( subType );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// key sub graph nodes

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeKeySubGraph();
	}

	@Override
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attributeName ).makeKeySubGraph( subtype );
	}

	@Override
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(PersistentAttribute<? extends J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph();
	}

	@Override
	default <AJ> SubGraphImplementor<? extends AJ> addKeySubGraph(
			PersistentAttribute<? extends J, AJ> attribute,
			Class<? extends AJ> subType)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph( subType );
	}
}
