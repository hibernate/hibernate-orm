/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.graph.internal;

import java.util.List;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;

/**
 * @author Steve Ebersole
 */
public class SubgraphImpl<T> extends AbstractGraphNode<T> implements Subgraph<T>, GraphNodeImplementor {
	private final ManagedType managedType;
	private final Class<T> subclass;

	@SuppressWarnings("WeakerAccess")
	public SubgraphImpl(
			SessionFactoryImplementor entityManagerFactory,
			ManagedType managedType,
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
		return super.attributeNodes();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Attribute<T,?> resolveAttribute(String attributeName) {
		final Attribute<T,?> attribute = managedType.getAttribute( attributeName );
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
	ManagedType getManagedType() {
		return managedType;
	}
}
