/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
public class SubGraphImpl<J> extends AbstractGraph<J> implements SubGraphImplementor<J> {
	public SubGraphImpl(
			ManagedDomainType<J> managedType,
			boolean mutable,
			JpaMetamodel jpaMetamodel) {
		super( managedType, mutable, jpaMetamodel );
	}

	public SubGraphImpl(boolean mutable, AbstractGraph<J> original) {
		super( mutable, original );
	}

	@Override
	public SubGraphImplementor<J> makeCopy(boolean mutable) {
		return new SubGraphImpl<>( mutable, this );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		if ( ! mutable && ! isMutable() ) {
			return this;
		}

		return makeCopy( true );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return super.addKeySubGraph( attributeName );
	}

	@Override
	public boolean appliesTo(ManagedDomainType<? super J> managedType) {
		if ( this.getGraphedType().equals( managedType ) ) {
			return true;
		}

		ManagedDomainType superType = managedType.getSuperType();
		while ( superType != null ) {
			if ( superType.equals( managedType ) ) {
				return true;
			}
			superType = superType.getSuperType();
		}

		return false;
	}

	@Override
	public boolean appliesTo(Class<? super J> javaType) {
		return appliesTo( jpaMetamodel().managedType( javaType ) );
	}
}
