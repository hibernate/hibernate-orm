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
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl implements AnyMappingDomainType<Class> {
	private final AnyType anyType;
	private final JavaType<Class> baseJtd;
	private final BasicType<Class> anyDiscriminatorType;

	public AnyMappingDomainTypeImpl(AnyType anyType, JavaType<Class> baseJtd, TypeConfiguration typeConfiguration) {
		this.anyType = anyType;
		this.baseJtd = baseJtd;
		final MetaType discriminatorType = (MetaType) anyType.getDiscriminatorType();
		final BasicType discriminatorBasicType = (BasicType) discriminatorType.getBaseType();
		anyDiscriminatorType =
				new ConvertedBasicTypeImpl<>(
						null, // no name
						discriminatorBasicType.getJdbcType(),
						new AnyDiscriminatorConverter( discriminatorType, discriminatorBasicType, typeConfiguration )
				);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Class> getJavaType() {
		return (Class<Class>) anyType.getReturnedClass();
	}

	@Override
	public JavaType<Class> getExpressibleJavaType() {
		return baseJtd;
	}

	@Override
	public BasicType<Class> getDiscriminatorType() {
		return anyDiscriminatorType;
	}

	@Override
	public SimpleDomainType getKeyType() {
		return (BasicType<?>) anyType.getIdentifierType();
	}

}
