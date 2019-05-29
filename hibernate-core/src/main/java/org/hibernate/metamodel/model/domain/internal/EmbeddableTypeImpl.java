/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.NodeBuilder;
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

	public EmbeddableTypeImpl(JavaTypeDescriptor<J> javaTypeDescriptor, NodeBuilder nodeBuilder) {
		super( javaTypeDescriptor.getJavaType().getName(), javaTypeDescriptor, null );
	}

	public EmbeddableTypeImpl(
			String name,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				name,
				(JavaTypeDescriptor) nodeBuilder.getDomainModel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class ),
				null
		);
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
