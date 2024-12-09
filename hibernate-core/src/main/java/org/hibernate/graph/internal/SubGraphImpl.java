/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * Implementation of the JPA-defined {@link jakarta.persistence.Subgraph} interface.
 *
 * @author Steve Ebersole
 */
public class SubGraphImpl<J> extends AbstractGraph<J> implements SubGraphImplementor<J> {

	private GraphImplementor<J> parent = null;

	public SubGraphImpl(ManagedDomainType<J> managedType, boolean mutable) {
		super( managedType, mutable );
	}

	public SubGraphImpl(AbstractGraph<J> original, boolean mutable) {
		super( original, mutable );
	}

	protected SubGraphImpl(ManagedDomainType<J> managedType, GraphImplementor<J> parent, boolean mutable) {
		this( managedType, mutable );
		this.parent = parent;
	}


	@Override
	public SubGraphImplementor<J> makeCopy(boolean mutable) {
		return new SubGraphImpl<>( this, mutable );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return !mutable && !isMutable() ? this : makeCopy( true );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return super.addKeySubGraph( attributeName );
	}

}
