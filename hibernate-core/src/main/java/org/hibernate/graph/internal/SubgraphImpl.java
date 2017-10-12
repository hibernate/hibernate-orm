/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import java.util.List;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeContainer;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;

/**
 * @author Steve Ebersole
 */
public class SubgraphImpl<T> extends AbstractAttributeNodeContainer<T> implements Subgraph<T>, AttributeNodeContainer {
	private final ManagedTypeDescriptor managedType;
	private final Class<T> subclass;

	@SuppressWarnings("WeakerAccess")
	public SubgraphImpl(
			SessionFactoryImplementor entityManagerFactory,
			ManagedTypeDescriptor managedType,
			Class<T> subclass) {
		super( entityManagerFactory, true );
		this.managedType = managedType;
		this.subclass = subclass;
	}

	private SubgraphImpl(SubgraphImpl<T> original) {
		super( original, false );
		this.managedType = original.managedType;
		this.subclass = original.subclass;
	}

	@SuppressWarnings("WeakerAccess")
	public SubgraphImpl<T> makeImmutableCopy() {
		return new SubgraphImpl<>( this );
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		super.addAttributeNodes( attributeNames );
	}

	@Override
	@SafeVarargs
	public final void addAttributeNodes(Attribute<T, ?>... attributes) {
		super.addAttributeNodes( attributes );
	}

	@Override
	public <X> SubgraphImpl<X> addSubgraph(Attribute<T, X> attribute) {
		return super.addSubgraph( attribute );
	}

	@Override
	public <X> SubgraphImpl<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return super.addSubgraph( attribute, type );
	}

	@Override
	public <X> SubgraphImpl<X> addSubgraph(String attributeName) {
		return super.addSubgraph( attributeName );
	}

	@Override
	public <X> SubgraphImpl<X> addSubgraph(String attributeName, Class<X> type) {
		return super.addSubgraph( attributeName, type );
	}

	@Override
	public <X> SubgraphImpl<X> addKeySubgraph(Attribute<T, X> attribute) {
		return super.addKeySubgraph( attribute );
	}

	@Override
	public <X> SubgraphImpl<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return super.addKeySubgraph( attribute, type );
	}

	@Override
	public <X> SubgraphImpl<X> addKeySubgraph(String attributeName) {
		return super.addKeySubgraph( attributeName );
	}

	@Override
	public <X> SubgraphImpl<X> addKeySubgraph(String attributeName, Class<X> type) {
		return super.addKeySubgraph( attributeName, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getClassType() {
		return managedType.getJavaType();
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodes() {
		return super.jpaAttributeNodes();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected PersistentAttribute<T,?> resolveAttribute(String attributeName) {
		final PersistentAttribute<T,?> attribute = managedType.findPersistentAttribute( attributeName );
		if ( attribute == null ) {
			throw new IllegalArgumentException(
					String.format(
							"Given attribute name [%s] is not an attribute on this class [%s]",
							attributeName,
							managedType.getClass().getName()
					)
			);
		}
		return attribute;
	}

	@Override
	ManagedTypeDescriptor getManagedType() {
		return managedType;
	}
}
