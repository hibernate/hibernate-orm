/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;
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

	private final ManagedTypeRepresentationStrategy representationStrategy;

	public EmbeddableTypeImpl(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			ManagedTypeRepresentationStrategy representationStrategy,
			JpaMetamodel domainMetamodel) {
		super( javaTypeDescriptor.getJavaType().getName(), javaTypeDescriptor, null, domainMetamodel );
		this.representationStrategy = representationStrategy;
	}

	public EmbeddableTypeImpl(
			String name,
			JpaMetamodel domainMetamodel) {
		//noinspection unchecked
		super(
				name,
				(JavaTypeDescriptor) domainMetamodel.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class ),
				null,
				domainMetamodel
		);

		// todo (6.0) : need ManagedTypeRepresentationStrategy impls
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ManagedTypeRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		return new SubGraphImpl( this, true, jpaMetamodel() );
	}
}
