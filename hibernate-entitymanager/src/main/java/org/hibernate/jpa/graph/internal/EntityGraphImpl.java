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
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.jpa.HibernateEntityManagerFactory;

/**
 * The Hibernate implementation of the JPA EntityGraph contract.
 *
 * @author Steve Ebersole
 */
public class EntityGraphImpl<T> extends AbstractGraphNode<T> implements EntityGraph<T>, GraphNodeImplementor {
	private final String name;
	private final EntityType<T> entityType;

	public EntityGraphImpl(String name, EntityType<T> entityType, HibernateEntityManagerFactory entityManagerFactory) {
		super( entityManagerFactory, true );
		this.name = name;
		this.entityType = entityType;
	}

	public EntityGraphImpl<T> makeImmutableCopy(String name) {
		return new EntityGraphImpl<T>( name, this, false );
	}

	public EntityGraphImpl<T> makeMutableCopy() {
		return new EntityGraphImpl<T>( name, this, true );
	}

	private EntityGraphImpl(String name, EntityGraphImpl<T> original, boolean mutable) {
		super( original, mutable );
		this.name = name;
		this.entityType = original.entityType;
	}

	public EntityType<T> getEntityType() {
		return entityType;
	}

	@Override
	public String getName() {
		return name;
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
	public <T1 extends Object> Subgraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodes() {
		return super.attributeNodes();
	}

	@Override
	protected Attribute<T,?> resolveAttribute(String attributeName) {
        final Attribute attribute = entityType.getAttribute( attributeName );
		if ( attribute == null ) {
			throw new IllegalArgumentException(
					String.format(
							"Given attribute name [%s] is not an attribute on this entity [%s]",
							attributeName,
							entityType.getName()
					)
			);
		}
		return attribute;
	}

	@SuppressWarnings("unchecked")
	public boolean appliesTo(String entityName) {
		return appliesTo( getFactory().getEntityTypeByName( entityName ) );
	}

	public boolean appliesTo(EntityType<? super T> entityType) {
		if ( this.entityType.equals( entityType ) ) {
			return true;
		}

		IdentifiableType superType = entityType.getSupertype();
		while ( superType != null ) {
			if ( superType.equals( entityType ) ) {
				return true;
			}
			superType = superType.getSupertype();
		}

		return false;
	}
}
