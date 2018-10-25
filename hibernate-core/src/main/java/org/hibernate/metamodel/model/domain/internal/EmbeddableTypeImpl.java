/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.type.ComponentType;

/**
 * Standard Hibernate implementation of JPA's {@link javax.persistence.metamodel.EmbeddableType}
 * contract
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole`
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements EmbeddedTypeDescriptor<J>, Serializable {

	private final ManagedTypeDescriptor<?> parent;
	private final ComponentType hibernateType;

	public EmbeddableTypeImpl(
			Class<J> javaType,
			ManagedTypeDescriptor<?> parent,
			ComponentType hibernateType,
			SessionFactoryImplementor sessionFactory) {
		super( javaType, null, null, sessionFactory );
		this.parent = parent;
		this.hibernateType = hibernateType;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	public ManagedTypeDescriptor<?> getParent() {
		return parent;
	}

	public ComponentType getHibernateType() {
		return hibernateType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		return new SubGraphImpl( this, true, sessionFactory() );
	}
}
