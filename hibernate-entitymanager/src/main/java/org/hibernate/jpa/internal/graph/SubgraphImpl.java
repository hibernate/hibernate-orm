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
package org.hibernate.jpa.internal.graph;

import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SubgraphImpl<T> extends AttributeNodeImpl<T>
		implements Subgraph<T>, GraphNode<T>, GraphDelegate.AttributeDelegate<T> {

	private final ManagedType managedType;
	private final boolean isMapKeySubgraph;
	private final GraphDelegate<T> graphDelegate;

	public SubgraphImpl(Attribute attribute, ManagedType managedType) {
		this( attribute, managedType, false );
	}

	public SubgraphImpl(Attribute attribute, ManagedType managedType, boolean isMapKeySubgraph) {
		super( attribute );
		this.managedType = managedType;
		this.isMapKeySubgraph = isMapKeySubgraph;
		this.graphDelegate = new GraphDelegate<T>( this );
	}

	private SubgraphImpl(SubgraphImpl<T> original) {
		super( original.getAttribute() );
		this.managedType = original.managedType;
		this.isMapKeySubgraph = original.isMapKeySubgraph;
		this.graphDelegate = original.graphDelegate.makeImmutableCopy( this );
	}

	@Override
	public SubgraphImpl<T> makeImmutableCopy() {
		return new SubgraphImpl<T>( this );
	}

	public boolean isMapKeySubgraph() {
		return isMapKeySubgraph;
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		graphDelegate.addAttributeNodes( attributeNames );
	}

	@Override
	public void addAttributeNodes(Attribute<T, ?>... attributes) {
		graphDelegate.addAttributeNodes( attributes );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addSubgraph(Attribute<T, X> attribute) {
		return (Subgraph<X>) addSubgraph( attribute, attribute.getJavaType() );
	}

	@Override
	public <X> Subgraph<? extends X> addSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return graphDelegate.addSubgraph( attribute, type );
	}

	@Override
	public <X> Subgraph<X> addSubgraph(String attributeName) {
		return graphDelegate.addSubgraph( attributeName );
	}

	@Override
	public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
		return graphDelegate.addSubgraph( attributeName, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> Subgraph<X> addKeySubgraph(Attribute<T, X> attribute) {
		return (Subgraph<X>) addKeySubgraph( attribute, attribute.getJavaType() );
	}

	@Override
	public <X> Subgraph<? extends X> addKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return graphDelegate.addKeySubgraph( attribute, type );
	}

	@Override
	public <X> Subgraph<X> addKeySubgraph(String attributeName) {
		return graphDelegate.addKeySubgraph( attributeName );
	}

	@Override
	public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
		return graphDelegate.addKeySubgraph( attributeName, type );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getClassType() {
		return managedType.getJavaType();
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodes() {
		return new ArrayList<AttributeNode<?>>( graphDelegate.getGraphNodes() );
	}


	// AttributeDelegate impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("unchecked")
	public AttributeNodeImpl<?> buildAttributeNode(String attributeName) {
		return buildAttributeNode( resolveAttribute( attributeName ) );
	}

	@SuppressWarnings("unchecked")
	private Attribute<T,?> resolveAttribute(String attributeName) {
		final Attribute<T,?> attribute = managedType.getDeclaredAttribute( attributeName );
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

	@SuppressWarnings("unchecked")
	private <X> Attribute<T, ? extends X> resolveAttribute(String attributeName, Class<? extends X> type) {
		final Attribute<T,?> attribute = managedType.getDeclaredAttribute( attributeName );
		if ( attribute == null ) {
			throw new IllegalArgumentException(
					String.format(
							"Given attribute name [%s] is not an attribute on this class [%s]",
							attributeName,
							managedType.getClass().getName()
					)
			);
		}

		return (Attribute<T, ? extends X>) attribute;
	}

	@Override
	public AttributeNodeImpl<?> buildAttributeNode(Attribute<T, ?> attribute) {
		return new AttributeNodeImpl<Object>( attribute );
	}

	@Override
	public SubgraphImpl buildSubgraph(String attributeName) {
		final Attribute<T,?> attribute = resolveAttribute( attributeName );
		return new SubgraphImpl( attribute, toManagedType( attribute ) );
	}

	@SuppressWarnings("unchecked")
	private <X> ManagedType<X> toManagedType(Attribute<T, X> attribute) {
		if ( attribute.isCollection() ) {
			final Type elementType = ( (PluralAttribute) attribute ).getElementType();
			if ( ! ManagedType.class.isInstance( elementType ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Specified collection attribute [%s] is not comprised of elements of a ManagedType",
								attribute.toString()
						)
				);
			}
			return (ManagedType<X>) elementType;
		}
		else {
			final Type type = ( (SingularAttribute) attribute ).getType();
			if ( ! ManagedType.class.isInstance( type ) ) {
				throw new IllegalArgumentException(
						String.format( "Specified attribute [%s] is not a ManagedType" , attribute.toString() )
				);
			}
			return (ManagedType<X>) type;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> buildSubgraph(String attributeName, Class<X> type) {
		final Attribute<T,X> attribute = (Attribute<T, X>) resolveAttribute( attributeName, type );
		return new SubgraphImpl<X>( attribute, toManagedType( attribute ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<? extends X> buildSubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return new SubgraphImpl( attribute, toManagedType( attribute ) );
	}

	@Override
	public SubgraphImpl buildKeySubgraph(String attributeName) {
		final Attribute<T,?> attribute = resolveAttribute( attributeName );
		return new SubgraphImpl( attribute, toManagedType( attribute ), true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> buildKeySubgraph(String attributeName, Class<X> type) {
		final Attribute<T,X> attribute = (Attribute<T, X>) resolveAttribute( attributeName, type );
		return new SubgraphImpl<X>( attribute, toManagedType( attribute ), true );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<? extends X> buildKeySubgraph(Attribute<T, X> attribute, Class<? extends X> type) {
		return new SubgraphImpl( attribute, toManagedType( attribute ), true );
	}

}
