/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl<T> implements AnyMappingDomainType<T> {
	private final AnyType anyType;
	private final JavaType<T> baseJtd;
	private final AnyDiscriminatorDomainTypeImpl<?> anyDiscriminatorType;

	public AnyMappingDomainTypeImpl(AnyType anyType, JavaType<T> baseJtd) {
		this.anyType = anyType;
		this.baseJtd = baseJtd;
		final MetaType discriminatorType = (MetaType) anyType.getDiscriminatorType();
		anyDiscriminatorType = new AnyDiscriminatorDomainTypeImpl<>(
				(BasicType<?>) discriminatorType.getBaseType(),
				discriminatorType);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override @SuppressWarnings("unchecked")
	public Class<T> getJavaType() {
		return (Class<T>) anyType.getReturnedClass();
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return baseJtd;
	}

	@Override
	public AnyDiscriminatorDomainTypeImpl<?> getDiscriminatorType() {
		return anyDiscriminatorType;
	}

	@Override
	public SimpleDomainType<?> getKeyType() {
		return (BasicType<?>) anyType.getIdentifierType();
	}
}
