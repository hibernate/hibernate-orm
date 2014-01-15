/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.graph.internal;

import java.util.List;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.jpa.HibernateEntityManagerFactory;

/**
 * @author Steve Ebersole
 */
public class SubgraphImpl<T> extends AbstractGraphNode<T> implements Subgraph<T>, GraphNodeImplementor {
	private final ManagedType managedType;
	private final Class<T> subclass;

	public SubgraphImpl(
			HibernateEntityManagerFactory entityManagerFactory,
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

	public SubgraphImpl<T> makeImmutableCopy() {
		return new SubgraphImpl<T>( this );
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		super.addAttributeNodes( attributeNames );
	}

	@Override
	public void addAttributeNodes(Attribute<T, ?>... attributes) {
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
}
