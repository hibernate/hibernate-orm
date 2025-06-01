/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.internal;

import jakarta.persistence.AttributeConverter;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.hibernate.internal.util.GenericsHelper.extractClass;
import static org.hibernate.internal.util.GenericsHelper.extractParameterizedType;

/**
 * @author Gavin King
 * @since 7.0
 */
public class ConverterHelper {
	public static <X, Y> BasicValueConverter<X, Y> createValueConverter(
			AttributeConverter<X,Y> converter, JavaTypeRegistry registry) {
		final ParameterizedType converterType =
				extractParameterizedType( converter.getClass(), AttributeConverter.class );
		final Type[] typeArguments = converterType.getActualTypeArguments();
		final Class<?> domainJavaClass = extractClass( typeArguments[0] );
		final Class<?> relationalJavaClass = extractClass( typeArguments[1] );
		return new AttributeConverterInstance<>(
				converter,
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}

	public static <X, Y> JpaAttributeConverter<X, Y> createJpaAttributeConverter(
			ManagedBean<? extends AttributeConverter<X,Y>> bean, JavaTypeRegistry registry) {
		final ParameterizedType converterType =
				extractParameterizedType( bean.getBeanClass(), AttributeConverter.class );
		final Type[] typeArguments = converterType.getActualTypeArguments();
		final Class<?> domainJavaClass = extractClass( typeArguments[0] );
		final Class<?> relationalJavaClass = extractClass( typeArguments[1] );
		return new AttributeConverterBean<>(
				bean,
				registry.resolveDescriptor( bean.getBeanClass() ),
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}
}
