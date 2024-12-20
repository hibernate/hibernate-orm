/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.converter.spi;

import jakarta.persistence.AttributeConverter;

import org.hibernate.Incubating;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * {@link BasicValueConverter} extension for {@link AttributeConverter}-specific support
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaAttributeConverter<O,R> extends BasicValueConverter<O,R> {
	JavaType<? extends AttributeConverter<O,R>> getConverterJavaType();

	ManagedBean<? extends AttributeConverter<O,R>> getConverterBean();
}
