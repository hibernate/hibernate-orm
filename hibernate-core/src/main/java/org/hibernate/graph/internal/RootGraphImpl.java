/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.EntityGraph} interface.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends AbstractGraph<J> implements RootGraphImplementor<J> {

	private final String name;

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
	public RootGraphImplementor<J> makeCopy(boolean mutable) {
		return new RootGraphImpl<>( null, this, mutable );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>( this, mutable );
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		return !mutable && !isMutable() ? this : super.makeRootGraph( name, mutable );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		return addTreatedSubgraph( (Class) type );
	}

	@Override
	public boolean appliesTo(EntityDomainType<?> entityType) {
		return GraphHelper.appliesTo( this, entityType );
	}
}
