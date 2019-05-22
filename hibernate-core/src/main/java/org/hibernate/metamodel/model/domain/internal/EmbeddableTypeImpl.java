/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Standard Hibernate implementation of JPA's {@link javax.persistence.metamodel.EmbeddableType}
 * contract
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole`
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements EmbeddableDomainType<J>, Serializable {

	private final ManagedDomainType<?> parent;
	private final ComponentType hibernateType;

	public EmbeddableTypeImpl(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			ManagedDomainType<?> parent,
			ComponentType hibernateType,
			SessionFactoryImplementor sessionFactory) {
		super( javaTypeDescriptor.getJavaType().getName(), javaTypeDescriptor, null, sessionFactory );
		this.parent = parent;
		this.hibernateType = hibernateType;
	}

	public EmbeddableTypeImpl(
			String name,
			ManagedDomainType<?> parent,
			ComponentType hibernateType,
			SessionFactoryImplementor sessionFactory) {
		//noinspection unchecked
		super(
				name,
				(JavaTypeDescriptor) sessionFactory.getMetamodel().getTypeConfiguration().getJavaTypeDescriptorRegistry().getDescriptor( Map.class ),
				null,
				sessionFactory
		);
		this.parent = parent;
		this.hibernateType = hibernateType;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	public ManagedDomainType<?> getParent() {
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
