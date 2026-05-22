/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import jakarta.persistence.AttributeConverter;
import org.hibernate.HibernateException;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;


import static org.hibernate.internal.util.GenericsHelper.erasedType;
import static org.hibernate.internal.util.GenericsHelper.typeArguments;

/**
 * @author Gavin King
 * @since 7.0
 */
public class ConverterHelper {
	public static <X, Y> BasicValueConverter<X, Y> createValueConverter(
			AttributeConverter<X,Y> converter, JavaTypeRegistry registry) {
		final var typeArguments =
				typeArguments( AttributeConverter.class,
						converter.getClass() );
		@SuppressWarnings("unchecked") // perfectly safe
		final var domainJavaClass = (Class<X>) erasedType( typeArguments[0] );
		@SuppressWarnings("unchecked") // perfectly safe
		final var relationalJavaClass = (Class<Y>) erasedType( typeArguments[1] );
		return new AttributeConverterInstance<>(
				converter,
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}

	public static <X, Y> JpaAttributeConverter<X, Y> createJpaAttributeConverter(
			ManagedBean<? extends AttributeConverter<X,Y>> bean, JavaTypeRegistry registry) {
		final var typeArguments =
				typeArguments( AttributeConverter.class,
						bean.getBeanClass() );
		@SuppressWarnings("unchecked") // perfectly safe
		final var domainJavaClass = (Class<X>) erasedType( typeArguments[0] );
		@SuppressWarnings("unchecked") // perfectly safe
		final var relationalJavaClass = (Class<Y>) erasedType( typeArguments[1] );
		return new AttributeConverterBean<>(
				bean,
				registry.resolveDescriptor( bean.getBeanClass() ),
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}

	public static <X, Y> JpaAttributeConverter<X, Y> createJpaAttributeConverter(
			Class<? extends AttributeConverter<X,Y>> converterClass,
			ServiceRegistry serviceRegistry,
			TypeConfiguration typeConfiguration) {
		var converterBean = serviceRegistry.requireService( ManagedBeanRegistry.class ).getBean( converterClass );
		return createJpaAttributeConverter( converterBean, typeConfiguration.getJavaTypeRegistry() );
	}

	public static <X> BasicType<X> createConvertedParameterType(
			Class<? extends AttributeConverter<X,?>> converterClass,
			ServiceRegistry serviceRegistry,
			TypeConfiguration typeConfiguration) {
		//noinspection unchecked,rawtypes
		final JpaAttributeConverter<X,Object> converter =
				createJpaAttributeConverter( (Class) converterClass, serviceRegistry, typeConfiguration );
		final var relationalJavaType = converter.getRelationalJavaType();
		final var relationalType =
				typeConfiguration.standardBasicTypeForJavaType( relationalJavaType.getJavaTypeClass() );
		if ( relationalType == null ) {
			throw new HibernateException(
					"Unable to determine JDBC type for converted parameter relational type: "
							+ relationalJavaType.getTypeName()
			);
		}
		return new ConvertedBasicTypeImpl<>(
				"converted-parameter::" + converter.getConverterJavaType().getTypeName(),
				String.format(
						"BasicType adapter for converted query parameter AttributeConverter<%s,%s>",
						converter.getDomainJavaType().getTypeName(),
						relationalJavaType.getTypeName()
				),
				relationalType.getJdbcType(),
				converter
		);
	}
}
