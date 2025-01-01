/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import org.hibernate.metamodel.model.domain.ManagedDomainType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.EntityGraph} interface.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends AbstractGraph<J> implements RootGraphImplementor<J> {

	private final String name;
	private List<SubGraphImpl<? extends J>> subgraphs;

	public RootGraphImpl(String name, EntityDomainType<J> entityType, boolean mutable) {
		super( entityType, mutable );
		this.name = name;
	}

	public RootGraphImpl(String name, EntityDomainType<J> entityType) {
		this( name, entityType, true );
	}

	public RootGraphImpl(String name, GraphImplementor<J> original, boolean mutable) {
		super( original, mutable );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean appliesTo(EntityDomainType<?> entityType) {
		return GraphHelper.appliesTo( this, entityType );
	}

	@Override
	public RootGraphImplementor<J> makeCopy(boolean mutable) {
		return new RootGraphImpl<>( null, this, mutable );
	}

	@Override @Deprecated(forRemoval = true)
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>( this, mutable );
	}

	@Override @Deprecated(forRemoval = true)
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		return !mutable && !isMutable() ? this : super.makeRootGraph( name, mutable );
	}

	@Override
	public RootGraphImplementor<J> makeImmutableCopy(String name) {
		return makeRootGraph( name, false );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> addTreatedSubgraph(Class<S> type) {
		final ManagedDomainType<S> managedDomainType = getGraphedType().getMetamodel().managedType( type );
		final SubGraphImpl<S> subgraph = new SubGraphImpl<>( managedDomainType, this, true );
		if ( subgraphs == null ) {
			subgraphs = new ArrayList<>( 1 );
		}
		subgraphs.add( subgraph );
		return subgraph;
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName) {
		final AttributeNodeImplementor<AJ> node = super.findAttributeNode( attributeName );
		if ( node == null && subgraphs != null ) {
			for ( SubGraphImpl<? extends J> subgraph : subgraphs ) {
				final AttributeNodeImplementor<AJ> subgraphNode = subgraph.findAttributeNode( attributeName );
				if ( subgraphNode != null ) {
					return subgraphNode;
				}
			}
			return null;
		}
		else {
			return node;
		}
	}
}
