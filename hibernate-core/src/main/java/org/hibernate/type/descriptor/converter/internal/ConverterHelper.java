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

import static org.hibernate.internal.util.GenericsHelper.extractClass;
import static org.hibernate.internal.util.GenericsHelper.extractParameterizedType;

/**
 * @author Gavin King
 * @since 7.0
 */
public class ConverterHelper {
	public static <X, Y> BasicValueConverter<X, Y> createValueConverter(
			AttributeConverter<X,Y> converter, JavaTypeRegistry registry) {
		final ParameterizedType converterParameterizedType = extractParameterizedType( converter.getClass() );
		final Class<?> domainJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[1] );
		return new AttributeConverterWrapper<>(
				converter,
				registry.resolveDescriptor( converter.getClass() ),
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}

	public static <X, Y> JpaAttributeConverter<X, Y> createJpaAttributeConverter(
			ManagedBean<? extends AttributeConverter<X,Y>> bean, JavaTypeRegistry registry) {
		final ParameterizedType converterParameterizedType = extractParameterizedType( bean.getBeanClass() );
		final Class<?> domainJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[0] );
		final Class<?> relationalJavaClass = extractClass( converterParameterizedType.getActualTypeArguments()[1] );
		return new JpaAttributeConverterImpl<>(
				bean,
				registry.resolveDescriptor( bean.getBeanClass() ),
				registry.resolveDescriptor( domainJavaClass ),
				registry.resolveDescriptor( relationalJavaClass )
		);
	}
}
