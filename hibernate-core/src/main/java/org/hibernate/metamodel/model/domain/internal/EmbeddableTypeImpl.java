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
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Standard Hibernate implementation of JPA's {@link jakarta.persistence.metamodel.EmbeddableType}
 * contract
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole`
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements EmbeddableDomainType<J>, Serializable {

	private final boolean isDynamic;
	private final EmbeddableRepresentationStrategy representationStrategy;

	public EmbeddableTypeImpl(
			JavaType<J> javaTypeDescriptor,
			EmbeddableRepresentationStrategy representationStrategy,
			boolean isDynamic,
			JpaMetamodel domainMetamodel) {
		super( javaTypeDescriptor.getJavaType().getTypeName(), javaTypeDescriptor, null, domainMetamodel );
		this.representationStrategy = representationStrategy;
		this.isDynamic = isDynamic;
	}

	public EmbeddableTypeImpl(
			String name,
			JpaMetamodel domainMetamodel) {
		//noinspection unchecked
		super(
				name,
				(JavaType) domainMetamodel.getTypeConfiguration()
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
