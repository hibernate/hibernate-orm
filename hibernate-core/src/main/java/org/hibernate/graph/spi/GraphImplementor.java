/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.metamodel.Attribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.Graph;
import org.hibernate.graph.SubGraph;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;

/**
 * Integration version of the Graph contract
 *
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface GraphImplementor<J> extends Graph<J>, GraphNodeImplementor<J> {
	boolean appliesTo(ManagedTypeDescriptor<? super J> managedType);

	boolean appliesTo(Class<? super J> javaType);

	@SuppressWarnings("unchecked")
	void merge(GraphImplementor<J>... others);

	SessionFactoryImplementor sessionFactory();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variant returns

	@Override
	ManagedTypeDescriptor<J> getGraphedType();

	@Override
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) throws CannotBecomeEntityGraphException;

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
	@SuppressWarnings("unchecked")
	default List<AttributeNode<?>> getAttributeNodeList() {
		return (List) getAttributeNodeImplementors();
	}

	@Override
	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName);

	<AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttributeDescriptor<? extends J, AJ> attribute);

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> AttributeNodeImplementor<AJ> findAttributeNode(Attribute<? extends J, AJ> attribute) {
		return (AttributeNodeImplementor) findAttributeNode( (PersistentAttributeDescriptor) attribute );
	}

	@Override
	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName) throws CannotContainSubGraphException;

	<AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttributeDescriptor<? extends J, AJ> attribute) throws CannotContainSubGraphException;

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> AttributeNodeImplementor<AJ> addAttributeNode(Attribute<? extends J, AJ> attribute)
			throws CannotContainSubGraphException {
		return addAttributeNode( (PersistentAttributeDescriptor) attribute );
	}

	@SuppressWarnings("unchecked")
	default <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String name) {
		return findOrCreateAttributeNode( (PersistentAttributeDescriptor) getGraphedType().getAttribute( name ) );
	}

	<AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttributeDescriptor<? extends J, AJ> attribute);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sub graph nodes

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName) throws CannotContainSubGraphException {
		return (SubGraphImplementor) findOrCreateAttributeNode( attributeName ).makeSubGraph();
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subType) throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attributeName ).makeSubGraph( subType );
	}

	default <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttributeDescriptor<? extends J, AJ> attribute)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeSubGraph();
	}

	default <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttributeDescriptor<? extends J, AJ> attribute, Class<AJ> subType)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( subType );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addSubGraph(Attribute<? extends J, AJ> attribute)
			throws CannotContainSubGraphException {
		return addSubGraph( (PersistentAttributeDescriptor) attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraph<? extends AJ> addSubGraph(Attribute<? extends J, AJ> attribute, Class<? extends AJ> type)
			throws CannotContainSubGraphException {
		return addSubGraph( (PersistentAttributeDescriptor) attribute, type );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// key sub graph nodes

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return (SubGraphImplementor) findOrCreateAttributeNode( attributeName ).makeKeySubGraph();
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attributeName ).makeKeySubGraph( subtype );
	}

	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(PersistentAttributeDescriptor<? extends J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph();
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<AJ> addKeySubGraph(Attribute<? extends J, AJ> attribute) {
		return addKeySubGraph( (PersistentAttributeDescriptor) attribute );
	}

	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<? extends AJ> addKeySubGraph(
			PersistentAttributeDescriptor<? extends J, AJ> attribute,
			Class<? extends AJ> subType) throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph( subType );
	}

	@Override
	@SuppressWarnings("unchecked")
	default <AJ> SubGraphImplementor<? extends AJ> addKeySubGraph(
			Attribute<? extends J, AJ> attribute,
			Class<? extends AJ> subType) throws CannotContainSubGraphException {
		return addKeySubGraph( (PersistentAttributeDescriptor) attribute, subType );
	}

}
