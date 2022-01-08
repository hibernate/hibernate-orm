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
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl<T> implements AnyMappingDomainType<T> {
	private final AnyType anyType;
	private final JavaType<T> baseJtd;

	public AnyMappingDomainTypeImpl(AnyType anyType, JavaType<T> baseJtd) {
		this.anyType = anyType;
		this.baseJtd = baseJtd;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public Class<T> getJavaType() {
		return anyType.getReturnedClass();
	}

	@Override
	public JavaType<T> getExpressableJavaTypeDescriptor() {
		return baseJtd;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public SimpleDomainType<?> getDiscriminatorType() {
		return (BasicType) anyType.getDiscriminatorType();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public SimpleDomainType<?> getKeyType() {
		return (BasicType) anyType.getIdentifierType();
	}
}
