/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import javax.persistence.EntityGraph;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;

/**
 * The Hibernate implementation of the JPA EntityGraph contract.
 *
 * @author Steve Ebersole
 */
public class RootGraphImpl<J> extends AbstractGraph<J> implements EntityGraph<J>, RootGraphImplementor<J> {
	private final String name;

	public RootGraphImpl(
			String name,
			EntityTypeDescriptor<J> entityType,
			boolean mutable,
			SessionFactoryImplementor sessionFactory) {
		super( entityType, mutable, sessionFactory );
		this.name = name;
	}

	public RootGraphImpl(String name, EntityTypeDescriptor<J> entityType, SessionFactoryImplementor sessionFactory) {
		this(
				name,
				entityType,
				true,
				sessionFactory
		);
	}

	public RootGraphImpl(String name, boolean mutable, GraphImplementor<J> original) {
		super( mutable, original );
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RootGraphImplementor<J> makeCopy(boolean mutable) {
		return new RootGraphImpl<>( null, mutable, this );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph(boolean mutable) {
		return new SubGraphImpl<>( mutable, this );
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		if ( ! mutable && ! isMutable() ) {
			return this;
		}

		return super.makeRootGraph( name, mutable );
	}

	@Override
	public <T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public boolean appliesTo(EntityTypeDescriptor<? super J> entityType) {
		if ( this.getGraphedType().equals( entityType ) ) {
			return true;
		}

		IdentifiableTypeDescriptor superType = entityType.getSupertype();
		while ( superType != null ) {
			if ( superType.equals( entityType ) ) {
				return true;
			}
			superType = superType.getSupertype();
		}

		return false;
	}

	@Override
	public boolean appliesTo(String entityName) {
		return appliesTo( sessionFactory().getMetamodel().entity( entityName ) );
	}

	@Override
	public boolean appliesTo(Class entityType) {
		return appliesTo( sessionFactory().getMetamodel().entity( entityType ) );
	}
}
