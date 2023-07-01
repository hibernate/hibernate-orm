/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

import jakarta.persistence.EntityGraph;

/**
 * The Hibernate implementation of the JPA EntityGraph contract.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends AbstractGraph<J> implements EntityGraph<J>, RootGraphImplementor<J> {

	private final String name;

	public RootGraphImpl(String name, EntityDomainType<J> entityType, boolean mutable) {
		super( entityType, mutable );
		this.name = name;
	}

	public RootGraphImpl(String name, EntityDomainType<J> entityType) {
		this( name, entityType, true );
	}

	public RootGraphImpl(String name, GraphImplementor<J> original, boolean mutable) {
		super(original, mutable);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RootGraphImplementor<J> makeCopy(boolean mutable) {
		return new RootGraphImpl<>( null, this, mutable);
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>(this, mutable);
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		return !mutable && !isMutable() ? this : super.makeRootGraph( name, mutable );
	}

	@Override
	public <T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean appliesTo(EntityDomainType<?> entityType, JpaMetamodel metamodel) {
		final ManagedDomainType<J> managedTypeDescriptor = getGraphedType();
		if ( managedTypeDescriptor.equals( entityType ) ) {
			return true;
		}

		IdentifiableDomainType<?> superType = entityType.getSupertype();
		while ( superType != null ) {
			if ( managedTypeDescriptor.equals( superType ) ) {
				return true;
			}
			superType = superType.getSupertype();
		}

		return false;
	}

	@Override
	public boolean appliesTo(String entityName, JpaMetamodel metamodel) {
		return appliesTo( metamodel.entity( entityName ), metamodel );
	}

	@Override
	public boolean appliesTo(Class<?> type, JpaMetamodel metamodel) {
		return appliesTo( metamodel.entity( type ), metamodel );
	}
}
