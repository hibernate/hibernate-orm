/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.metamodel.SingularAttribute;

/**
 * Implementation of {@link jakarta.persistence.metamodel.EmbeddableType}.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements EmbeddableDomainType<J>, Serializable {
	private final boolean isDynamic;

	public EmbeddableTypeImpl(
			JavaType<J> javaType,
			boolean isDynamic,
			JpaMetamodelImplementor domainMetamodel) {
		this( javaType, null, isDynamic, domainMetamodel );
	}

	public EmbeddableTypeImpl(
			JavaType<J> javaType,
			ManagedDomainType<? super J> superType,
			boolean isDynamic,
			JpaMetamodelImplementor domainMetamodel) {
		super( javaType.getTypeName(), javaType, superType, domainMetamodel );
		this.isDynamic = isDynamic;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	public int getTupleLength() {
		int count = 0;
		for ( SingularAttribute<? super J, ?> attribute : getSingularAttributes() ) {
			count += ( (DomainType<?>) attribute.getType() ).getTupleLength();
		}
		return count;
	}
}
