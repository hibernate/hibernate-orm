/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.graph.internal;

import java.util.List;
import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;

/**
 * The Hibernate implementation of the JPA EntityGraph contract.
 *
 * @author Steve Ebersole
 */
public class EntityGraphImpl<T> extends AbstractGraphNode<T> implements EntityGraph<T>, EntityGraphImplementor<T> {
	private final String name;
	private final EntityType<T> entityType;

	public EntityGraphImpl(String name, EntityType<T> entityType, SessionFactoryImplementor sessionFactory) {
		super( sessionFactory, true );
		this.name = name;
		this.entityType = entityType;
	}

	public EntityGraphImpl<T> makeImmutableCopy(String name) {
		return new EntityGraphImpl<>( name, this, false );
	}

	public EntityGraphImpl<T> makeMutableCopy() {
		return new EntityGraphImpl<>( name, this, true );
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
	public <X> Subgraph<? extends X> addSubclassSubgraph(Class<? extends X> type) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	@Override
	public List<AttributeNode<?>> getAttributeNodes() {
		return super.attributeNodes();
	}

	@Override
	@SuppressWarnings("unchecked")
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
		return appliesTo( getFactory().getMetamodel().entity( entityName ) );
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

	@Override
	ManagedType getManagedType() {
		return entityType;
	}
}
