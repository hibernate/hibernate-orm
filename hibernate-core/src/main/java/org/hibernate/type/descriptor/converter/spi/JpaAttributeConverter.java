/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.spi;

import jakarta.persistence.AttributeConverter;

import org.hibernate.Incubating;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Adapts a JPA-standard {@link AttributeConverter} to the native
 * {@link BasicValueConverter}.
 *
 * @param <O> The entity attribute type
 * @param <R> The converted type
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaAttributeConverter<O,R> extends BasicValueConverter<O,R> {
	/**
	 * A {@link JavaType} representing the JPA {@link AttributeConverter}.
	 */
	JavaType<? extends AttributeConverter<O,R>> getConverterJavaType();

	/**
	 * A {@link ManagedBean} representing the JPA {@link AttributeConverter},
	 * in the case that the converter is a managed bean, e.g., a CDI bean.
	 */
	ManagedBean<? extends AttributeConverter<O,R>> getConverterBean();
}
